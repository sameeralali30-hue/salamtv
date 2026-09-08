package tv.own.owntv.features.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
 * ═══════════════════════════════════════════════════════════════════════════
 *  إنشاء حساب من داخل التطبيق.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ الرمز حاملٌ لقيمة لا يعرف صاحبه، والأيام تُضاف إلى حساب. فمن اشترى
 *    رمزاً من وكيل ولا حساب له كان عليه أن يفتح المتصفّح ويسجّل في الموقع
 *    ثمّ يعود إلى التطبيق. ومن لم يعرف ذلك ضغط «لديك رمز تفعيل؟» وأدخل
 *    اسماً لم يُنشئه بعد، فقيل له «اسم المستخدم أو كلمة المرور غير صحيحة» —
 *    وهي رسالة صادقة لا تدلّه على شيء، فيتّصل بالدعم أو ينصرف.
 *
 *  ═══ أربعة قرارات ═══
 *
 *  ① الرمز حقلٌ في نفس النافذة، اختياري.
 *     من معه رمز يفرغ في خطوة واحدة ولا يرى شاشةً فارغة قطّ. ومن ليس معه
 *     يُنشئ حسابه ويفعّله لاحقاً من «حسابي». نافذتان تعنيان من ينشئ حساباً
 *     ثمّ ينسى الرمز في جيبه.
 *
 *  ② شروط الاسم وكلمة المرور تُقال قبل أن يُرسل.
 *     الشرط الذي لا يُعرف إلا بعد الرفض يُجرّب مرّتين وثلاثاً. وهو مكتوب
 *     تحت الحقلين لا في رسالة خطأ.
 *
 *  ③ ما ترفضه اللوحة يُعرض بنصّها.
 *     «هذا الاسم محجوز» أكثر ما سيقع، وهو غير «كلمة المرور قصيرة» وغير
 *     «الرمز مستعمَل». ودمجها في «فشل» يجعل كلّ حالة مكالمةَ دعم.
 *
 *  ④ الحروف الكبيرة والمسافات في الرمز تُصلَح لا تُرفض.
 *     من ينسخ رمزاً من واتساب يجرّ معه مسافة. رفضُ رمزٍ صحيح بسببها عيبٌ
 *     فينا لا فيه.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
fun RegisterDialog(
    busy: Boolean,
    message: String?,
    succeeded: Boolean,
    offline: Boolean,
    onSubmit: (username: String, password: String, code: String) -> Unit,
    onHaveAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    // ② نفس شروط اللوحة، مطبَّقة هنا قبل الإرسال
    val canSubmit = !busy && user.trim().length >= 3 && pass.length >= 8

    tv.own.owntv.features.profiles.ProfileScrim(onDismiss = onDismiss, width = 460.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.salamtv_register_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.salamtv_register_hint),   // ②
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            OwnTVTextField(
                value = user,
                onValueChange = { user = it.filter { c -> !c.isWhitespace() } },
                label = stringResource(R.string.salamtv_login_username),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OwnTVTextField(
                value = pass,
                onValueChange = { pass = it },
                label = stringResource(R.string.salamtv_login_password),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OwnTVTextField(
                value = code,
                // ④ نُصلح ما يفسده النسخ بدل أن نرفضه
                onValueChange = { code = it.uppercase().filter { c -> !c.isWhitespace() } },
                label = stringResource(R.string.salamtv_register_code_optional),   // ①
                onImeDone = { if (canSubmit) onSubmit(user.trim(), pass, code.trim()) },
                modifier = Modifier.fillMaxWidth(),
            )

            val shown = if (offline) stringResource(R.string.salamtv_redeem_offline) else message
            if (!shown.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    shown,                                        // ③ نصّ اللوحة حرفياً
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
                    stringResource(R.string.salamtv_register_have_account),
                    onClick = onHaveAccount,
                    style = OwnTVButtonStyle.SECONDARY,
                )
                OwnTVButton(
                    stringResource(
                        if (busy) R.string.salamtv_register_working else R.string.salamtv_register_action,
                    ),
                    onClick = { if (canSubmit) onSubmit(user.trim(), pass, code.trim()) },
                    enabled = canSubmit,
                )
            }
        }
    }
}
