package tv.own.owntv.features.adverts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.theme.OwnTVTheme

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
fun OutOfTimeDialog(onDismiss: () -> Unit) {
    val colors = OwnTVTheme.colors

    tv.own.owntv.features.profiles.ProfileScrim(onDismiss = onDismiss, width = 480.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.salamtv_out_of_time_title),   // ①
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.salamtv_out_of_time_body),    // ②③
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                OwnTVButton(stringResource(R.string.salamtv_out_of_time_ok), onClick = onDismiss)
            }
        }
    }
}
