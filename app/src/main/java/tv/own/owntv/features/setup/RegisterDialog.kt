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
import androidx.compose.ui.focus.FocusRequester
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
 *  إنشاء حساب — اسمٌ وكلمة مرور، لا غير.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ أوّل نسخة وضعت حقل «رمز التفعيل» في هذه النافذة، وكان خطأً من وجهين:
 *
 *    • أوهم أنّ المشترك يُنشئ الرمز كما يُنشئ اسمه. والرمز لا يُنشأ هنا —
 *      نحن نولّده في اللوحة والموزّع يبيعه.
 *
 *    • ربط فشلين لا علاقة بينهما. من قيل له «الاسم محجوز» غيّر اسمه وضغط
 *      «إنشاء» فقيل له «الرمز غير صحيح» — لأنّ الرمز كان قد استُهلك في
 *      المحاولة التي نجح فيها الحساب. فبدا كأنّ التطبيق يمنعه بلا سبب.
 *
 *    الرمز الآن نافذته وحده، بعد أن يقوم الحساب.
 *
 *  ═══ ثلاثة قرارات ═══
 *
 *  ① الحقل الأوّل يفتح نفسه، و«التالي» ينقل إلى الثاني.
 *     ⚠ كلّ لمسة على حقل تعني فتح لوحة مفاتيح وانتظار حركتها. حقلان
 *       يعنيان لمستين وإغلاقاً وفتحاً بينهما، وهو ما يجعل نافذةً بسيطة
 *       تبدو ثقيلة. الآن: تُفتح النافذة فيكتب، «التالي»، يكتب، ✓.
 *
 *  ② شروط الاسم وكلمة المرور تُقال قبل أن يُرسل.
 *     الشرط الذي لا يُعرف إلا بعد الرفض يُجرَّب مرّتين وثلاثاً.
 *
 *  ③ ما ترفضه اللوحة يُعرض بنصّها.
 *     «هذا الاسم محجوز» غير «كلمة المرور قصيرة»، ودمجهما في «فشل» يجعل
 *     كلّ حالة مكالمةَ دعم.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
fun RegisterDialog(
    busy: Boolean,
    message: String?,
    succeeded: Boolean,
    offline: Boolean,
    onSubmit: (username: String, password: String) -> Unit,
    onHaveAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val userFocus = remember { FocusRequester() }
    val passFocus = remember { FocusRequester() }
    // «التالي» يرفعها فيفتح حقل كلمة المرور نفسه بلا لمسة ولا إغلاق للوحة.
    var editingPass by remember { mutableStateOf(false) }

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
                focusRequester = userFocus,
                // ① يفتح نفسه: النافذة لا عمل فيها إلا الكتابة
                startEditing = true,
                // «التالي» ينقل بلا إغلاق اللوحة
                onImeNext = {
                    runCatching { passFocus.requestFocus() }
                    editingPass = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OwnTVTextField(
                value = pass,
                onValueChange = { pass = it },
                label = stringResource(R.string.salamtv_login_password),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                focusRequester = passFocus,
                requestEditing = editingPass,
                onImeDone = { if (canSubmit) onSubmit(user.trim(), pass) },
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
                    onClick = { if (canSubmit) onSubmit(user.trim(), pass) },
                    enabled = canSubmit,
                )
            }
        }
    }
}
