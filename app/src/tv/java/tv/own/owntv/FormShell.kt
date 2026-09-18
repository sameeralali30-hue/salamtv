package tv.own.owntv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.own.owntv.features.shell.OwnTVShell
import tv.own.owntv.features.shell.ShellArgs

/** نكهة التلفاز: القشرة الأصليّة (شريط جانبيّ + ثلاثة أعمدة + ريموت) كما هي. */
@Composable
fun FormShell(args: ShellArgs, modifier: Modifier = Modifier) {
    OwnTVShell(
        selectedSection = args.selectedSection,
        visibleSections = args.visibleSections,
        onSelectSection = args.onSelectSection,
        themeMode = args.themeMode,
        uiZoomPercent = args.uiZoomPercent,
        onSetZoom = args.onSetZoom,
        fontCustomization = args.fontCustomization,
        onSetFontCustomization = args.onSetFontCustomization,
        avatarId = args.avatarId,
        onSetAvatar = args.onSetAvatar,
        profileName = args.profileName,
        sourceSummary = args.sourceSummary,
        playlists = args.playlists,
        activePlaylistId = args.activePlaylistId,
        onSelectPlaylist = args.onSelectPlaylist,
        weatherInfo = args.weatherInfo,
        weatherFahrenheit = args.weatherFahrenheit,
        activeProfileId = args.activeProfileId,
        pendingDeepLink = args.pendingDeepLink,
        onDeepLinkConsumed = args.onDeepLinkConsumed,
        isOffline = args.isOffline,
        onExitApp = args.onExitApp,
        onSwitchProfile = args.onSwitchProfile,
        modifier = modifier,
    )
}
