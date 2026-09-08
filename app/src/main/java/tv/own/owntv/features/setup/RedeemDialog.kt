package tv.own.owntv.features.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVTextField
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * تفعيل رمز اشتراه المشترك من وكيل.
 *
 * ═══ ثلاثة قرارات ═══
 *
 * ① الرسالة تأتي من اللوحة كما هي.
 *    «الرمز غير صحيح» و«استعملتَه من قبل» و«انتهت صلاحيته» ثلاثة أشياء
 *    مختلفة، واللوحة وحدها تعرف أيّها وقع. أن يقول التطبيق «فشل التفعيل»
 *    يعني اتصالاً بالدعم في كل مرّة.
 *
 * ② بيانات الحساب تُملأ حين تكون معروفة.
 *    من داخل الإعدادات يكون المشترك داخلاً بالفعل، فلا نطلب منه كتابة ما
 *    نعرفه. ومن شاشة الدخول لا نعرفه، فتظهر الحقول الثلاثة.
 *
 * ③ الحروف تُرفع تلقائياً والمسافات تُزال.
 *    الرموز تُكتب بحروف كبيرة وشرطات، ومن ينسخها من واتساب يجرّ معها
 *    مسافة. رفضُ رمزٍ صحيح بسبب مسافة عيبٌ فينا لا فيه.
 */
@Composable
fun RedeemDialog(
    knownUsername: String?,
    knownPassword: String?,
    busy: Boolean,
    message: String?,
    succeeded: Boolean,
    /** عطل نقل لا رفض — نصّه من الموارد لأنّ اللوحة لم تُجب. */
    offline: Boolean = false,
    /**
     * هل تُعرض حقول الحساب؟
     *
     * ⚠ أوّل ربطٍ من الإعدادات مرّر شرطتين كاسمٍ وكلمة مرور ليُخفيهما، وهي
     *   قيمة وهمية تظهر للمشترك إن تغيّر الشرط يوماً. الاستدعاء من الإعدادات
     *   لا يحتاجهما أصلاً: الـViewModel يقرأ البيانات من المصدر ولا تمرّ
     *   عبر الشاشة.
     */
    askForAccount: Boolean = knownUsername.isNullOrBlank() || knownPassword.isNullOrBlank(),
    onSubmit: (username: String, password: String, code: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    var user by remember { mutableStateOf(knownUsername.orEmpty()) }
    var pass by remember { mutableStateOf(knownPassword.orEmpty()) }
    var code by remember { mutableStateOf("") }

    // ② ما نعرفه لا نسأل عنه
    val needsAccount = askForAccount
    val canSubmit = !busy && code.isNotBlank() && (!needsAccount || (user.isNotBlank() && pass.isNotBlank()))

    // نفس غلاف نوافذ البروفايل: تعتيمٌ خلف اللوح وحدود تركيز للريموت.
    // ⚠ أوّل نسخة استعملت OwnTVPopup مباشرةً بلا لوح، فظهرت الحقول فوق
    //   شاشة الدخول بلا خلفية وبدت الشاشتان مندمجتين.
    tv.own.owntv.features.profiles.ProfileScrim(onDismiss = onDismiss, width = 420.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.salamtv_redeem_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.salamtv_redeem_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (needsAccount) {
                OwnTVTextField(
                    user, { user = it },
                    label = stringResource(R.string.salamtv_login_username),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OwnTVTextField(
                    pass, { pass = it },
                    label = stringResource(R.string.salamtv_login_password),
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }

            OwnTVTextField(
                code,
                // ③ نُصلح ما يفسده النسخ بدل أن نرفضه
                { code = it.uppercase().filter { c -> !c.isWhitespace() } },
                label = stringResource(R.string.salamtv_redeem_field),
                modifier = Modifier.fillMaxWidth(),
            )

            val shown = if (offline) stringResource(R.string.salamtv_redeem_offline) else message
            if (!shown.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    shown,                                    // ① نصّ اللوحة حرفياً
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (succeeded) colors.primary else colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                OwnTVButton(
                    stringResource(R.string.common_cancel),
                    onClick = onDismiss,
                    style = OwnTVButtonStyle.SECONDARY,
                )
                Spacer(Modifier.width(2.dp))
                OwnTVButton(
                    stringResource(
                        if (busy) R.string.salamtv_redeem_working else R.string.salamtv_redeem_action,
                    ),
                    onClick = { if (canSubmit) onSubmit(user.trim(), pass, code.trim()) },
                    enabled = canSubmit,
                )
            }
        }
    }
}
