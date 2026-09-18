package tv.own.owntv.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.features.live.LiveRailItem
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.ui.components.EmptyState
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.SearchBar

/** اسم الرفّ: المدمجة (المفضّلة/الأخيرة/الكلّ/الإعادة) بلا عنوان من النموذج — نسمّيها هنا. */
@Composable
fun railLabel(item: LiveRailItem): String = item.title ?: when (item.key) {
    LiveKey.Favorites -> stringResource(R.string.phone_rail_favorites)
    LiveKey.History -> stringResource(R.string.phone_rail_history)
    LiveKey.Catchup -> stringResource(R.string.phone_rail_catchup)
    LiveKey.All -> stringResource(R.string.phone_rail_all)
    is LiveKey.Folder, is LiveKey.Custom -> ""
}

/** القنوات: بحث، شرائح الأقسام، قائمة عموديّة؛ لمسة = تشغيل مع قائمة القسم كقائمة تنقّل. */
@Composable
fun PhoneLiveScreen(liveVm: LiveViewModel, onPlay: (ChannelEntity, List<ChannelEntity>) -> Unit, modifier: Modifier = Modifier) {
    val rails by liveVm.railItems.collectAsStateWithLifecycle()
    val selected by liveVm.selectedKey.collectAsStateWithLifecycle()
    val query by liveVm.searchQuery.collectAsStateWithLifecycle()
    val favorites by liveVm.favoriteIds.collectAsStateWithLifecycle()
    val channels: LazyPagingItems<ChannelEntity> = liveVm.channels.collectAsLazyPagingItems()
    val showNumbers by liveVm.showChannelNumbers.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        PhoneHeader(stringResource(R.string.phone_tab_live))
        SearchBar(
            query = query, onQueryChange = liveVm::setSearchQuery,
            placeholder = stringResource(R.string.phone_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter),
        )
        PhoneChips(
            items = rails, selected = { it.key == selected }, label = { railLabel(it) },
            onSelect = { liveVm.select(it.key) }, key = { it.key.toString() },
            modifier = Modifier.padding(vertical = PhoneDimens.RowGap),
        )
        if (channels.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(icon = OwnTVIcon.LIVE_TV, title = stringResource(R.string.phone_empty_channels), message = "")
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(channels.itemCount, key = { i -> channels.peek(i)?.id ?: i }) { i ->
                val ch = channels[i] ?: return@items
                PhoneChannelRow(
                    name = ch.name, logoUrl = ch.logoUrl, subtitle = null,
                    favorite = ch.id in favorites,
                    number = if (showNumbers) ch.number else null,
                    onClick = { onPlay(ch, channels.itemSnapshotList.items) },
                    onToggleFavorite = { liveVm.toggleFavorite(ch) },
                )
            }
        }
    }
}
