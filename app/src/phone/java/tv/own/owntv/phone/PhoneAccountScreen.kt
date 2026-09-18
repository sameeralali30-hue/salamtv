package tv.own.owntv.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.BuildConfig
import tv.own.owntv.R
import tv.own.owntv.features.salamtv.WhatsAppButton
import tv.own.owntv.features.settings.ConfirmDialog
import tv.own.owntv.features.settings.SettingsViewModel
import tv.own.owntv.features.setup.RedeemDialog
import tv.own.owntv.features.setup.SubscriberLoginClient
import tv.own.owntv.features.update.UpdateDialog
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * حسابي: بطاقة الاشتراك في الأعلى، ثمّ الأفعال الثلاثة التي يأتي المشترك من أجلها —
 * رمز التفعيل، واتساب، الخروج — وتحتها التحديث. الإعدادات التفصيليّة (المشغّل، المظهر، الدليل…)
 * خلف بطاقة واحدة تفتح شاشة الإعدادات الكاملة نفسها التي على التلفاز.
 */
@Composable
fun PhoneAccountScreen(
    settingsVm: SettingsViewModel,
    status: SubscriberLoginClient.Status?,
    isOffline: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    var showRedeem by remember { mutableStateOf(false) }
    var showPromo by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { settingsVm.refreshSubscription(force = false) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PhoneHeader(stringResource(R.string.phone_tab_account))

        // ── بطاقة الاشتراك ──
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter)
                .clip(RoundedCornerShape(PhoneDimens.Corner)).background(colors.surfaceContainer).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(status?.username ?: "…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            val stateLabel = when {
                status == null -> if (isOffline) stringResource(R.string.phone_offline) else stringResource(R.string.phone_player_loading)
                status.active -> stringResource(R.string.phone_account_state_active)
                status.state == "pending" -> stringResource(R.string.phone_account_state_pending)
                status.state == "disabled" -> stringResource(R.string.phone_account_state_disabled)
                else -> stringResource(R.string.phone_account_state_expired)
            }
            Text(stateLabel, style = MaterialTheme.typography.labelLarge, color = if (status?.active == true) colors.tertiary else colors.primary)
            if (status != null) {
                PhoneDivider(Modifier.padding(vertical = 6.dp))
                PhoneKv(stringResource(R.string.phone_account_plan), status.planName.ifBlank { "—" })
                PhoneKv(
                    stringResource(R.string.phone_account_days_left),
                    if (status.daysLeft < 0) stringResource(R.string.phone_account_unlimited) else status.daysLeft.toString(),
                )
            }
        }

        Column(Modifier.padding(PhoneDimens.Gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionCard(OwnTVIcon.SPARKLE, stringResource(R.string.phone_account_redeem), stringResource(R.string.phone_account_redeem_desc), accent = true,
                onClick = { showRedeem = true })
            PhoneActionCard(OwnTVIcon.STAR, stringResource(R.string.phone_account_promo), null, onClick = { showPromo = true })
            // كلّ أقسام الإعدادات (نفس شاشة التلفاز): للمشترك الذي يريد التحكّم بالمشغّل والمظهر والدليل.
            PhoneActionCard(OwnTVIcon.SETTINGS, stringResource(R.string.phone_account_settings), stringResource(R.string.phone_account_settings_desc), onClick = onOpenSettings)
            val number = status?.contact?.ifBlank { BuildConfig.SALAMTV_WHATSAPP } ?: BuildConfig.SALAMTV_WHATSAPP
            if (number.isNotBlank()) {
                WhatsAppButton(number = number, message = status?.contactText.orEmpty(), style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
            PhoneActionCard(OwnTVIcon.REFRESH, stringResource(R.string.phone_account_check_update),
                stringResource(R.string.phone_account_version, BuildConfig.VERSION_NAME), onClick = { showUpdate = true })
            PhoneActionCard(OwnTVIcon.POWER, stringResource(R.string.phone_account_sign_out), null, onClick = { showSignOut = true })
        }
    }

    if (showRedeem) {
        val ui by settingsVm.accountRedeem.collectAsStateWithLifecycle()
        RedeemDialog(
            knownUsername = status?.username, knownPassword = null, askForAccount = false,
            busy = ui.busy, message = ui.message, succeeded = ui.ok, offline = ui.offline,
            onSubmit = { _, _, code -> settingsVm.redeemFromAccount(code) },
            onDismiss = { settingsVm.clearAccountRedeem(); showRedeem = false },
        )
    }
    if (showPromo) {
        val ui by settingsVm.accountPromo.collectAsStateWithLifecycle()
        RedeemDialog(
            knownUsername = status?.username, knownPassword = null, askForAccount = false, promoMode = true,
            busy = ui.busy, message = ui.message, succeeded = ui.ok, offline = ui.offline,
            onSubmit = { _, _, code -> settingsVm.promoFromAccount(code) },
            onDismiss = { settingsVm.clearAccountPromo(); showPromo = false },
        )
    }
    if (showSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.phone_account_sign_out),
            message = stringResource(R.string.phone_account_sign_out_confirm),
            confirmLabel = stringResource(R.string.phone_account_sign_out),
            onConfirm = { settingsVm.signOutAccount(); showSignOut = false },
            onDismiss = { showSignOut = false },
        )
    }
    if (showUpdate) UpdateDialog(onDismiss = { showUpdate = false }, checkOnOpen = true)
}

@Composable
private fun PhoneKv(key: String, value: String) {
    val colors = OwnTVTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
    }
}
