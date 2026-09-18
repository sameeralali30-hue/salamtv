package tv.own.owntv.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.features.movies.MovieViewModel
import tv.own.owntv.features.series.SeriesViewModel
import tv.own.owntv.ui.components.EmptyState
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.PosterCard
import tv.own.owntv.ui.components.SearchBar
import tv.own.owntv.ui.theme.OwnTVTheme

/** الأفلام: شبكة أغلفة بثلاثة أعمدة؛ لمسة = صفحة الفيلم (غلاف كبير، قصّة، تشغيل/متابعة). */
@Composable
fun PhoneMoviesScreen(movieVm: MovieViewModel, onPlay: (MovieEntity, Long) -> Unit, modifier: Modifier = Modifier) {
    val rails by movieVm.railItems.collectAsStateWithLifecycle()
    val selected by movieVm.selectedKey.collectAsStateWithLifecycle()
    val query by movieVm.searchQuery.collectAsStateWithLifecycle()
    val favorites by movieVm.favoriteIds.collectAsStateWithLifecycle()
    val progress by movieVm.movieProgress.collectAsStateWithLifecycle()
    var opened by remember { mutableStateOf<MovieEntity?>(null) }
    val movies = movieVm.movies.collectAsLazyPagingItems()

    opened?.let { movie ->
        BackHandler { opened = null }
        val p = progress[movie.id]
        PhoneVodDetail(
            title = movie.name, poster = movie.posterUrl, backdrop = movie.backdropUrl, year = movie.year,
            rating = movie.rating, plot = movie.plot, favorite = movie.id in favorites,
            resumeMs = p?.positionMs?.takeIf { it > 0 && !movieVm.isMovieCompleted(p) },
            onPlay = { pos -> onPlay(movie, pos) },
            onToggleFavorite = { movieVm.toggleFavorite(movie) },
            onBack = { opened = null },
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        PhoneHeader(stringResource(R.string.phone_tab_movies))
        SearchBar(
            query = query, onQueryChange = movieVm::setSearchQuery,
            placeholder = stringResource(R.string.phone_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter),
        )
        PhoneChips(
            items = rails, selected = { it.key == selected }, label = { railLabel(it) },
            onSelect = { movieVm.select(it.key) }, key = { it.key.toString() },
            modifier = Modifier.padding(vertical = PhoneDimens.RowGap),
        )
        if (movies.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(icon = OwnTVIcon.MOVIES, title = stringResource(R.string.phone_empty_movies), message = "")
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(movies.itemCount, key = { i -> movies.peek(i)?.id ?: i }) { i ->
                val m = movies[i] ?: return@items
                val p = progress[m.id]
                PosterCard(
                    posterUrl = m.posterUrl, title = m.name, rating = m.rating,
                    progressFraction = p?.let { if (it.durationMs > 0) (it.positionMs.toFloat() / it.durationMs).coerceIn(0f, 1f) else null },
                    completed = p != null && movieVm.isMovieCompleted(p),
                    isFavorite = m.id in favorites,
                    onClick = { movieVm.onMovieFocused(m); opened = m },
                )
            }
        }
    }
}

/** المسلسلات: شبكة أغلفة؛ لمسة = المواسم والحلقات؛ لمسة الحلقة = تشغيل. */
@Composable
fun PhoneSeriesScreen(seriesVm: SeriesViewModel, onPlay: (SeriesEntity, EpisodeEntity, Long) -> Unit, modifier: Modifier = Modifier) {
    val rails by seriesVm.railItems.collectAsStateWithLifecycle()
    val selected by seriesVm.selectedKey.collectAsStateWithLifecycle()
    val query by seriesVm.searchQuery.collectAsStateWithLifecycle()
    val favorites by seriesVm.favoriteIds.collectAsStateWithLifecycle()
    val opened by seriesVm.openedSeries.collectAsStateWithLifecycle()
    val series = seriesVm.series.collectAsLazyPagingItems()

    opened?.let { show ->
        BackHandler { seriesVm.closeSeries() }
        PhoneSeriesDetail(seriesVm = seriesVm, show = show, favorite = show.id in favorites, onPlay = { ep, pos -> onPlay(show, ep, pos) }, modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        PhoneHeader(stringResource(R.string.phone_tab_series))
        SearchBar(
            query = query, onQueryChange = seriesVm::setSearchQuery,
            placeholder = stringResource(R.string.phone_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter),
        )
        PhoneChips(
            items = rails, selected = { it.key == selected }, label = { railLabel(it) },
            onSelect = { seriesVm.select(it.key) }, key = { it.key.toString() },
            modifier = Modifier.padding(vertical = PhoneDimens.RowGap),
        )
        if (series.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(icon = OwnTVIcon.SERIES, title = stringResource(R.string.phone_empty_series), message = "")
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(series.itemCount, key = { i -> series.peek(i)?.id ?: i }) { i ->
                val s = series[i] ?: return@items
                PosterCard(
                    posterUrl = s.posterUrl, title = s.name, rating = s.rating, isFavorite = s.id in favorites,
                    onClick = { seriesVm.openSeries(s) },
                )
            }
        }
    }
}

@Composable
private fun PhoneSeriesDetail(
    seriesVm: SeriesViewModel,
    show: SeriesEntity,
    favorite: Boolean,
    onPlay: (EpisodeEntity, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val episodes by seriesVm.episodes.collectAsStateWithLifecycle()
    val season by seriesVm.selectedSeason.collectAsStateWithLifecycle()
    val progress by seriesVm.episodeProgress.collectAsStateWithLifecycle()
    val loading by seriesVm.episodesLoading.collectAsStateWithLifecycle()
    val seasons = episodes.map { it.seasonNumber }.distinct().sorted()
    val visible = episodes.filter { it.seasonNumber == season }.sortedBy { it.episodeNumber }

    Column(modifier = modifier.fillMaxSize()) {
        PhoneDetailHeader(title = show.name, poster = show.posterUrl, backdrop = show.backdropUrl, year = show.year, rating = show.rating,
            plot = show.plot, favorite = favorite, onToggleFavorite = { seriesVm.toggleFavorite(show) }, onBack = { seriesVm.closeSeries() })
        if (seasons.size > 1) {
            PhoneChips(
                items = seasons, selected = { it == season }, label = { stringResource(R.string.phone_season, it) },
                onSelect = seriesVm::selectSeason, key = { it }, modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        if (loading && episodes.isEmpty()) {
            Text(stringResource(R.string.phone_player_loading), color = colors.onSurfaceVariant, modifier = Modifier.padding(PhoneDimens.Gutter))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visible, key = { it.id }) { ep ->
                val p = progress[ep.id]
                val resume = p?.positionMs?.takeIf { it > 0 && p.durationMs > 0 && it < p.durationMs * 0.95 } ?: 0L
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(PhoneDimens.Corner))
                        .background(colors.surfaceContainerLow).clickable { onPlay(ep, resume) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OwnTVIcon(OwnTVIcon.PLAY, tint = colors.primary, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.phone_episode, ep.episodeNumber, ep.name), style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (resume > 0) Text(stringResource(R.string.phone_resume), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    }
                }
            }
        }
    }
}

/** صفحة فيلم: الرأس نفسه + زرّ تشغيل/متابعة. */
@Composable
private fun PhoneVodDetail(
    title: String, poster: String?, backdrop: String?, year: Int?, rating: Double?, plot: String?,
    favorite: Boolean, resumeMs: Long?, onPlay: (Long) -> Unit, onToggleFavorite: () -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PhoneDetailHeader(title, poster, backdrop, year, rating, plot, favorite, onToggleFavorite, onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (resumeMs != null) {
                OwnTVButton(label = stringResource(R.string.phone_resume), icon = OwnTVIcon.PLAY, onClick = { onPlay(resumeMs) }, modifier = Modifier.weight(1f))
                OwnTVButton(label = stringResource(R.string.phone_play), style = OwnTVButtonStyle.SECONDARY, onClick = { onPlay(0) }, modifier = Modifier.weight(1f))
            } else {
                OwnTVButton(label = stringResource(R.string.phone_play), icon = OwnTVIcon.PLAY, onClick = { onPlay(0) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** رأس صفحة فيلم/مسلسل: خلفيّة عريضة، غلاف، عنوان، سنة/تقييم، قصّة، قلب، رجوع. */
@Composable
private fun PhoneDetailHeader(
    title: String, poster: String?, backdrop: String?, year: Int?, rating: Double?, plot: String?,
    favorite: Boolean, onToggleFavorite: () -> Unit, onBack: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceContainer)) {
        val art = backdrop ?: poster
        if (!art.isNullOrBlank()) AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PhoneRoundButton(OwnTVIcon.BACK, onBack)
            PhoneRoundButton(OwnTVIcon.FAVORITE, onToggleFavorite, tint = if (favorite) colors.favorite else null, filled = favorite)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!poster.isNullOrBlank()) {
            AsyncImage(model = poster, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.width(84.dp).height(120.dp).clip(RoundedCornerShape(10.dp)))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(year?.toString(), rating?.takeIf { it > 0 }?.let { "★ %.1f".format(it) }).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (!plot.isNullOrBlank()) {
                Text(plot, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
fun PhoneRoundButton(icon: OwnTVIcon, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color? = null, filled: Boolean = false) {
    val colors = OwnTVTheme.colors
    Box(
        modifier = Modifier.size(PhoneDimens.Touch).clip(RoundedCornerShape(999.dp))
            .background(colors.surface.copy(alpha = 0.55f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { OwnTVIcon(icon, tint = tint ?: colors.onSurface, modifier = Modifier.size(22.dp), filled = filled) }
}
