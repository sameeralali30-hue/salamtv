package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.ui.theme.LocalGlass
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.glass

/**
 * A remote-friendly single-line text field, TV-style and two-stage: D-pad focus only *highlights*
 * the field (no keyboard), so you can move past it to other controls / a Save button freely. Press
 * **OK** to start editing (the keyboard appears); **Back** or the IME's Done returns to the field
 * without leaving the form. This mirrors the search bars and fixes the "keyboard pops up on every
 * field and traps you" problem on Fire TV / Android TV.
 *
 * When [isPassword] is true, a show/hide eye button appears on the right of the field and is
 * independently D-pad focusable so the user can reveal the password without a keyboard.
 *
 * Glass effect: when [surface] is in scope the field frosts and gains a white glass rim on focus;
 * otherwise it keeps its solid tonal fill + accent/outline border. Defaults to [GlassSurface.CARDS]
 * (most fields live on a panel); pass [GlassSurface.DIALOGS] for a field inside a popup/dialog.
 *
 * @param surface glass surface to frost with, or null to stay flat.
 */
@Composable
fun OwnTVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    focusRequester: FocusRequester? = null,
    surface: GlassSurface? = GlassSurface.CARDS,
    /** Corner radius of the field; a pill reads better where the field sits inline in a header. */
    corner: androidx.compose.ui.unit.Dp = 12.dp,
    /**
     * ما يفعله مفتاح ✓ بعد إغلاق اللوحة — أو لا شيء، وهو الأصل.
     *
     * يخصّ الشاشات التي يقع فيها زرّ الإرسال تحت لوحة المفاتيح: في الوضع
     * الأفقي على الهاتف تأخذ اللوحة ثلثي الارتفاع، فيبقى الزرّ خلفها ولا
     * يُبلغ إلا بإغلاقها. ومفتاح ✓ في متناول الإبهام أصلاً.
     */
    onImeDone: (() -> Unit)? = null,
    /**
     * ما يفعله مفتاح «التالي» — وحين يُمرَّر يصير المفتاح «التالي» لا ✓.
     *
     * ⚠ بلا هذا كان الانتقال من حقلٍ إلى الذي تحته يعني: ✓ فتُغلق اللوحة،
     *   ثمّ لمسة على الحقل التالي، ثمّ انتظار اللوحة وهي تُفتح من جديد.
     *   ثلاث حركات ومئات الأجزاء من الثانية بين كلّ حقلين — وهو ما يجعل
     *   نافذةً من حقلين تبدو بطيئة وثقيلة.
     */
    onImeNext: (() -> Unit)? = null,
    /**
     * يبدأ الحقل في وضع الكتابة، فتُفتح لوحة المفاتيح مع الشاشة.
     *
     * ⚠ فتحُ لوحة المفاتيح كلّف نحو ثانيتين حين قِستُه — وأكثرها إقلاعُ
     *   اللوحة نفسها لا التطبيق. فما نملكه ليس تسريعَها بل ألّا ندفع ثمنها
     *   مرّتين: نافذةٌ لا عمل فيها إلا الكتابة تفتح حقلها الأوّل جاهزاً،
     *   فتسقط لمسةٌ وانتظارٌ كامل من الطريق.
     *
     *   لا تُستعمل إلا حيث يكون الحقل هو الغرض الوحيد من الشاشة.
     */
    startEditing: Boolean = false,
    /**
     * ارفعها ليدخل الحقل وضع الكتابة — لنقل الكتابة من حقلٍ إلى الذي تحته.
     *
     * ⚠ [focusRequester] لا يكفي: هو مربوطٌ بغلاف الحقل لا بحقل الكتابة
     *   داخله. فمفتاح «التالي» كان ينقل التركيز إلى الغلاف وتُغلق لوحة
     *   المفاتيح، فيضطرّ المشترك إلى لمس الحقل على كلّ حال — أسوأ ممّا كان
     *   قبل «التالي». رأيتُه على الجهاز: الاسم نزل والكلمة لم تنزل.
     */
    requestEditing: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val fieldFocused by interaction.collectIsFocusedAsState()
    var editing by remember { mutableStateOf(startEditing) }
    // True once the inner field has genuinely held focus — see onFocusChanged below.
    var hadFocus by remember { mutableStateOf(false) }
    val pillFocus = remember { FocusRequester() }
    val innerFocus = remember { FocusRequester() }
    val bringIntoView = remember { BringIntoViewRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val tvImeWatcher = LocalTvImeWatcher.current
    val tvImeMetrics = LocalTvImeMetrics.current
    val shape = RoundedCornerShape(corner)
    val focused = fieldFocused || editing
    var showPassword by remember { mutableStateOf(false) }
    val eyeInteraction = remember { MutableInteractionSource() }
    val eyeFocused by eyeInteraction.collectIsFocusedAsState()

    // Glassy only when a surface is given and it's in the active glass scope (matches FocusableSurface).
    val glassy = surface != null && LocalGlass.current.isGlassy(surface)
    // Glass edge: a faint white rim lenses the whole edge at all times, brightening on focus.
    // Focused = where the remote is, so it follows the user's focus highlight (#121) in both
    // materials; glass keeps its faint white hairline for the idle edge.
    val borderColor = when {
        focused || eyeFocused -> colors.focusBorder
        glassy -> Color.White.copy(alpha = 0.22f)
        else -> colors.outlineVariant
    }

    LaunchedEffect(requestEditing) { if (requestEditing) editing = true }
    LaunchedEffect(editing) {
        if (editing) {
            // Tell the shared popup before showing the IME. If this TV publishes no inset/frame
            // change, the calibrated estimate still constrains the modal immediately.
            tvImeWatcher?.onImeRequested()
            // Wait one frame before asking for focus. `canFocus = editing` only becomes true on
            // the recomposition that this same flag triggers, so a requestFocus() issued in the
            // same frame runs against canFocus=false and is silently dropped. On a remote that
            // was survivable — focus was already on the pill next to it. On a touchscreen nothing
            // holds focus at all, so the request failed, no view was ever served to the IME, and
            // the field could not be typed into: keyboard never opened, only paste worked.
            withFrameNanos { }
            runCatching { innerFocus.requestFocus() }
            keyboard?.show()
            kotlinx.coroutines.delay(120)
            runCatching { bringIntoView.bringIntoView() }
        } else {
            tvImeWatcher?.onImeDismissed()
        }
    }
    // A measured inset/frame can arrive after the keyboard animation. Bring the existing field into
    // the dialogPanel scroll viewport again using the final geometry.
    LaunchedEffect(editing, tvImeMetrics.keyboardTopPx, tvImeMetrics.visible) {
        if (editing && tvImeMetrics.visible) {
            kotlinx.coroutines.delay(32)
            runCatching { bringIntoView.bringIntoView() }
        }
    }
    Column(modifier = modifier) {
        // A blank label means the field is captioned by where it sits (an inline header), and an
        // empty caption line would just make that header two rows tall.
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .then(
                    if (surface != null) Modifier.glass(surface = surface, baseFill = colors.surfaceContainerHigh, shape = shape, frostScale = 0.6f)
                    else Modifier.background(colors.surfaceContainerHigh)
                )
                .border(
                    width = if (focused || eyeFocused) tv.own.owntv.ui.theme.LocalFocusBorderWidth.current else 1.dp,
                    color = borderColor,
                    shape = shape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .focusRequester(pillFocus)
                    .clickable(interactionSource = interaction, indication = null) { editing = true },
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .bringIntoViewRequester(bringIntoView)
                    .focusRequester(innerFocus)
                        .focusProperties { canFocus = editing }
                        // Only leave editing once focus has actually been held and then lost.
                        // The first callback after `editing` flips arrives with isFocused=false —
                        // treating that as "focus lost" reset the flag immediately and undid the
                        // focus request that was still in flight.
                        .onFocusChanged {
                            if (it.isFocused) hadFocus = true
                            else if (hadFocus) { hadFocus = false; editing = false }
                        }
                        .onPreviewKeyEvent {
                            if (it.key == Key.Back) {
                                if (it.type == KeyEventType.KeyUp) {
                                    editing = false
                                    keyboard?.hide()
                                    runCatching { pillFocus.requestFocus() }
                                }
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = if (onImeNext != null) ImeAction.Next else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            editing = false
                            keyboard?.hide()
                            runCatching { pillFocus.requestFocus() }
                            onImeDone?.invoke()
                        },
                        // «التالي» لا يُغلق اللوحة: الحقل التالي يفتح نفسه.
                        onNext = { onImeNext?.invoke() },
                    ),
                    visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
                )

                // ═══ لماذا طبقةٌ فوق الحقل ═══
                //
                // اللمسة على حقل نصّ تصل إلى BasicTextField أولاً، وهو يبتلعها (يستخدمها
                // لوضع المؤشّر) فلا تصل أبداً إلى clickable الموضوع على الحاوية — والحاوية
                // هي التي تضبط `editing`. وبما أنّ `canFocus = editing` يبقى false، لا يُخدَم
                // أيّ عرضٍ للوحة المفاتيح: لا تظهر اللوحة، ولا يُكتَب حرف، ويبقى اللصق
                // الطريقة الوحيدة لملء الحقل. على الريموت لم يظهر العطل لأنّ زرّ OK حدث
                // مفتاحٍ لا لمسة، فيمرّ إلى clickable مباشرة.
                //
                // فنضع طبقةً شفّافة تلتقط اللمسة الأولى قبل الحقل. وتزول فور بدء التحرير،
                // فتذهب اللمسات التالية إلى الحقل نفسه لوضع المؤشّر وتحديد النصّ كالمعتاد.
                // وهي غير قابلة للتركيز حتى لا تضيف محطّةً زائدة في مسار الريموت.
                if (!editing) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .focusProperties { canFocus = false }
                            .clickable(interactionSource = interaction, indication = null) { editing = true }
                    )
                }
            }

            if (isPassword) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(interactionSource = eyeInteraction, indication = null) { showPassword = !showPassword }
                        .onPreviewKeyEvent {
                            if (it.key == Key.DirectionCenter && it.type == KeyEventType.KeyUp) {
                                showPassword = !showPassword
                                true
                            } else false
                        },
                ) {
                    Text(
                        text = if (showPassword) stringResource(R.string.common_hide) else stringResource(R.string.common_show),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (eyeFocused) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
