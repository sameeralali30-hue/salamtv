package tv.own.owntv.features.setup

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Subscriber sign-in — the only setup screen in a locked build.
 *
 * Two fields. No server address, no playlist type, no "add source": the account decides which host
 * the subscriber uses, which channels they see, and at what resolution, and all three are resolved
 * on the panel. The app is not asked to know any of it.
 *
 * ═══ Three decisions ═══
 *
 * ① The failure message comes from the panel and is shown verbatim.
 *    "Your subscription expired — contact your reseller" is the operator's wording and changes
 *    with the operator's policy. The app only supplies a fallback for the one case the panel can
 *    never answer: it was unreachable.
 *
 * ② The button stays enabled while both fields have content, and disables only during the call.
 *    A greyed-out button with no explanation is the most common reason a subscriber phones support.
 *
 * ③ No "forgot password" and no "create account".
 *    Accounts are issued by the operator or a reseller from the panel. Offering either here would
 *    promise a flow that does not exist.
 */
@Composable
fun SubscriberLoginScreen(
    onSubmit: (username: String, password: String) -> Unit,
    busy: Boolean,
    /** Panel wording for the last refusal, or blank. */
    errorMessage: String,
    /** True when the last failure was transport, not credentials — needs the app's own wording. ① */
    offline: Boolean,
    onBack: (() -> Unit)? = null,
    /** يفتح نافذة تفعيل الرمز — لمن اشترى رمزاً من وكيل ولم يُفعّله بعد. */
    onRedeem: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }
    RequestInitialFocus(firstFocus)
    onBack?.let { BackHandler { it() } }

    val canSubmit = username.isNotBlank() && password.isNotBlank() && !busy

    // ═══ ترتيبٌ ثانٍ حين تظهر لوحة المفاتيح ═══
    //
    // التطبيق مقفولٌ على الوضع الأفقي، ولوحة المفاتيح هناك تأخذ نحو ثلثي
    // الارتفاع. فما يبقى شريطٌ لا يتّسع للعنوان والحقلين والزرّ معاً، وكان
    // العنوان يأخذه كلّه فيبقى الحقلان تحت الطيّة.
    //
    // فحين تظهر اللوحة: يسقط العنوان والشرح والتذييل — وكلّها قد قُرئت
    // قبل أن يبدأ الكتابة — ويقف الحقلان جنباً إلى جنب بدل واحدٍ تحت آخر.
    // فيصير الارتفاع المطلوب حقلاً واحداً وزرّاً، وهو ما يتّسع له الشريط.
    val compact = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // ═══ لوحة المفاتيح كانت تبتلع نصف الشاشة ═══
    //
    // ⚠ في الوضع الأفقي على الهاتف تغطّي اللوحةُ حقلَ كلمة المرور وزرّ الدخول
    //   معاً، فتهبط نقرة المشترك على مفاتيحها لا على ما قصده. العمود يمرّر
    //   أصلاً (verticalScroll) لكنّه لا يعرف أنّ اللوحة ظهرت — و`imePadding`
    //   على الصندوق الحاوي هي ما يُقلّص المساحة فيصير التمرير ذا معنى.
    //
    //   نفس النمط المستعمل في نوافذ الإعدادات (SettingsScreen.kt).
    Box(
        modifier.fillMaxSize().imePadding().roundedPanel().background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 24.dp else 40.dp, vertical = if (compact) 12.dp else 40.dp)
                .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(460.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compact) {
                Text(
                    stringResource(R.string.salamtv_login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.salamtv_login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            }

            // ⚠ أوّل محاولة وضعت الحقلين في صفٍّ حين تظهر اللوحة وفي عمودٍ حين
            //   تختفي. والنتيجة أنّ اللوحة لم تظهر أصلاً: الفرعان موضعان
            //   مختلفان في شجرة التأليف، فالحقل الذي يظهر في أحدهما غير الذي
            //   في الآخر، وحالته «قيد الكتابة» تسقط معه — فتُغلق اللوحة فور
            //   فتحها، ويعود الترتيب، وهكذا في حلقة.
            //
            //   الحقلان إذن في موضع واحد لا يتغيّر، والذي يُخفى ما حولهما.
            OwnTVTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.salamtv_login_username),
                focusRequester = firstFocus,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OwnTVTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.salamtv_login_password),
                isPassword = true,
                keyboardType = KeyboardType.Password,
                // ✓ يُرسل: الزرّ خلف لوحة المفاتيح في الوضع الأفقي.
                onImeDone = { if (canSubmit) onSubmit(username.trim(), password) },
                modifier = Modifier.fillMaxWidth(),
            )

            // ① panel wording, or the one case the panel could not answer.
            val shown = when {
                offline -> stringResource(R.string.salamtv_login_offline)
                errorMessage.isNotBlank() -> errorMessage
                else -> ""
            }
            if (shown.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    shown,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                onBack?.let {
                    OwnTVButton(
                        stringResource(R.string.common_back),
                        onClick = it,
                        style = OwnTVButtonStyle.SECONDARY,
                    )
                }
                OwnTVButton(
                    stringResource(
                        if (busy) R.string.salamtv_login_working else R.string.salamtv_login_action,
                    ),
                    onClick = { if (canSubmit) onSubmit(username.trim(), password) },
                    enabled = canSubmit,     // ②
                )
            }

            if (onRedeem != null && !compact) {
                Spacer(Modifier.height(14.dp))
                OwnTVButton(
                    stringResource(R.string.salamtv_redeem_open),
                    onClick = onRedeem,
                    style = OwnTVButtonStyle.SECONDARY,
                )
            }

            if (!compact) {
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.salamtv_login_help),   // ③
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            }
        }
    }
}

/** يضع التركيز على أول حقل عند الظهور — المشروع يعرّف مثيلتها خاصّةً في ملف آخر. */
@Composable
private fun RequestInitialFocus(fr: FocusRequester) {
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
}
