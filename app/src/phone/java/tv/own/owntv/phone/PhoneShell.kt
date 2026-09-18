package tv.own.owntv.phone

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.features.movies.MovieViewModel
import tv.own.owntv.features.series.SeriesViewModel
import tv.own.owntv.features.settings.SettingsViewModel
import tv.own.owntv.features.shell.ShellArgs
import tv.own.owntv.features.shell.ShellViewModel
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

/** تبويبات الشريط السفليّ — الخمسة التي يحتاجها مشترك، لا أقسام الهاوي السبعة. */
enum class PhoneTab(val labelRes: Int, val icon: OwnTVIcon) {
    HOME(R.string.phone_tab_home, OwnTVIcon.HOME),
    LIVE(R.string.phone_tab_live, OwnTVIcon.LIVE_TV),
    MOVIES(R.string.phone_tab_movies, OwnTVIcon.MOVIES),
    SERIES(R.string.phone_tab_series, OwnTVIcon.SERIES),
    ACCOUNT(R.string.phone_tab_account, OwnTVIcon.PERSON),
}

/** ما يعرضه المشغّل الآن — يقرّر أيّ محرّك يُرسم وأيّ عنوان يُكتب. */
sealed interface PhonePlaying {
    data class Live(val channel: ChannelEntity) : PhonePlaying
    data class Movie(val movie: MovieEntity) : PhonePlaying
    data class Episode(val series: SeriesEntity, val episode: EpisodeEntity) : PhonePlaying
}

/**
 * قشرة الهاتف: عموديّة، شريط سفليّ، شاشة واحدة في المرّة، ومشغّل يغطّي كلّ شيء حين يعمل.
 *
 * المحرّكات ونماذج العرض هي نفسها التي تُدير قشرة التلفاز ([LiveViewModel] وأخواتها) — هنا
 * **عرضٌ** جديد فقط: ما يُلمَس، وما يُقرأ من مسافة الذراع، وما يدور مع الجهاز. أيّ منطق تشغيل
 * (السلّم، الإعلانات، الرصيد، التذكرة) يبقى حيث هو ويصل الهاتفَ والتلفازَ معاً.
 */
@Composable
fun PhoneShell(args: ShellArgs, modifier: Modifier = Modifier) {
    val liveVm: LiveViewModel = koinViewModel()
    val movieVm: MovieViewModel = koinViewModel()
    val seriesVm: SeriesViewModel = koinViewModel()
    val shellVm: ShellViewModel = koinViewModel()
    val settingsVm: SettingsViewModel = koinViewModel()
    val player = koinInject<OwnTVPlayer>()
    val colors = OwnTVTheme.colors

    var tab by rememberSaveable { mutableStateOf(PhoneTab.LIVE) }
    var playing by remember { mutableStateOf<PhonePlaying?>(null) }
    val subscription by shellVm.subscription.collectAsStateWithLifecycle()

    // التصفّح عموديّ؛ المشغّل يتبع المستشعر (يدور إلى العرضيّ حين يقلب المشاهد هاتفه).
    val activity = LocalContext.current as? Activity
    DisposableEffect(playing != null) {
        activity?.requestedOrientation = if (playing != null) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose { }
    }

    val stopPlaying = {
        movieVm.saveProgressNow()
        seriesVm.saveEpisodeProgressNow()
        player.exitAudioOnly()
        runCatching { liveVm.previewEngine.exitAudioOnly() }
        liveVm.onFullscreenExited()
        player.stop()
        liveVm.clearLiveOnExo()
        playing = null
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    PhoneTab.HOME -> PhoneHomeScreen(
                        liveVm = liveVm, movieVm = movieVm, seriesVm = seriesVm,
                        onPlayChannel = { ch, list -> liveVm.watchFullscreen(ch, list); playing = PhonePlaying.Live(ch) },
                        onOpenTab = { tab = it },
                        onPlayMovie = { m, pos -> movieVm.play(m, pos); playing = PhonePlaying.Movie(m) },
                    )
                    PhoneTab.LIVE -> PhoneLiveScreen(
                        liveVm = liveVm,
                        onPlay = { ch, list -> liveVm.watchFullscreen(ch, list); playing = PhonePlaying.Live(ch) },
                    )
                    PhoneTab.MOVIES -> PhoneMoviesScreen(
                        movieVm = movieVm,
                        onPlay = { m, pos -> movieVm.play(m, pos); playing = PhonePlaying.Movie(m) },
                    )
                    PhoneTab.SERIES -> PhoneSeriesScreen(
                        seriesVm = seriesVm,
                        onPlay = { s, ep, pos -> seriesVm.playEpisode(ep, pos); playing = PhonePlaying.Episode(s, ep) },
                    )
                    PhoneTab.ACCOUNT -> PhoneAccountScreen(
                        settingsVm = settingsVm, status = subscription, isOffline = args.isOffline,
                    )
                }
            }
            PhoneBottomBar(selected = tab, onSelect = { tab = it })
        }

        playing?.let { now ->
            BackHandler(onBack = stopPlaying)
            PhonePlayerScreen(
                playing = now,
                liveVm = liveVm,
                player = player,
                contactMessage = subscription?.contactText.orEmpty(),
                onClose = stopPlaying,
                onSwitchChannel = { ch -> liveVm.watchFullscreen(ch, liveVm.zapChannels.value); playing = PhonePlaying.Live(ch) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PhoneBottomBar(selected: PhoneTab, onSelect: (PhoneTab) -> Unit) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer)
            .navigationBarsPadding()
            .height(PhoneDimens.BottomBar),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhoneTab.entries.forEach { t ->
            val on = t == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onSelect(t) }
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                OwnTVIcon(t.icon, tint = if (on) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(24.dp), filled = on)
                Text(
                    stringResource(t.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
