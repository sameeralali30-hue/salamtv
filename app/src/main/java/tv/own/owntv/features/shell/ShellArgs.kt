package tv.own.owntv.features.shell

import androidx.compose.runtime.Immutable
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.core.theme.FontCustomization
import tv.own.owntv.core.theme.ThemeMode
import tv.own.owntv.core.weather.WeatherInfo

/**
 * [SALAMTV] كلّ ما تحتاجه القشرة من النشاط — واحدٌ للنكهتين.
 *
 * `FormShell` (src/tv و src/phone) هي التي تقرّر أيّ قشرة تُركَّب: التلفاز يمرّر هذا كما هو إلى
 * [OwnTVShell]، والهاتف إلى قشرته الخاصّة. النشاط لا يعرف أيّهما — فلا `if` على نوع الجهاز في
 * مكانين. حقولٌ لا تستعملها قشرة الهاتف اليوم (الطقس، القوائم المتعدّدة) تبقى هنا: كلفتها
 * صفر، وحذفها من التوقيع يعني تعديل النشاط عند كلّ ميزة تُضاف للهاتف.
 */
@Immutable
data class ShellArgs(
    val selectedSection: MainSection,
    val visibleSections: Set<MainSection>,
    val onSelectSection: (MainSection) -> Unit,
    val themeMode: ThemeMode,
    val uiZoomPercent: Int,
    val onSetZoom: (Int) -> Unit,
    val fontCustomization: FontCustomization,
    val onSetFontCustomization: (FontCustomization) -> Unit,
    val avatarId: Int,
    val onSetAvatar: (Int) -> Unit,
    val profileName: String,
    val sourceSummary: String?,
    val playlists: List<SourceEntity>,
    val activePlaylistId: Long,
    val onSelectPlaylist: (Long) -> Unit,
    val weatherInfo: WeatherInfo?,
    val weatherFahrenheit: Boolean,
    val activeProfileId: Long?,
    val pendingDeepLink: tv.own.owntv.core.launcher.LauncherDeepLink?,
    val onDeepLinkConsumed: () -> Unit,
    val isOffline: Boolean,
    val onExitApp: () -> Unit,
    val onSwitchProfile: () -> Unit,
)
