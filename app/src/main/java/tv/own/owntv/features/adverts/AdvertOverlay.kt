package tv.own.owntv.features.adverts

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.adverts.AdvertDecision
import tv.own.owntv.core.adverts.AdvertOutcome
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.theme.OwnTVTheme
import java.io.File

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  الطبقة التي تعرض الإعلان.
 * ───────────────────────────────────────────────────────────────────────────
 *  ⚠ عقدٌ واحد يحكم هذا الملفّ: **[onFinished] تُنادى في كلّ مسارٍ بلا استثناء.**
 *    من ينتظرها هو [tv.own.owntv.features.live.LiveViewModel.playChannel]،
 *    وهو معلّقٌ حتّى تصل. مسارٌ واحد لا يُناديها يعني قناةً لا تُفتح أبداً
 *    وشاشةً سوداء بلا رسالة. ولهذا الحارس ⑤ والإيقاف القسريّ ⑥ ليسا تحسيناً.
 *
 *  ═══ ستّة قرارات ═══
 *
 *  ① مشغّل مستقلّ قصير العمر.
 *     لا `LivePreviewEngine` — كلّ آلته (مراقبات التعثّر، سلّم التراجع إلى
 *     mpv، إعدادات الحافة الحيّة) مبنيّةٌ على بثٍّ لا ينتهي، وملفٌّ محلّيّ يبلغ
 *     `STATE_ENDED` بعد ٢٥ ثانية سيوقظها كلّها.
 *     ولا mpv — هو **السطح الآخر**، والانتقال بينهما يعيد إنشاء `SurfaceView`
 *     ويُظهر ومضةً سوداء، وهو ما يوجد رفعُ السطح في `OwnTVShell` لمنعه.
 *
 *  ② ثلاث آليّات لمنع التخطّي، لا واحدة.
 *     ⚠ خلطها هو مصدر التسريب:
 *       • `inert` في `PlayerHud` يمنع **طلب التركيز** ولا يمنع معالجة المفاتيح.
 *       • فجذر الطبقة يبتلع كلّ مفتاح بـ`onPreviewKeyEvent { true }`.
 *       • والرجوع له `BackHandler` خاصّ، وCompose يوزّع من الأعمق فيحجب
 *         `exitPlayer` في الغلاف.
 *
 *  ③ مخرجٌ موثّق: رجوعٌ مطوّل ثلاث ثوانٍ.
 *     ليس تحايلاً: يُسجَّل `aborted`، و**لا تُفتح القناة**، ولا يُمنح رصيد.
 *     فلا يربح المستخدم منه شيئاً سوى الخروج ممّا دخله. وبدونه يصير الإغلاق
 *     القسريّ هو المخرج الوحيد — وهو أسوأ نتيجةٍ ممكنة: يُعلّم الناس أنّ قتل
 *     التطبيق يُخلّصهم من الإعلان، ثمّ يحذفونه.
 *
 *  ④ العدّاد يبدأ حين تظهر الصورة، والإيقاف القسريّ من ساعة الجدار.
 *     فحصان لغرضين مختلفين، ولا يجوز خلطهما:
 *       • «التخطّي بعد ٥» يجب أن يُقاس ممّا رآه المشاهد، لا من لحظةٍ كانت
 *         الشاشة فيها سوداء تُهيّئ المُفكِّك.
 *       • والإيقاف القسريّ ⑥ يُقاس من ساعة الجدار مهما فعل المشغّل، وإلّا
 *         تجمّد مع موضعه وبقيت الطبقة إلى الأبد.
 *
 *  ⑤ حارس الاستعداد — إلزاميّ.
 *     ⚠ ملفٌّ تالف واحد بلا هذا الحارس يُعطّل **كلّ فتحِ قناةٍ على كلّ جهازٍ في
 *       الميدان**، ولا علاج إلّا مفتاح القتل من اللوحة.
 *
 *     ⚠ وكانت مهلته ١٥٠٠ م.ث بناءً على أنّ «ملفّاً محلّيّاً يستعدّ في عشرات
 *       الملّي ثانية». وهذا صحيحٌ عن فكّ الترميز وخطأٌ عن الإقلاع: أوّل
 *       `ExoPlayer` في العمليّة يبني `MediaCodec` من الصفر. قيست فعليّاً على
 *       المحاكي فتجاوزت ثانيتين، فكان كلّ إعلانٍ أوّل يُلغى بـ`ERROR`.
 *       أربع ثوانٍ تبقى إشارةَ عطلٍ حقيقيّة لملفٍّ على القرص، ودون عتبة ANR،
 *       وداخل مهلة الإيقاف القسريّ. والزمن المقيس يُكتب في السجلّ فيُعرف
 *       الرقم الحقيقيّ على أجهزة الميدان بدل أن يُخمَّن ثانيةً.
 *
 *  ⑥ إيقافٌ قسريّ عند الاستعداد + المدّة + خمس ثوانٍ.
 *     شبكة الأمان الأخيرة: مهما قال المشغّل، الطبقة تنتهي.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@OptIn(UnstableApi::class)
@Composable
fun AdvertOverlay(
    decision: AdvertDecision,
    channelName: String,
    onFinished: (AdvertOutcome, Long) -> Unit,
) {
    val colors = OwnTVTheme.colors
    val spot = decision.spot

    /** ④ ثوانٍ مضت منذ **ظهور الصورة** — لا منذ ظهور الطبقة. */
    var elapsed by remember(decision.eventUid) { mutableIntStateOf(0) }
    var ready by remember(decision.eventUid) { mutableStateOf(spot.kind == "image") }
    var done by remember(decision.eventUid) { mutableStateOf(false) }
    val startedAt = remember(decision.eventUid) { System.currentTimeMillis() }

    /* ═══ لماذا يُحرَّر المشغّل من هنا لا من onDispose وحده ═══

       كان التحرير معلّقاً بـ`onDispose` في [AdvertVideo]، أي أنّه لا يقع حتى
       تُزيل Compose الطبقةَ في إعادة تركيبٍ لاحقة. وبين انتهاء الإعلان وتلك
       اللحظة يكون النموذج قد استأنف ضبطَ القناة سلفاً.

       ⚠ فينفتح مُفكِّكا ترميزٍ معاً للحظة. وعلى جهازٍ لا يملك إلا واحداً —
         المحاكي، والصناديق الرخيصة التي كُتب من أجلها هذا كلّه — يفشل الثاني
         في الفتح، فلا تأتي صورةُ القناة أبداً. قِسته: بعد كلّ إعلانٍ يكتمل
         تعطب أوّل محاولةِ فتح: «no picture within 30s of tuning»، ثمّ تنجح
         إعادةُ المحاولة لأنّ الطبقة تكون قد زالت وحُرِّر المشغّل.

       فالتحرير يسبق التبليغ. و`release()` يحجز الخيط حتى يفرغ الخيط الداخليّ،
       فحين تعود `onFinished` يكون المُفكِّك متاحاً فعلاً لا وعداً. */
    val releasePlayer = remember(decision.eventUid) {
        java.util.concurrent.atomic.AtomicReference<(() -> Unit)?>(null)
    }

    /** يضمن أنّ [onFinished] لا تُنادى مرّتين مهما تسابقت المسارات. */
    val finish: (AdvertOutcome) -> Unit = { outcome ->
        if (!done) {
            done = true
            Log.i(TAG, "advert #${spot.id} finishing: $outcome at ${elapsed}s")
            runCatching { releasePlayer.getAndSet(null)?.invoke() }
            onFinished(outcome, elapsed * 1000L)
        }
    }

    val canSkip = spot.isSkippable && elapsed >= spot.skipAfterSecs
    val remaining = (spot.durationSecs - elapsed).coerceAtLeast(0)

    // ④ العدّاد لا يمضي قبل أن تظهر صورة: زرّ «تخطٍّ بعد ٥» يجب أن يُقاس
    //    ممّا رآه المشاهد لا من سوادٍ كان المُفكِّك يُبنى فيه.
    LaunchedEffect(decision.eventUid, ready) {
        if (!ready) return@LaunchedEffect
        while (true) {
            delay(1000)
            elapsed += 1
        }
    }

    /* ⑥ الإيقاف القسريّ — يُقاس من **ظهور الصورة** لا من ظهور الطبقة.

       ⚠ كان يُقاس من ظهور الطبقة ويضيف READY_TIMEOUT_MS كاملاً إلى الميزانيّة،
         أي أنّ إعلاناً مدّته عشر ثوانٍ يبقى ٨+١٠+٥ = ٢٣ ثانية مهما استعدّ
         المشغّل بسرعة. قِسته: استعدّ في ٣٤٦٠ م.ث وانتهى عند ٢٣ ثانية — فأكثر
         من أربع ثوانٍ ضاعت لأنّ الميزانيّة حجزت زمن استعدادٍ لم يُستهلك.

         والفارق ليس تجميليّاً: من يبيع «إعلان ثلاثين ثانية» يعرضه ثلاثاً
         وأربعين، ومن يُحسب عليه الانتظار هو المشاهد.

       والحارس ⑤ يبقى مستقلّاً: ما لم تظهر صورةٌ أصلاً تُفتح القناة بـERROR. */
    LaunchedEffect(decision.eventUid, ready) {
        if (!ready) return@LaunchedEffect
        delay((spot.durationSecs + HARD_STOP_GRACE_SECS) * 1000L)
        Log.w(TAG, "advert #${spot.id} hard stop after ${spot.durationSecs + HARD_STOP_GRACE_SECS}s")
        finish(AdvertOutcome.COMPLETED)
    }

    // ⑤ الحارس: ما لم يستعدّ المشغّل في الوقت، تُفتح القناة ويُسجَّل عطل.
    LaunchedEffect(decision.eventUid) {
        delay(READY_TIMEOUT_MS)
        if (!ready) {
            Log.w(TAG, "advert #${spot.id} not ready within ${READY_TIMEOUT_MS}ms — opening the channel")
            finish(AdvertOutcome.ERROR)
        }
    }

    /* ⚠ طلب التركيز مرّةً واحدة في LaunchedEffect لا يكفي.
         `requestFocus` يفشل صامتاً إن لم يكن هدفُ التركيز قد رُكِّب بعد، و
         `runCatching` كان يبتلع الفشل فلا يبقى منه أثر. والنتيجة أنّ الطبقة
         تُرسم ولا تستقبل مفتاحاً واحداً: لا تخطّي بـOK، ولا ابتلاعَ لبقيّة
         المفاتيح — أي أنّ الحماية كلّها كانت معطّلة بلا عَرَض ظاهر.

         المحاولة تتكرّر بضعة إطارات، ونتيجتها تُكتب في السجلّ فلا تعود
         الحالةُ تُخمَّن. */
    val focus = remember { FocusRequester() }
    var focusHeld by remember(decision.eventUid) { mutableStateOf(false) }
    LaunchedEffect(decision.eventUid) {
        repeat(FOCUS_ATTEMPTS) { attempt ->
            if (focusHeld) return@LaunchedEffect
            runCatching { focus.requestFocus() }
                .onFailure { Log.w(TAG, "advert focus attempt ${attempt + 1} failed: ${it.javaClass.simpleName}") }
            delay(FOCUS_RETRY_MS)
        }
        if (!focusHeld) Log.w(TAG, "advert overlay never took focus — keys are not trapped")
    }

    /* ③ الرجوع المطوّل — بوظيفةٍ مؤقّتة لا بعدّ تكرارات المفتاح.
         ⚠ قياسُ المدّة من تكرار أحداث KeyDown يفترض أنّ النظام يُكرّرها ما دام
           الزرّ مضغوطاً. وهذا يصحّ على أجهزةٍ ولا يصحّ على أخرى، وبعض أجهزة
           التحكّم لا تُكرّر Back أصلاً — فيصير المخرج الوحيد غيرَ موجود على
           الجهاز الذي يحتاجه.
           الوظيفة تبدأ عند الضغط وتُلغى عند الرفع، فلا تعتمد على شيء.
           وهو نفس نمط `backHoldJob` المستعمل في `OwnTVShell`. */
    val scope = rememberCoroutineScope()
    var backHoldJob by remember(decision.eventUid) { mutableStateOf<Job?>(null) }
    DisposableEffect(decision.eventUid) { onDispose { backHoldJob?.cancel() } }

    // ② الرجوع: أعمق من BackHandler الغلاف فيحجب exitPlayer.
    BackHandler(enabled = true) { /* لا شيء: الخروج بالضغط المطوّل وحده */ }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .onFocusChanged { state ->
                if (state.isFocused && !focusHeld) {
                    focusHeld = true
                    Log.i(TAG, "advert overlay holds focus")
                }
            }
            .focusable()
            // ② ابتلاع كلّ مفتاح: `inert` يمنع طلب التركيز ولا يمنع
            //    معالجات `PlayerHud` من العمل.
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (backHoldJob == null) {
                                backHoldJob = scope.launch {
                                    delay(HOLD_BACK_MS)
                                    finish(AdvertOutcome.ABORTED)   // ③
                                }
                            }
                        }
                        KeyEventType.KeyUp -> {
                            backHoldJob?.cancel()
                            backHoldJob = null
                        }
                        else -> Unit
                    }
                    return@onPreviewKeyEvent true
                }
                if (canSkip && event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    finish(AdvertOutcome.SKIPPED)
                    return@onPreviewKeyEvent true
                }
                true
            },
    ) {
        if (spot.kind == "image") {
            AsyncImage(
                model = File(decision.localPath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AdvertVideo(
                path = decision.localPath,
                onReady = {
                    // الزمن المقيس يُكتب مرّةً: هو الرقم الذي يُضبط عليه الحارس
                    // لاحقاً، ولا يُعرف إلّا من أجهزة الميدان.
                    if (!ready) Log.i(TAG, "advert #${spot.id} ready in ${System.currentTimeMillis() - startedAt}ms")
                    ready = true
                },
                onEnded = { finish(AdvertOutcome.COMPLETED) },
                onError = { finish(AdvertOutcome.ERROR) },
                registerReleaser = { releasePlayer.set(it) },
            )
        }

        // ── الشارة والعدّاد ──
        Row(
            Modifier.align(Alignment.TopEnd).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(spot.label.ifBlank { stringResource(R.string.salamtv_advert_label) })
            Chip(stringResource(R.string.salamtv_advert_remaining, remaining))
        }

        // ── ما يُفتح بعده، فلا يظنّ المشاهد أنّه ضغط خطأً ──
        Column(
            Modifier.align(Alignment.BottomStart).padding(24.dp),
        ) {
            Text(
                stringResource(R.string.salamtv_advert_opening, channelName),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (!spot.isSkippable) {
                Spacer(Modifier.height(4.dp))
                // ③ المخرج يُقال، ولا يُترك ليُكتشف بالمصادفة.
                Text(
                    stringResource(R.string.salamtv_advert_hold_back),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (spot.isSkippable) {
            Box(Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                if (canSkip) {
                    OwnTVButton(
                        stringResource(R.string.salamtv_advert_skip),
                        onClick = { finish(AdvertOutcome.SKIPPED) },
                    )
                } else {
                    Chip(
                        stringResource(
                            R.string.salamtv_advert_skip_in,
                            (spot.skipAfterSecs - elapsed).coerceAtLeast(0),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * ① المشغّل المستقلّ: يُبنى مع الطبقة ويُحرَّر معها.
 *
 * `setVideoSurfaceHolder` لا `PlayerView`: لا نحتاج شيئاً من واجهته، وسطحٌ
 * عارٍ أخفّ وأوضح.
 */
@OptIn(UnstableApi::class)
@Composable
private fun AdvertVideo(
    path: String,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    /** يُسلّم المُناديَ وسيلةَ تحريرٍ مبكّر — انظر التعليق عند `releasePlayer`. */
    registerReleaser: (() -> Unit) -> Unit = {},
) {
    val context = LocalContext.current
    val player = remember(path) { ExoPlayer.Builder(context).build() }

    DisposableEffect(path) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> onReady()
                    Player.STATE_ENDED -> onEnded()
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // ⚠ العطل لا يُعرض للمشاهد ولا يُعاد المحاولة: تُفتح القناة.
                //   إعلانٌ لا يعمل خسارةُ انطباعٍ واحد؛ وقناةٌ لا تُفتح خسارةُ
                //   مشترك.
                onError()
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(File(path).toURI().toString()))
        player.playWhenReady = true
        player.prepare()

        // مرّةً واحدة لا مرّتين: التحرير المبكّر أو onDispose، أيّهما سبق.
        val released = java.util.concurrent.atomic.AtomicBoolean(false)
        val releaseOnce = {
            if (released.compareAndSet(false, true)) {
                player.removeListener(listener)
                player.release()
            }
        }
        registerReleaser(releaseOnce)

        onDispose { releaseOnce() }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(h: SurfaceHolder) = player.setVideoSurfaceHolder(h)
                    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) = Unit
                    override fun surfaceDestroyed(h: SurfaceHolder) = player.setVideoSurfaceHolder(null)
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private const val TAG = "SalamTVAds"

/**
 * ⑤ ما لم يبلغ المشغّل الاستعداد خلاله، تُفتح القناة.
 *
 * ⚠ لا تُنقص هذا الرقم استناداً إلى «الملفّ محلّيّ فيستعدّ فوراً». صحيحٌ عن
 *   فكّ الترميز، خطأٌ عن أوّل `ExoPlayer` في العمليّة: بناء `MediaCodec` من
 *   الصفر. المقيس على المحاكي: ٢٩٥٤ و٣٢٥٧ و٣٤٩٩ و٣٧٢٨ م.ث — ثمّ تشغيلةٌ
 *   تجاوزت الأربعة آلاف. راجع سطر `ready in …ms` قبل أيّ تعديل.
 *
 * ⚠ والأهمّ: هذا الحارس **ليس** الذي يمسك الملفّ التالف. ذاك يُمسك في
 *   `onPlayerError` فوراً وبلا انتظار. ما يمسكه هذا هو الحالة المتبقّية
 *   النادرة: مشغّلٌ لا يستعدّ ولا يُخطئ. فالتضييق عليه لا يشتري أماناً،
 *   ويشتري ضرراً حقيقيّاً: على جهازٍ بطيء يُلغى كلّ إعلانٍ صامتاً — يخسر
 *   المشغّلُ انطباعاته ولا يظهر في الشاشة عَرَض واحد.
 *
 *   والميزان يميل إلى السخاء لأنّ أجهزة الميدان صناديقُ رخيصة أبطأ من أيّ
 *   هاتف اختُبر عليه.
 */
private const val READY_TIMEOUT_MS = 8_000L

/** ⑥ فائضٌ فوق المدّة المعلنة قبل الإيقاف القسريّ. */
private const val HARD_STOP_GRACE_SECS = 5

/** ③ كم يُمسك زرّ الرجوع للإلغاء. */
private const val HOLD_BACK_MS = 3_000L

/** كم مرّة يُعاد طلب التركيز، وكم بينها — انظر التعليق عند [FocusRequester]. */
private const val FOCUS_ATTEMPTS = 8
private const val FOCUS_RETRY_MS = 120L
