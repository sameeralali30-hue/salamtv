package tv.own.owntv.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.features.home.ContinueKind
import tv.own.owntv.features.home.HomeViewModel
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.features.movies.MovieViewModel
import tv.own.owntv.features.series.SeriesViewModel
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * الرئيسيّة: «تابع المشاهدة» ثمّ القنوات الأخيرة ثمّ اختصارات الأقسام. لا بحث ولا معاينة
 * فيديو هنا — الهاتف يُفتح على عجل ليُشغَّل شيء، والفيديو الذي يعمل تحت الإبهام كلفة بطّاريّة
 * وبيانات لمن لم يطلبه.
 */
@Composable
fun PhoneHomeScreen(
    liveVm: LiveViewModel,
    movieVm: MovieViewModel,
    seriesVm: SeriesViewModel,
    onPlayChannel: (ChannelEntity, List<ChannelEntity>) -> Unit,
    onPlayMovie: (MovieEntity, Long) -> Unit,
    onOpenTab: (PhoneTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val homeVm: HomeViewModel = koinViewModel()
    val cont by homeVm.continueTarget.collectAsStateWithLifecycle()
    val recent by liveVm.recentlyWatched.collectAsStateWithLifecycle()
    val favorites by liveVm.favoriteIds.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PhoneHeader(stringResource(R.string.phone_tab_home))

        cont?.let { target ->
            PhoneSectionTitle(stringResource(R.string.phone_continue_watching))
            PhoneActionCard(
                icon = when (target.kind) {
                    ContinueKind.LIVE -> OwnTVIcon.LIVE_TV
                    ContinueKind.MOVIE -> OwnTVIcon.MOVIES
                    ContinueKind.EPISODE -> OwnTVIcon.SERIES
                },
                title = target.name,
                desc = null,
                accent = true,
                onClick = {
                    when (target.kind) {
                        ContinueKind.LIVE -> recent.firstOrNull { it.id == target.channelId }?.let { onPlayChannel(it, recent) }
                            ?: onOpenTab(PhoneTab.LIVE)
                        ContinueKind.MOVIE -> onOpenTab(PhoneTab.MOVIES)
                        ContinueKind.EPISODE -> onOpenTab(PhoneTab.SERIES)
                    }
                },
                modifier = Modifier.padding(horizontal = PhoneDimens.Gutter),
            )
        }

        if (recent.isNotEmpty()) {
            PhoneSectionTitle(stringResource(R.string.phone_recent_channels), modifier = Modifier.padding(top = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recent, key = { it.id }) { ch ->
                    Column(
                        modifier = Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable { onPlayChannel(ch, recent) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PhoneLogo(ch.logoUrl, ch.name, size = 64.dp)
                        Text(ch.name, style = MaterialTheme.typography.labelSmall, color = colors.onSurface, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        if (cont == null && recent.isEmpty()) {
            Text(
                stringResource(R.string.phone_home_empty), style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant, modifier = Modifier.padding(PhoneDimens.Gutter),
            )
        }

        PhoneSectionTitle(stringResource(R.string.phone_browse), modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneShortcut(OwnTVIcon.LIVE_TV, stringResource(R.string.phone_tab_live), Modifier.weight(1f)) { onOpenTab(PhoneTab.LIVE) }
            PhoneShortcut(OwnTVIcon.MOVIES, stringResource(R.string.phone_tab_movies), Modifier.weight(1f)) { onOpenTab(PhoneTab.MOVIES) }
            PhoneShortcut(OwnTVIcon.SERIES, stringResource(R.string.phone_tab_series), Modifier.weight(1f)) { onOpenTab(PhoneTab.SERIES) }
        }
        if (favorites.isNotEmpty()) {
            PhoneSectionTitle(stringResource(R.string.phone_favorite_channels), modifier = Modifier.padding(top = 12.dp))
            PhoneActionCard(
                icon = OwnTVIcon.FAVORITE, title = stringResource(R.string.phone_rail_favorites),
                desc = favorites.size.toString(),
                onClick = { liveVm.select(tv.own.owntv.core.live.LiveKey.Favorites); onOpenTab(PhoneTab.LIVE) },
                modifier = Modifier.padding(horizontal = PhoneDimens.Gutter),
            )
        }
    }
}

@Composable
private fun PhoneShortcut(icon: OwnTVIcon, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = OwnTVTheme.colors
    Column(
        modifier = modifier.clip(RoundedCornerShape(PhoneDimens.Corner)).background(colors.surfaceContainerLow)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OwnTVIcon(icon, tint = colors.primary, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
    }
}
