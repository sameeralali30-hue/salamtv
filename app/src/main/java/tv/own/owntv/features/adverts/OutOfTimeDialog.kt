package tv.own.owntv.features.adverts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.R

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  نفد وقت المشاهدة المجّانيّ، ولا إعلان يشتري المزيد.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ هذه الشاشة تظهر في أسوأ لحظة يمكن أن يقرأ فيها المستخدم شيئاً: توقّفت
 *    الصورة للتوّ. فالمطلوب منها ثلاثة أشياء بالترتيب، ولا رابع:
 *
 *      ① أن تقول ما حدث بلا لوم — «انتهى وقتك» لا «تجاوزت حدّك».
 *      ② أن تقول متى يعود — «غداً». بدونها يبدو الأمر عطلاً دائماً،
 *        ومن يظنّ التطبيق معطّلاً يحذفه ولا يشكو.
 *      ③ أن تعرض المخرج مرّةً واحدة بلا إلحاح — الترقية.
 *
 *  ولا تُعرض إلّا بعد أن تفشل محاولة الإعلان الوسطيّ: ما دام هناك إعلانٌ
 *  يشتري وقتاً، فعرضُه أولى من إغلاق الباب في وجه من يريد أن يشاهد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
fun OutOfTimeDialog(contact: String, onDismiss: () -> Unit) {
    tv.own.owntv.features.salamtv.SalamTVNoticeDialog(
        emoji = "⏳",
        title = stringResource(R.string.salamtv_out_of_time_title),   // ①
        body = stringResource(R.string.salamtv_out_of_time_body),     // ②③
        contact = contact,
        onDismiss = onDismiss,
    )
}
