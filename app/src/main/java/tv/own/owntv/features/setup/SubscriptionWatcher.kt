package tv.own.owntv.features.setup

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  يُبقي التطبيق على ما تقوله اللوحة، بلا خروجٍ ولا مسحِ بيانات.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ العطل الذي وُلد منه هذا الملف: الموزّع يرفع خطّة مشترك في اللوحة فلا
 *    يرى المشترك شيئاً — لا قنوات الفئات الجديدة ولا سقف الجودة الأعلى —
 *    حتى يُسجّل خروجاً ودخولاً أو يمسح التطبيق. فكانت كلّ ترقية مكالمةَ دعم،
 *    وكان أوّل ما يفعله المشترك بعد أن يدفع هو أن يشكو.
 *
 *  ═══ خمسة قرارات ═══
 *
 *  ① بصمةٌ تُقارَن، لا كتالوجٌ يُسحب.
 *     اللوحة تحسب `rev` من الخطّة ونهاية الاشتراك وحالة التفعيل وسقف
 *     الجودة. فالسؤال «هل تغيّر شيء؟» ردٌّ في مئتَي بايت، والجواب «لا» في
 *     الغالب الأعمّ. سحبُ مئتَي قناة كلّ بضع دقائق لاكتشاف أنّ شيئاً لم
 *     يتغيّر يستهلك بيانات المشترك وخادمنا معاً بلا طائل.
 *
 *  ② «لا أدري» ليست «تغيّر شيء».
 *     ⚠ لو عومل انقطاعُ الشبكة معاملةَ التغيير لأعاد التطبيق بناء الكتالوج
 *       كلّما ضعفت الشبكة — وهو أسوأ وقتٍ لذلك. فالبصمة لا تُكتب إلا عن
 *       ردٍّ مفهوم، والفشل يُترك للمحاولة التالية.
 *
 *  ③ أوّل قراءة تُسجَّل ولا تُشغّل شيئاً.
 *     من سجّل دخوله للتوّ كتالوجه جديد. لو عُدّت أوّل بصمة تغييراً لأعاد
 *     كلّ مشترك بناء كتالوجه مرّة زائدة بعد الدخول مباشرة.
 *
 *  ④ فحصٌ واحد في كلّ لحظة.
 *     العودة إلى التطبيق قد تقع مرّتين متتاليتين (دوران الشاشة، إشعار)،
 *     والقفل يمنع فحصين متوازيين يُطلقان مزامنتين لنفس المصدر.
 *
 *  ⑤ الحالة تُنشر كما هي حتّى لو لم تتغيّر.
 *     شاشة «حسابي» تعرض ما في [status]. فحصٌ لم يجد تغييراً ما زال يُحدّث
 *     «يتبقّى ١٢ يوماً» إلى «١١».
 *
 *  ⑥ نبضٌ ما دام في المقدّمة، لا فحصٌ عند الفتح وحده.
 *     ⚠ كان الفحص عند الإقلاع وعند العودة من الخلفية فقط. وهذا يكفي هاتفاً
 *       يُفتح ويُغلق عشرات المرّات، ولا يكفي شاشةً تُترك مفتوحة أربع ساعات
 *       — وهي الحالة الغالبة على أجهزة التلفاز. فمن رُقّيت خطّته وهو يشاهد
 *       بقي على القديم إلى أن يُطفئ الجهاز، أي إلى الغد.
 *
 *       والنبضة ثلاثمئة بايت: مئةٌ منها في الساعة أرخص من ثانيةٍ واحدة من
 *       البثّ.
 *
 *  ⑦ إيقاع النبض يأتي من الخادم.
 *     `poll` في الردّ. القيمة تُحدّ بين دقيقة وربع ساعة هنا، فلا يستطيع ردٌّ
 *     مشوّه — أو خادمٌ منتحَل — أن يجعل الجهاز ينبض كلّ ثانية.
 * ═══════════════════════════════════════════════════════════════════════════
 */
class SubscriptionWatcher(
    context: Context,
    private val login: SubscriberLoginClient,
) {
    private val prefs = context.getSharedPreferences("salamtv_subscription", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()   // ④

    private val _status = MutableStateFlow<SubscriberLoginClient.Status?>(null)

    /** آخر حالة معروفة، أو null قبل أوّل فحص ناجح. */
    val status: StateFlow<SubscriberLoginClient.Status?> = _status.asStateFlow()

    private val _checks = MutableStateFlow(0L)

    /**
     * عدّاد يزداد بعد **كلّ** فحصٍ ناجح، تغيّرت الحالة أو لم تتغيّر.
     *
     * ⚠ [status] لا يصلح لمن يريد «وقع فحصٌ الآن». هو `StateFlow`، ولا يُشعر
     *   متلقّيه إلّا حين تختلف القيمة عن سابقتها — وحين لا يتغيّر شيء في
     *   الاشتراك تكون الحالة مطابقة تماماً فلا يصل شيء. من علّق عملاً دوريّاً
     *   عليه (تصريف تقارير مثلاً) وجده يعمل مرّةً عند الدخول ثمّ يصمت أبداً،
     *   والنبض يعمل طوال الوقت. القيمة هنا لا تتكرّر، فالإشعار مضمون.
     */
    val checks: StateFlow<Long> = _checks.asStateFlow()

    private var lastCheckAtMs = 0L

    /**
     * ⑦ ما بين نبضتين، بالملّي ثانية. يُحدَّث من كلّ ردّ ناجح.
     *
     * `@Volatile` لأنّ الكاتب هو خيط الشبكة والقارئ حلقةُ النبض على الخيط
     * الرئيسي.
     */
    @Volatile
    private var pollIntervalMs: Long = DEFAULT_POLL_MS

    /** ⑥ حلقةٌ واحدة لا أكثر — فتحُ شاشةٍ فوق أخرى لا يضاعف النبض. */
    private var beat: Job? = null

    /**
     * يفحص إن مضى ما يكفي، ويستدعي [onChanged] حين تتبدّل البصمة فعلاً.
     *
     * @param force يتجاوز المهلة — لتفعيلٍ وقع للتوّ، فلا ينتظر المشترك
     *              الذي دفع دقائق ليرى ما اشتراه.
     */
    fun check(
        username: String,
        password: String,
        force: Boolean = false,
        onChanged: () -> Unit,
    ) {
        if (username.isBlank() || password.isBlank()) return
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckAtMs < MIN_INTERVAL_MS) return

        scope.launch {
            if (!gate.tryLock()) return@launch          // ④
            try {
                val st = login.account(username, password)
                if (st == null) {
                    // ② تعذّر الوصول — لا نكتب شيئاً ولا نُشغّل شيئاً.
                    Log.d(TAG, "check inconclusive; leaving revision as-is")
                    return@launch
                }
                lastCheckAtMs = now
                _status.value = st                       // ⑤
                _checks.value = _checks.value + 1
                // ⑦ الإيقاع بيد الخادم، والحدّان هنا فلا يُساء استعماله.
                if (st.pollSeconds > 0) {
                    pollIntervalMs = (st.pollSeconds * 1000L).coerceIn(MIN_POLL_MS, MAX_POLL_MS)
                }

                val key = KEY_REV + ":" + username
                val known = prefs.getString(key, null)
                prefs.edit().putString(key, st.rev).apply()

                when {
                    // ③ أوّل قراءة: تُسجَّل فقط.
                    known == null -> Log.i(TAG, "first revision recorded")
                    known != st.rev -> {
                        Log.i(TAG, "subscription changed — rebuilding catalogue")
                        onChanged()
                    }
                    else -> Log.d(TAG, "no change")
                }
            } finally {
                gate.unlock()
            }
        }
    }

    /**
     * ⑥ يبدأ النبض ويبقى إلى أن يُلغى — يُربط بدورة حياة الشاشة في
     * `MainActivity`، فينتهي حين يخرج التطبيق من المقدّمة.
     *
     * أوّل نبضة فوريّة: من فتح التطبيق للتوّ لا ينتظر دقيقتين ليرى ما
     * اشتراه قبل قليل. وحارسُ الثلاثين ثانية في [check] يمنعها من أن
     * تتضاعف مع فحص الإقلاع.
     */
    fun startBeating(
        scope: CoroutineScope,
        credentials: suspend () -> Pair<String, String>?,
        onChanged: () -> Unit,
    ) {
        beat?.cancel()
        beat = scope.launch {
            while (isActive) {
                val creds = credentials()
                val u = creds?.first.orEmpty()
                val p = creds?.second.orEmpty()
                if (u.isNotBlank() && p.isNotBlank()) {
                    check(u, p, force = false, onChanged = onChanged)
                }
                // ⚠ التأخير خارج الشرط عمداً. لو كان داخله لدارت الحلقة بلا
                //   توقّف على شاشة الدخول — حيث لا حساب بعد — فأحرقت البطّارية
                //   في لا شيء. من لا حساب له ينتظر مثل غيره، والحلقة تلتقطه
                //   حين يسجّل دخوله.
                delay(pollIntervalMs)
            }
        }
    }

    /** يوقف النبض — الخروج من المقدّمة، أو تسجيل خروج. */
    fun stopBeating() {
        beat?.cancel()
        beat = null
    }

    /** يُنسي ما يخصّ حساباً — يُستدعى عند تسجيل الخروج فلا تبقى بصمة غريبة. */
    fun forget(username: String) {
        prefs.edit().remove(KEY_REV + ":" + username).apply()
        _status.value = null
    }

    companion object {
        private const val TAG = "SalamTVWatch"
        private const val KEY_REV = "rev"

        /**
         * أقصر ما بين فحصين — حارسٌ ضدّ الازدواج لا مهلة انتظار.
         *
         * ⚠ كانت خمس دقائق، فسقط الغرض كلّه: من رُقّيت خطّته ثمّ فتح التطبيق
         *   في الحال وجد كتالوجه القديم، لأنّ فحصه أُهمل. جرّبتُه على الجهاز
         *   فلم يقع شيء — والحلّ ليس إطالة الانتظار بل تقصيره.
         *
         *   ثلاثون ثانية تكفي لمنع فحصين من دوران شاشة أو إشعار متتاليين،
         *   ولا تمنع فتحةً قاصدة. والطلب مئتا بايت: فتحُ التطبيق مئةَ مرّة
         *   في اليوم أرخص من صورة واحدة.
         */
        private const val MIN_INTERVAL_MS = 30 * 1000L

        /** ⑦ ما يُستعمل قبل أوّل ردّ، وحدّاه. */
        private const val DEFAULT_POLL_MS = 120 * 1000L
        private const val MIN_POLL_MS = 60 * 1000L
        private const val MAX_POLL_MS = 15 * 60 * 1000L
    }
}
