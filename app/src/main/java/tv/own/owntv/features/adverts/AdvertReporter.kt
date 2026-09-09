package tv.own.owntv.features.adverts

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import tv.own.owntv.core.database.dao.AdvertDao
import tv.own.owntv.features.setup.SubscriberLoginClient

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  صندوق البريد الصادر: ما عُرض فعلاً يصل اللوحة.
 * ───────────────────────────────────────────────────────────────────────────
 *  لا مؤقّت ولا عامل خلفيّة: يُصرَّف على نبضة الاشتراك القائمة. نبضةٌ ثانية
 *  تعني ضِعف الطلبات لخدمةٍ لا تستعجل، وبطّاريّةَ هاتفٍ في جيب مقابل لا شيء.
 *
 *  ═══ خمسة قرارات ═══
 *
 *  ① لا بروتوكول إيصالات.
 *     الخادم يحمل `UNIQUE(device_key, event_uid)` ويبتلع المكرّر بـ
 *     `INSERT IGNORE`. فالجهاز يرسل ما عنده كلّما استطاع، ولا يضرّه أن تصل
 *     الدفعة مرّتين. وبدون ذلك يصير كلّ انقطاعِ شبكةٍ اختياراً بين فقدِ
 *     انطباعاتٍ وتضخيمها — وكلاهما يُفسد ما تقوله للمعلن.
 *
 *  ② لا يُعلَّم صفٌّ مُرسَلاً إلّا بعد ردٍّ ناجح.
 *     ⚠ التعليم قبل الردّ يفقد الدفعة كاملةً عند أوّل انقطاع، ولا شيء
 *       يُعيدها: الصفوف صارت «مُرسَلة» وهي لم تصل.
 *
 *  ③ الجارية لا تُرسَل.
 *     `outcome IS NULL` يعني إعلاناً يُعرض الآن. إرساله يعني انطباعاً بلا
 *     نتيجة، ثمّ صفّاً ثانياً حين ينتهي — والمفتاح الفريد يمنع الثاني، فتبقى
 *     النتيجة الحقيقيّة ضائعة إلى الأبد.
 *
 *  ④ صندوقٌ محدود.
 *     ما لم يصل خلال أسبوعين يُهمل. جهازٌ بلا شبكة شهراً لا يجوز أن يُراكم
 *     صفوفاً بلا سقف، ولا أن يُغرق الخادم بها حين يعود.
 *
 *  ⑤ تصريفٌ واحد في كلّ لحظة.
 *     النبضة قد تقع مرّتين متلاحقتين، وتصريفان متوازيان يرسلان نفس الصفوف —
 *     يبتلعها الخادم، لكنّها طلباتٌ ضائعة وحدُّ معدّلٍ يُستهلك بلا سبب.
 * ═══════════════════════════════════════════════════════════════════════════
 */
class AdvertReporter(
    private val dao: AdvertDao,
    private val login: SubscriberLoginClient,
) {
    private val gate = Mutex()   // ⑤

    /**
     * يرسل ما تجمّع، ويعيد الرصيد المعتمَد من الخادم (‎-1‎ لمشتركٍ مدفوع)
     * أو null إن لم يجرِ إرسال.
     *
     * @param consumedMinutes دقائق المشاهدة المستهلكة اليوم — للمجّانيّ وحده.
     */
    suspend fun drain(
        username: String,
        password: String,
        batchLimit: Int,
        consumedMinutes: Int = 0,
    ): Int? {
        if (username.isBlank() || password.isBlank()) return null
        if (!gate.tryLock()) return null                       // ⑤

        try {
            // ④ التنظيف أوّلاً: لا معنى لإرسال ما تجاوز عمره المفيد.
            runCatching { dao.prune(System.currentTimeMillis() - MAX_AGE_MS) }

            // ③ المكتملة وحدها.
            val rows = dao.unreported(batchLimit.coerceIn(1, 200))
            if (rows.isEmpty() && consumedMinutes <= 0) return null

            val events = JSONArray()
            for (r in rows) {
                events.put(
                    JSONObject()
                        .put("uid", r.eventUid)
                        .put("spot_id", r.spotId)
                        .put("channel_id", r.channelRemoteId.toLongOrNull() ?: 0L)
                        .put("placement", r.placement)
                        .put("at", r.shownAt / 1000)
                        .put("watched_ms", r.watchedMs)
                        .put("outcome", r.outcome ?: "")
                        .put("granted_minutes", r.grantedMinutes),
                )
            }

            val balance = login.reportAdverts(username, password, events, consumedMinutes)
            if (balance == null) {
                // ② لم يصل ردّ: لا يُعلَّم شيء. المحاولة التالية تُعيد الدفعة،
                //    والمفتاح الفريد يمنع الازدواج.
                Log.d(TAG, "advert report not delivered; ${rows.size} row(s) stay queued")
                return null
            }

            if (rows.isNotEmpty()) {
                dao.markReported(rows.map { it.eventUid })
                Log.i(TAG, "reported ${rows.size} advert impression(s)")
            }
            return balance
        } finally {
            gate.unlock()
        }
    }

    companion object {
        private const val TAG = "SalamTVAds"

        /** ④ عمر الصفّ الأقصى في الصندوق. */
        private const val MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
    }
}
