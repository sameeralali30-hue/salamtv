package tv.own.owntv.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.features.adverts.AdvertOverlay
import tv.own.owntv.features.adverts.OutOfTimeDialog
import tv.own.owntv.PipBridge
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.features.subtitles.SubtitleSearchScreen
import tv.own.owntv.ui.components.BrowseMode
import tv.own.owntv.ui.components.StorageBrowser
import org.koin.compose.koinInject
import tv.own.owntv.core.subtitles.SubtitleController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import tv.own.owntv.player.ExoPreviewSurface
import tv.own.owntv.player.MpvPlaybackEngine
import tv.own.owntv.player.MpvVideoSurface
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.PlaybackEngine
import tv.own.owntv.player.SubtitleOverlay
import tv.own.owntv.player.TrackDialog
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

private const val CONTROLS_HIDE_MS = 4_000L

/**
 * مشغّل الهاتف.
 *
 * عموديّاً: الفيديو في الأعلى (16:9) وتحته العنوان، دليل الآن/التالي، وقائمة قنوات القسم —
 * كما يفعل يوتيوب. عرضيّاً (المشاهد قلب هاتفه): الفيديو ملء الشاشة، ولمسة تُظهر الأزرار أربع
 * ثوانٍ. المحرّك نفسه في الحالتين: السطح مرفوع إلى هنا فلا يُعاد إنشاؤه عند الدوران.
 *
 * ما يُرسم فوق الفيديو دائماً: الإعلان (لا يُغطّى بشيء) ونفاد الوقت — نفس مكوّنات التلفاز.
 */
@Composable
fun PhonePlayerScreen(
    playing: PhonePlaying,
    liveVm: LiveViewModel,
    player: OwnTVPlayer,
    contactMessage: String,
    onClose: () -> Unit,
    onSwitchChannel: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    val liveOnExo by liveVm.liveOnExo.collectAsStateWithLifecycle()
    val mpvEngine = remember(player) { MpvPlaybackEngine(player) }
    val engine: PlaybackEngine = if (liveOnExo) liveVm.previewEngine else mpvEngine
    val advert by liveVm.advert.collectAsStateWithLifecycle()
    val outOfTime by liveVm.outOfTime.collectAsStateWithLifecycle()
    val isLive = playing is PhonePlaying.Live
    val title = when (playing) {
        is PhonePlaying.Live -> playing.channel.name
        is PhonePlaying.Movie -> playing.movie.name
        is PhonePlaying.Episode -> stringResource(R.string.phone_episode, playing.episode.episodeNumber, playing.episode.name)
    }

    var controls by remember { mutableStateOf(true) }
    var showQuality by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }
    var showSubSearch by remember { mutableStateOf(false) }
    var showSubFile by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    // ما تُظهره الإيماءة لحظةً: «🔊 63%»، «☀ 40%»، «+10 ث»
    var gestureHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(gestureHint) { if (gestureHint != null) { delay(900); gestureHint = null } }
    val subtitleController = koinInject<SubtitleController>()
    val subtitleContext by subtitleController.current.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pipActive by PipBridge.active.collectAsStateWithLifecycle()
    // صورة داخل صورة: ما دام المشغّل مركّباً فمغادرة التطبيق تُصغّره لا توقفه.
    DisposableEffect(Unit) {
        PipBridge.wanted = true
        onDispose { PipBridge.wanted = false }
    }
    // عرضيّاً الفيديو وحده على الشاشة: أشرطة النظام تختفي وتعود بسحبة، وتعود كلّها عند الخروج.
    val view = LocalView.current
    DisposableEffect(landscape) {
        val window = (view.context as? Activity)?.window
        val ic = window?.let { WindowCompat.getInsetsController(it, view) }
        if (landscape) {
            ic?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ic?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ic?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { ic?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(controls, landscape) {
        if (controls && landscape) { delay(CONTROLS_HIDE_MS); controls = false }
    }

    Box(modifier = modifier.background(PhoneVideoBlack)) {
        Column(Modifier.fillMaxSize()) {
            // ── سطح الفيديو ──
            val view = LocalView.current
            Box(
                modifier = (if (landscape) Modifier.fillMaxSize() else if (pipActive) Modifier.fillMaxSize() else Modifier.fillMaxWidth().statusBarsPadding().aspectRatio(16f / 9f))
                    .background(PhoneVideoBlack)
                    /* لمسة = الأزرار؛ نقرتان على اليمين/اليسار = ±10 ث (للأفلام والحلقات). */
                    .pointerInput(landscape, isLive) {
                        detectTapGestures(
                            onTap = { controls = !controls },
                            onDoubleTap = { pos ->
                                if (isLive) return@detectTapGestures
                                val forward = pos.x > size.width / 2
                                engine.seekBy(if (forward) 10_000 else -10_000)
                                gestureHint = if (forward) "+10" else "−10"
                            },
                        )
                    }
                    /* سحب عموديّ: النصف الأيمن صوت، الأيسر سطوع؛ سحب أفقيّ: تقديم (للأفلام). */
                    .pointerInput(isLive) {
                        var axis = 0          // 0 مجهول، 1 عموديّ، 2 أفقيّ
                        var startX = 0f
                        var accum = Offset.Zero
                        detectDragGestures(
                            onDragStart = { pos -> axis = 0; startX = pos.x; accum = Offset.Zero },
                            onDragEnd = { axis = 0 },
                            onDrag = { change, drag ->
                                change.consume()
                                accum += drag
                                if (axis == 0 && (abs(accum.x) > 24f || abs(accum.y) > 24f)) axis = if (abs(accum.y) >= abs(accum.x)) 1 else 2
                                when (axis) {
                                    1 -> {
                                        val stepPx = size.height / 40f
                                        if (abs(accum.y) >= stepPx) {
                                            val steps = (accum.y / stepPx).toInt()
                                            accum = Offset(accum.x, accum.y - steps * stepPx)
                                            if (startX > size.width / 2) {
                                                engine.adjustVolumeByUser(-steps * 3)
                                                gestureHint = "🔊 " + engine.volume.value + "%"
                                            } else {
                                                val w = (view.context as? Activity)?.window
                                                if (w != null) {
                                                    val lp = w.attributes
                                                    val cur = if (lp.screenBrightness < 0f) 0.6f else lp.screenBrightness
                                                    lp.screenBrightness = (cur - steps * 0.03f).coerceIn(0.05f, 1f)
                                                    w.attributes = lp
                                                    gestureHint = "☀ " + (lp.screenBrightness * 100).toInt() + "%"
                                                }
                                            }
                                        }
                                    }
                                    2 -> if (!isLive) {
                                        val stepPx = size.width / 30f
                                        if (abs(accum.x) >= stepPx) {
                                            val steps = (accum.x / stepPx).toInt()
                                            accum = Offset(accum.x - steps * stepPx, accum.y)
                                            engine.seekBy(steps * 2_000L)
                                            gestureHint = formatClock(engine.position.value)
                                        }
                                    }
                                }
                            },
                        )
                    },
            ) {
                if (liveOnExo) {
                    ExoPreviewSurface(engine = liveVm.previewEngine, modifier = Modifier.fillMaxSize(), keepAwake = true)
                } else {
                    MpvVideoSurface(player = player, modifier = Modifier.fillMaxSize(), autoFrameRate = false)
                    SubtitleOverlay(player = player, modifier = Modifier.fillMaxSize())
                }
                if (!pipActive) PhonePlayerChrome(
                    engine = engine, title = title, isLive = isLive, visible = controls || !landscape,
                    onClose = onClose, onQuality = { showQuality = true },
                    onSubtitles = if (!isLive) { { showSubs = true } } else null,
                    onAudio = { showAudio = true },
                    modifier = Modifier.fillMaxSize(),
                )
                gestureHint?.let { hint ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(hint, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                if (outOfTime) {
                    OutOfTimeDialog(contact = liveVm.advertContact(), onDismiss = liveVm::dismissOutOfTime, contactMessage = contactMessage)
                }
                advert?.let { decision ->
                    AdvertOverlay(decision = decision, channelName = decision.channelName, onFinished = liveVm::onAdvertFinished)
                }
            }
            // ── ما تحت الفيديو (عموديّاً فقط) ──
            if (!landscape && !pipActive) {
                PhoneBelowVideo(playing = playing, liveVm = liveVm, onSwitchChannel = onSwitchChannel, modifier = Modifier.weight(1f))
            }
        }
    }

    if (showSubs) {
        var tracks by remember { mutableStateOf(engine.textTracks()) }
        LaunchedEffect(Unit) { repeat(10) { if (tracks.isNotEmpty()) return@LaunchedEffect; delay(300); tracks = engine.textTracks() } }
        PhoneSubtitleSheet(
            tracks = tracks,
            onSelect = { engine.selectSubtitle(it.mpvId); showSubs = false },
            onOff = { engine.disableSubtitles(); showSubs = false },
            onSearch = if (subtitleContext != null) { { showSubs = false; showSubSearch = true } } else null,
            onFile = if (subtitleContext != null) { { showSubs = false; showSubFile = true } } else null,
            onDismiss = { showSubs = false },
        )
    }
    if (showSubSearch) {
        SubtitleSearchScreen(onDismiss = { showSubSearch = false }, modifier = Modifier.fillMaxSize())
    }
    if (showSubFile) {
        StorageBrowser(
            title = stringResource(R.string.content_subtitle_select_file),
            mode = BrowseMode.FILE,
            fileExtensions = setOf("srt", "ass", "ssa", "vtt", "webvtt"),
            onPick = { file -> showSubFile = false; scope.launch { runCatching { subtitleController.applyLocal(file) } } },
            onDismiss = { showSubFile = false },
        )
    }
    if (showAudio) {
        var tracks by remember { mutableStateOf(engine.audioTracks()) }
        LaunchedEffect(Unit) { repeat(10) { if (tracks.isNotEmpty()) return@LaunchedEffect; delay(300); tracks = engine.audioTracks() } }
        TrackDialog(
            title = stringResource(R.string.player_tool_audio),
            tracks = tracks,
            onSelect = { engine.selectAudio(it.mpvId); showAudio = false },
            onOff = null,
            onDismiss = { showAudio = false },
        )
    }
    if (showQuality) {
        var tracks by remember { mutableStateOf(engine.videoTracks()) }
        LaunchedEffect(Unit) { repeat(10) { if (tracks.isNotEmpty()) return@LaunchedEffect; delay(300); tracks = engine.videoTracks() } }
        TrackDialog(
            title = stringResource(R.string.phone_player_quality),
            tracks = tracks.map { it.copy(label = it.label + "p") },
            onSelect = { engine.selectVideo(it.mpvId); showQuality = false },
            onOff = { engine.selectVideo(-1); showQuality = false },
            offLabel = stringResource(R.string.phone_player_auto),
            onDismiss = { showQuality = false },
        )
    }
}

/** الأزرار فوق الفيديو: رجوع وعنوان في الأعلى؛ تشغيل/إيقاف وشريط تقدّم (للأفلام) في الأسفل؛ الجودة. */
@Composable
private fun PhonePlayerChrome(
    engine: PlaybackEngine,
    title: String,
    isLive: Boolean,
    visible: Boolean,
    onClose: () -> Unit,
    onQuality: () -> Unit,
    onSubtitles: (() -> Unit)?,
    onAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buffering by engine.buffering.collectAsStateWithLifecycle()
    val audioCount by engine.audioCount.collectAsStateWithLifecycle()
    val error by engine.error.collectAsStateWithLifecycle()
    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()
    val qualityCount by engine.qualityCount.collectAsStateWithLifecycle()
    val position by engine.position.collectAsStateWithLifecycle()
    val duration by engine.duration.collectAsStateWithLifecycle()

    if (buffering && error == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.phone_player_loading), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (error != null) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            OwnTVButton(label = stringResource(R.string.phone_player_retry), icon = OwnTVIcon.REFRESH, onClick = engine::retry)
        }
    }
    if (!visible) return
    Box(modifier) {
        // شريط علويّ
        Row(
            modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoButton(OwnTVIcon.BACK, onClick = onClose)
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
            if (onSubtitles != null) VideoButton(OwnTVIcon.SUBTITLE, onClick = onSubtitles)
            if (audioCount > 1) VideoButton(OwnTVIcon.AUDIO, onClick = onAudio)
            if (qualityCount > 1) VideoButton(OwnTVIcon.VIDEO, onClick = onQuality)
        }
        // شريط سفليّ
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))).padding(8.dp),
        ) {
            if (!isLive && duration > 0) {
                PhoneSeekBar(position = position, duration = duration, onSeek = { engine.seekBy(it - position) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (!isLive) VideoButton(OwnTVIcon.SEEK_BACK) { engine.seekBy(-10_000) }
                VideoButton(if (isPlaying) OwnTVIcon.PAUSE else OwnTVIcon.PLAY, big = true, onClick = engine::togglePlayPause)
                if (!isLive) VideoButton(OwnTVIcon.SEEK_FORWARD) { engine.seekBy(10_000) }
            }
        }
    }
}

@Composable
private fun VideoButton(icon: OwnTVIcon, big: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(if (big) 64.dp else PhoneDimens.Touch).clip(RoundedCornerShape(999.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { OwnTVIcon(icon, tint = Color.White, modifier = Modifier.size(if (big) 36.dp else 24.dp), filled = big) }
}

/** شريط تقدّم بسيط باللمس: النقر على موضعٍ يقفز إليه. */
@Composable
private fun PhoneSeekBar(position: Long, duration: Long, onSeek: (Long) -> Unit) {
    val colors = OwnTVTheme.colors
    val frac = (position.toFloat() / duration).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp)
                .pointerInput(duration) { detectTapGestures { offset -> onSeek((offset.x / size.width * duration).toLong()) } },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f)))
            Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatClock(position), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text(formatClock(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** عموديّاً تحت الفيديو: العنوان، الآن/التالي، وقائمة قنوات القسم (للبثّ الحيّ). */
@Composable
private fun PhoneBelowVideo(playing: PhonePlaying, liveVm: LiveViewModel, onSwitchChannel: (ChannelEntity) -> Unit, modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {
        when (playing) {
            is PhonePlaying.Live -> {
                val epg by liveVm.nowNext.collectAsStateWithLifecycle()
                val zap by liveVm.zapChannels.collectAsStateWithLifecycle()
                val favorites by liveVm.favoriteIds.collectAsStateWithLifecycle()
                Column(Modifier.padding(PhoneDimens.Gutter)) {
                    Text(playing.channel.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    epg?.now?.let { Text(stringResource(R.string.phone_now) + " · " + it.title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    epg?.next?.let { Text(stringResource(R.string.phone_next) + " · " + it.title, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (zap.isNotEmpty()) {
                    PhoneSectionTitle(stringResource(R.string.phone_player_channels))
                    LazyColumn(contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(zap, key = { it.id }) { ch ->
                            PhoneChannelRow(
                                name = ch.name, logoUrl = ch.logoUrl, subtitle = null, favorite = ch.id in favorites,
                                onClick = { if (ch.id != playing.channel.id) onSwitchChannel(ch) },
                                onToggleFavorite = { liveVm.toggleFavorite(ch) },
                            )
                        }
                    }
                }
            }
            is PhonePlaying.Movie -> Column(Modifier.padding(PhoneDimens.Gutter)) {
                Text(playing.movie.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                playing.movie.plot?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)) }
            }
            is PhonePlaying.Episode -> Column(Modifier.padding(PhoneDimens.Gutter)) {
                Text(playing.series.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text(stringResource(R.string.phone_episode, playing.episode.episodeNumber, playing.episode.name), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** ورقة الترجمة: المسارات المدمجة، «تلقائيّ/إيقاف»، البحث في OpenSubtitles، وملفّ من الجهاز. */
@Composable
private fun PhoneSubtitleSheet(
    tracks: List<tv.own.owntv.player.TrackOption>,
    onSelect: (tv.own.owntv.player.TrackOption) -> Unit,
    onOff: () -> Unit,
    onSearch: (() -> Unit)?,
    onFile: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    tv.own.owntv.ui.components.OwnTVPopup(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.player_subtitles), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            PhoneSheetRow(stringResource(R.string.common_off), selected = tracks.none { it.selected }, onClick = onOff)
            tracks.forEach { t -> PhoneSheetRow(t.label, selected = t.selected, onClick = { onSelect(t) }) }
            if (onSearch != null || onFile != null) PhoneDivider(Modifier.padding(vertical = 4.dp))
            onSearch?.let { PhoneSheetRow(stringResource(R.string.player_search_subtitles), selected = false, onClick = it, icon = OwnTVIcon.SEARCH) }
            onFile?.let { PhoneSheetRow(stringResource(R.string.content_subtitle_select_file), selected = false, onClick = it, icon = OwnTVIcon.DOWNLOADS) }
        }
    }
}

@Composable
private fun PhoneSheetRow(label: String, selected: Boolean, onClick: () -> Unit, icon: OwnTVIcon? = null) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primaryContainer else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) OwnTVIcon(icon, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurface, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = colors.onPrimaryContainer)
    }
}
