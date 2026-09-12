package tv.own.owntv.features.salamtv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.dialogPanel
import tv.own.owntv.ui.components.modalScrim
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.animationsOn

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  هويّة SalamTV البصريّة — ما تشترك فيه صفحة الهبوط والتطبيق
 * ───────────────────────────────────────────────────────────────────────────
 *  ① [AuroraBackground] الخلفيّة الحيّة نفسها التي في صفحة الهبوط: ثلاث بقع
 *    (فيروزيّ، أزرق، ذهبيّ) تنجرف ببطء. Canvas واحد لا ثلاث طبقات مطموسة —
 *    التلفاز الرخيص لا يحتمل blur حقيقيّاً، والتدرّج الشعاعيّ يعطي الأثر نفسه.
 *    تقف تماماً حين يُطفئ المستخدم الحركة ([animationsOn]).
 *  ② [SalamTVNoticeDialog] رسالة الحالة الموحّدة (نفاد الوقت، انتهاء الاشتراك،
 *    موقوف): أيقونة، عنوان، سطر واحد، وزرّ تواصل. على الهاتف يفتح واتساب،
 *    وعلى التلفاز — حيث لا متصفّح — يبقى الرقم مقروءاً. الرقم من اللوحة.
 *  ③ [SalamTVOnboarding] ثلاث شرائح قبل الدخول ثمّ التنبيه القانونيّ في
 *    الرابعة. بالريموت: التالي/رجوع؛ لا سحب. تُعرض مرّةً (أوّل تشغيل).
 * ═══════════════════════════════════════════════════════════════════════════
 */

private val Gold = Color(0xFFF0C060)

/** ألوان الهويّة تتبع لون التمييز الذي يختاره المستخدم — لا لوناً ثابتاً. */
@Composable private fun accentA(): Color = OwnTVTheme.colors.primary
@Composable private fun accentB(): Color = OwnTVTheme.colors.primaryContainer.let { c -> Color(c.red * .5f + OwnTVTheme.colors.primary.red * .5f, c.green * .5f + OwnTVTheme.colors.primary.green * .5f, c.blue * .5f + OwnTVTheme.colors.primary.blue * .5f) }

@Composable
fun AuroraBackground(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val on = animationsOn
    val t = if (on) {
        val inf = rememberInfiniteTransition(label = "aurora")
        inf.animateFloat(
            initialValue = 0f, targetValue = 1f, label = "drift",
            animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        ).value
    } else 0.35f
    val bg = OwnTVTheme.colors.background
    val a = accentA(); val b = accentB()
    Canvas(modifier.fillMaxSize().background(bg)) {
        val w = size.width; val h = size.height
        fun blob(c: Color, cx: Float, cy: Float, r: Float, a: Float) {
            drawCircle(
                brush = Brush.radialGradient(listOf(c.copy(alpha = a * intensity), Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r, center = Offset(cx, cy),
            )
        }
        blob(a, w * (0.85f - 0.12f * t), h * (0.05f + 0.15f * t), w * 0.55f, 0.30f)
        blob(b, w * (0.12f + 0.10f * t), h * (0.95f - 0.20f * t), w * 0.50f, 0.22f)
        blob(Gold, w * (0.45f + 0.15f * t), h * (0.50f - 0.10f * t), w * 0.32f, 0.10f)
    }
}

/** اسم العلامة بتدرّج فيروزيّ→أزرق كما في الهبوط. */
@Composable
fun BrandGradientTitle(size: Int = 44, modifier: Modifier = Modifier) {
    val own = stringResource(R.string.brand_own)
    val tv = stringResource(R.string.brand_tv)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(own, style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black, color = OwnTVTheme.colors.onSurface))
        Text(tv, style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black, brush = Brush.linearGradient(listOf(accentA(), Gold))))
    }
}

/**
 * ② زرّ التواصل: يحاول واتساب، ثمّ أيّ متصفّح، وإلّا يترك الرقم مقروءاً.
 * على التلفاز غالباً لا شيء يفتح — وهذا متوقّع؛ الرقم نفسه في العنوان.
 */
@Composable
fun WhatsAppButton(number: String, modifier: Modifier = Modifier, style: OwnTVButtonStyle = OwnTVButtonStyle.PRIMARY) {
    if (number.isBlank()) return
    val ctx = LocalContext.current
    val digits = number.filter { it.isDigit() }
    OwnTVButton(
        stringResource(R.string.salamtv_whatsapp_button, number),
        style = style,
        modifier = modifier,
        onClick = {
            val url = "https://wa.me/$digits"
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { e -> if (e !is ActivityNotFoundException) throw e }
        },
    )
}

@Composable
fun SalamTVNoticeDialog(
    emoji: String,
    title: String,
    body: String,
    contact: String,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.salamtv_out_of_time_ok),
) {
    val colors = OwnTVTheme.colors
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    androidx.activity.compose.BackHandler { onDismiss() }
    Box(Modifier.fillMaxSize().modalScrim(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.dialogPanel(width = 500.dp, padding = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).background(Brush.linearGradient(listOf(accentA().copy(alpha = .25f), Gold.copy(alpha = .25f))), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, style = TextStyle(fontSize = 30.sp)) }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (contact.isNotBlank()) WhatsAppButton(contact, Modifier.focusRequester(fr))
                OwnTVButton(dismissLabel, onClick = onDismiss, style = if (contact.isNotBlank()) OwnTVButtonStyle.SECONDARY else OwnTVButtonStyle.PRIMARY,
                    modifier = if (contact.isBlank()) Modifier.focusRequester(fr) else Modifier)
            }
        }
    }
}

/** ③ الشرائح: عنوان، سطر، رمز. الرابعة هي التنبيه القانونيّ. */
private data class Slide(val emoji: Int, val title: Int, val body: Int)

@Composable
fun SalamTVOnboarding(onDone: () -> Unit) {
    val slides = remember {
        listOf(
            Slide(R.string.salamtv_ob1_emoji, R.string.salamtv_ob1_title, R.string.salamtv_ob1_body),
            Slide(R.string.salamtv_ob2_emoji, R.string.salamtv_ob2_title, R.string.salamtv_ob2_body),
            Slide(R.string.salamtv_ob3_emoji, R.string.salamtv_ob3_title, R.string.salamtv_ob3_body),
            Slide(R.string.salamtv_ob4_emoji, R.string.salamtv_ob4_title, R.string.salamtv_ob4_body),
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val last = index == slides.lastIndex
    val colors = OwnTVTheme.colors
    val nextFocus = remember { FocusRequester() }
    LaunchedEffect(index) { runCatching { nextFocus.requestFocus() } }
    androidx.activity.compose.BackHandler(enabled = index > 0) { index-- }
    val on = animationsOn

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandGradientTitle(size = 34)
            Spacer(Modifier.height(26.dp))
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    if (!on) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else {
                        val dir = if (targetState > initialState) -1 else 1
                        (slideInHorizontally(tween(320)) { it / 6 * dir } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(220)))
                    }
                },
                label = "slide",
            ) { i ->
                val s = slides[i]
                Column(Modifier.widthIn(max = 640.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(s.emoji), style = TextStyle(fontSize = 64.sp))
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(s.title), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(s.body), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slides.indices.forEach { i ->
                    Box(
                        Modifier.height(6.dp).width(if (i == index) 26.dp else 8.dp)
                            .background(if (i == index) colors.primary else colors.outlineVariant, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!last) {
                    OwnTVButton(stringResource(R.string.salamtv_ob_skip), onClick = { index = slides.lastIndex }, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.width(140.dp))
                    OwnTVButton(stringResource(R.string.salamtv_ob_next), onClick = { index++ }, modifier = Modifier.width(160.dp).focusRequester(nextFocus))
                } else {
                    OwnTVButton(stringResource(R.string.common_back), onClick = { index-- }, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.width(140.dp))
                    OwnTVButton(stringResource(R.string.setup_i_understand), onClick = onDone, modifier = Modifier.width(200.dp).focusRequester(nextFocus))
                }
            }
        }
    }
}
