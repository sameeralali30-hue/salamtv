package tv.own.owntv.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.features.setup.SubscriberLoginClient
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.theme.OwnTVTheme
import java.text.DateFormat
import java.util.Date

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  «حسابي» — ما يعرفه المشترك عن اشتراكه، ومن حيث يجدّده أو يخرج.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ كان رمز التفعيل على شاشة الدخول وحدها. فمن انتهت باقته واشترى رمز
 *    تجديد لم يجد أين يُدخله وهو داخلٌ في التطبيق — كان عليه أن يُسجّل
 *    خروجاً أوّلاً، وهو آخر ما يخطر ببال من يريد أن يدفع لنا. صار التجديد
 *    والخروج والبيانات في مكان واحد: حيث يبحث عنها من يسأل «متى ينتهي؟».
 *
 *  ═══ أربعة قرارات ═══
 *
 *  ① التواريخ تُصاغ على الجهاز بلغته.
 *     اللوحة ترسل رقم حقبة لا نصّاً: نصّ بلا منطقة زمنية يُقرأ على الجهاز
 *     بمنطقته هو، فيظهر الانتهاء متقدّماً أو متأخّراً ساعات بحسب بلد
 *     المشترك — وهو أكثر ما يُشكى منه في خدمات الاشتراك.
 *
 *  ② الحالة سطرٌ يقول ما العمل، لا كلمة.
 *     «منتهٍ» وحدها تُخبر ولا تُرشد. «انتهى — أدخل رمز تفعيل للتجديد»
 *     تُخبر وترشد، والزرّ تحتها مباشرة.
 *
 *  ③ الاشتراك المفتوح ليس «صفر يوماً».
 *     اللوحة ترسل ‎-1‎ للتفعيل الإداري الدائم. عرضُه صفراً يجعل صاحبه
 *     يظنّ اشتراكه انتهى اليوم.
 *
 *  ④ آخر ما عُرف يُعرض حين يتعذّر الاتصال.
 *     من فتح الشاشة بلا شبكة يريد أن يعرف متى ينتهي اشتراكه — وهو ما
 *     نعرفه من آخر فحص. شاشةٌ فارغة تُخفي ما نملكه بلا سبب.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
fun SubscriberAccountDialog(
    status: SubscriberLoginClient.Status?,
    /** لم يصل ردٌّ بعد ولا حالة محفوظة. */
    loading: Boolean,
    onRedeem: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val dates = DateFormat.getDateInstance(DateFormat.MEDIUM)   // ① لغة الجهاز ومنطقته

    tv.own.owntv.features.profiles.ProfileScrim(onDismiss = onDismiss, width = 520.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.salamtv_account),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(14.dp))

            when {
                status == null && loading -> Text(
                    stringResource(R.string.salamtv_account_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                // ④ لا حالة ولا اتصال
                status == null -> Text(
                    stringResource(R.string.salamtv_account_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                else -> {
                    // ② الحالة تقول ما العمل
                    Text(
                        stringResource(
                            when (status.state) {
                                "active" -> R.string.salamtv_account_state_active
                                "expired" -> R.string.salamtv_account_state_expired
                                "disabled" -> R.string.salamtv_account_state_disabled
                                else -> R.string.salamtv_account_state_pending
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.active) colors.primary else colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    AccountLine(stringResource(R.string.salamtv_account_username), status.username)
                    if (status.planName.isNotBlank()) {
                        AccountLine(stringResource(R.string.salamtv_account_plan), status.planName)
                    }
                    status.subStart?.let {
                        AccountLine(
                            stringResource(R.string.salamtv_account_started),
                            dates.format(Date(it * 1000L)),
                        )
                    }
                    status.subEnd?.let {
                        AccountLine(
                            stringResource(R.string.salamtv_account_ends),
                            dates.format(Date(it * 1000L)),
                        )
                    }
                    AccountLine(
                        stringResource(R.string.salamtv_account_days_left),
                        // ③ اشتراك مفتوح لا «صفر يوماً»
                        if (status.daysLeft < 0) {
                            stringResource(R.string.salamtv_account_open)
                        } else {
                            status.daysLeft.toString()
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, androidx.compose.ui.Alignment.CenterHorizontally),
            ) {
                OwnTVButton(
                    stringResource(R.string.salamtv_redeem_title),
                    onClick = onRedeem,
                )
                OwnTVButton(
                    stringResource(R.string.salamtv_sign_out),
                    onClick = onSignOut,
                    style = OwnTVButtonStyle.SECONDARY,
                )
            }
        }
    }
}

/** سطر «عنوان ← قيمة»، والقيمة تأخذ ما بقي من العرض فلا تُقصّ. */
@Composable
private fun AccountLine(label: String, value: String) {
    val colors = OwnTVTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),   // امتداد RowScope، لا استيراد له
        )
    }
    Spacer(Modifier.height(8.dp))
}
