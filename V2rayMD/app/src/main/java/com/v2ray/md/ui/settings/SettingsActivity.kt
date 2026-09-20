@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.v2ray.md.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import com.v2ray.md.ui.perappproxy.PerAppProxyActivity
import com.v2ray.md.ui.userasset.UserAssetActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.v2ray.md.AppConfig
import com.v2ray.md.AppConfig.VPN
import com.v2ray.md.R
import com.v2ray.md.extension.toastError
import com.v2ray.md.handler.AppLocaleManager
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.handler.MmkvManager.rememberMmkvBool
import com.v2ray.md.handler.MmkvManager.rememberMmkvString
import com.v2ray.md.handler.SettingsChangeManager
import com.v2ray.md.root.RootManager
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.NavigationBarsSpacer
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SettingsEditItem
import com.v2ray.md.ui.compose.SettingsListItem
import com.v2ray.md.ui.compose.SettingsMenuItem
import com.v2ray.md.ui.compose.SettingsSwitchItem
import com.v2ray.md.ui.compose.SingleSelectButtonGroup
import com.v2ray.md.ui.compose.ThemeManager
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.LogUtil
import com.v2ray.md.util.Utils
import kotlinx.coroutines.launch

class SettingsActivity : BaseComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshSystemVpnSettingsAvailability()
            }
        }
    }

    private fun openSystemVpnSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            reportSystemVpnSettingsFailure(error)
        } catch (error: SecurityException) {
            reportSystemVpnSettingsFailure(error)
        }
    }

    private fun reportSystemVpnSettingsFailure(error: RuntimeException) {
        LogUtil.e(AppConfig.TAG, "Cannot open system VPN settings", error)
        toastError(R.string.toast_system_vpn_settings_unavailable)
    }

    @Composable
    override fun ScreenContent() {
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onModeHelpClicked = { Utils.openUri(this, AppConfig.APP_WIKI_MODE) },
            onSystemVpnSettingsClicked = ::openSystemVpnSettings
        )
    }
}

private data class SettingsEntry(
    val titleRes: Int,
    val summaryRes: Int?,
    val content: @Composable (Shape) -> Unit
)

@Composable
private fun SettingsEntry.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (stringResource(titleRes).contains(q, ignoreCase = true)) return true
    val s = summaryRes?.let { stringResource(it) } ?: return false
    return s.contains(q, ignoreCase = true)
}

private data class SettingsSection(
    val titleRes: Int,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val entries: List<SettingsEntry>
)

@Composable
private fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(48.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.menu_item_search),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        MorphIconButton( onClick = onClear) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = stringResource(R.string.logcat_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onModeHelpClicked: () -> Unit,
    onSystemVpnSettingsClicked: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val systemVpnSettingsAvailable by viewModel.systemVpnSettingsAvailable.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var openSectionRes by rememberSaveable { mutableStateOf<Int?>(null) }

    var localDns by rememberMmkvBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
    var fakeDns by rememberMmkvBool(AppConfig.PREF_FAKE_DNS_ENABLED, false)
    var appendHttpProxy by rememberMmkvBool(AppConfig.PREF_APPEND_HTTP_PROXY, false)
    var vpnDns by rememberMmkvString(AppConfig.PREF_VPN_DNS, "")
    var vpnBypassLan by rememberMmkvString(AppConfig.PREF_VPN_BYPASS_LAN, AppConfig.DEFAULT_VPN_BYPASS_LAN)
    var vpnInterfaceAddress by rememberMmkvString(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
    var vpnMtu by rememberMmkvString(AppConfig.PREF_VPN_MTU, "")

    var mux by rememberMmkvBool(AppConfig.PREF_MUX_ENABLED, false)
    var muxConcurrency by rememberMmkvString(AppConfig.PREF_MUX_CONCURRENCY, "8")
    var muxXudpConcurrency by rememberMmkvString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY)
    var muxXudpQuic by rememberMmkvString(AppConfig.PREF_MUX_XUDP_QUIC, "reject")

    var fragment by rememberMmkvBool(AppConfig.PREF_FRAGMENT_ENABLED, false)
    var fragmentPackets by rememberMmkvString(AppConfig.PREF_FRAGMENT_PACKETS, "tlshello")
    var fragmentLength by rememberMmkvString(AppConfig.PREF_FRAGMENT_LENGTH, "50-100")
    var fragmentInterval by rememberMmkvString(AppConfig.PREF_FRAGMENT_INTERVAL, "10-20")
    var fragmentMaxSplit by rememberMmkvString(AppConfig.PREF_FRAGMENT_MAXSPLIT, "10")
    var observatoryLeastPingInterval by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL, AppConfig.OBSERVATORY_LEAST_PING_INTERVAL)
    var observatoryLeastLoadInterval by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL, AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL)
    var observatoryLeastLoadMethod by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD, AppConfig.OBSERVATORY_LEAST_LOAD_METHOD)
    var observatoryLeastLoadSampling by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING, AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING)
    var observatoryLeastLoadTimeout by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT, AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT)

    var mode by rememberMmkvString(AppConfig.PREF_MODE, VPN)
    var enableRootMode by rememberMmkvBool(AppConfig.PREF_ROOT_MODE_ENABLE, false)
    var lanSharing by rememberMmkvBool(AppConfig.PREF_ROOT_LAN_SHARING, false)

    var hevTunLogLevel by rememberMmkvString(AppConfig.PREF_HEV_TUNNEL_LOGLEVEL, AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL)
    var hevTunRwTimeout by rememberMmkvString(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, "")
    var useHevTun by rememberMmkvBool(AppConfig.PREF_USE_HEV_TUNNEL, true)

    var enableLocalProxy by rememberMmkvBool(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
    var socksPort by rememberMmkvString(AppConfig.PREF_SOCKS_PORT, "")
    var dynamicSocksPort by rememberMmkvBool(AppConfig.PREF_DYNAMIC_SOCKS_PORT, false)
    var socksUsername by rememberMmkvString(AppConfig.PREF_SOCKS_USERNAME, "")
    var socksPassword by rememberMmkvString(AppConfig.PREF_SOCKS_PASSWORD, "")
    var socksEnableUdp by rememberMmkvBool(AppConfig.PREF_SOCKS_ENABLE_UDP, AppConfig.DEFAULT_SOCKS_ENABLE_UDP)
    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)

    var speedEnabled by rememberMmkvBool(AppConfig.PREF_SPEED_ENABLED, false)
    var confirmRemove by rememberMmkvBool(AppConfig.PREF_CONFIRM_REMOVE, false)
    var doubleColumnDisplay by rememberMmkvBool(AppConfig.PREF_DOUBLE_COLUMN_DISPLAY, false)
    var groupAllDisplay by rememberMmkvBool(AppConfig.PREF_GROUP_ALL_DISPLAY, false)
    var language by remember {
        mutableStateOf(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE, "auto") ?: "auto"
        )
    }
    var uiModeNight by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "0")
    var dynamicColor by rememberMmkvBool(AppConfig.PREF_DYNAMIC_COLOR, true)

    var ipv6Enabled by rememberMmkvBool(AppConfig.PREF_IPV6_ENABLED, false)
    var preferIpv6 by rememberMmkvBool(AppConfig.PREF_PREFER_IPV6, false)
    var sniffingEnabled by rememberMmkvBool(AppConfig.PREF_SNIFFING_ENABLED, true)
    var routeOnlyEnabled by rememberMmkvBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
    var remoteDns by rememberMmkvString(AppConfig.PREF_REMOTE_DNS, "")
    var domesticDns by rememberMmkvString(AppConfig.PREF_DOMESTIC_DNS, "")
    var dnsHosts by rememberMmkvString(AppConfig.PREF_DNS_HOSTS, "")
    var coreLogLevel by rememberMmkvString(AppConfig.PREF_LOGLEVEL, "warning")
    var outboundResolveMethod by rememberMmkvString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, AppConfig.DEFAULT_OUTBOUND_DOMAIN_RESOLVE_METHOD)

    var isBooted by rememberMmkvBool(AppConfig.PREF_IS_BOOTED, false)
    var delayTestUrl by rememberMmkvString(AppConfig.PREF_DELAY_TEST_URL, "")
    var realPingConcurrency by rememberMmkvString(AppConfig.PREF_REAL_PING_CONCURRENCY, "16")
    var ipApiUrl by rememberMmkvString(AppConfig.PREF_IP_API_URL, "")

    val isVpn = mode == VPN
    val hevTunEnabled = isVpn && useHevTun
    val localProxyForced = hevTunEnabled
    val effectiveLocalProxy = enableLocalProxy || localProxyForced
    val muxXudpConcurrencyInt = muxXudpConcurrency.toIntOrNull() ?: AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt()

    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val uiModeNightEntries = stringArrayResource(R.array.ui_mode_night).toList()
    val uiModeNightValues = stringArrayResource(R.array.ui_mode_night_value).toList()
    val bypassLanEntries = stringArrayResource(R.array.vpn_bypass_lan).toList()
    val bypassLanValues = stringArrayResource(R.array.vpn_bypass_lan_value).toList()
    val interfaceAddrEntries = stringArrayResource(R.array.vpn_interface_address).toList()
    val interfaceAddrValues = stringArrayResource(R.array.vpn_interface_address_value).toList()
    val hevLogEntries = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val hevLogValues = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val coreLogLevelEntries = stringArrayResource(R.array.core_loglevel).toList()
    val coreLogLevelValues = stringArrayResource(R.array.core_loglevel).toList()
    val outboundResolveEntries = stringArrayResource(R.array.outbound_domain_resolve_method).toList()
    val outboundResolveValues = stringArrayResource(R.array.outbound_domain_resolve_method_value).toList()
    val xudpQuicEntries = stringArrayResource(R.array.mux_xudp_quic_entries).toList()
    val xudpQuicValues = stringArrayResource(R.array.mux_xudp_quic_value).toList()
    val fragmentPacketsEntries = stringArrayResource(R.array.fragment_packets).toList()
    val fragmentPacketsValues = stringArrayResource(R.array.fragment_packets).toList()
    val observatoryLeastLoadMethodEntries = stringArrayResource(R.array.observatory_least_load_method).toList()
    val observatoryLeastLoadMethodValues = stringArrayResource(R.array.observatory_least_load_method).toList()
    val modeEntries = stringArrayResource(R.array.mode_entries).toList()
    val modeValues = stringArrayResource(R.array.mode_value).toList()

    val sections = listOf(
        SettingsSection(
            R.string.title_ui_settings,
            iconVector = Icons.Filled.Palette, entries = listOf(
                SettingsEntry(R.string.title_pref_speed_enabled, R.string.summary_pref_speed_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_play_24dp),
                            title = stringResource(R.string.title_pref_speed_enabled),
                            summary = stringResource(R.string.summary_pref_speed_enabled),
                            checked = speedEnabled,
                            onCheckedChange = { speedEnabled = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_confirm_remove, R.string.summary_pref_confirm_remove) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_delete_24dp),
                            title = stringResource(R.string.title_pref_confirm_remove),
                            summary = stringResource(R.string.summary_pref_confirm_remove),
                            checked = confirmRemove,
                            onCheckedChange = { confirmRemove = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_double_column_display, R.string.summary_pref_double_column_display) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_select_all_24dp),
                            title = stringResource(R.string.title_pref_double_column_display),
                            summary = stringResource(R.string.summary_pref_double_column_display),
                            checked = doubleColumnDisplay,
                            onCheckedChange = {
                                doubleColumnDisplay = it
                                SettingsChangeManager.makeSetupGroupTab()
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_group_all_display, R.string.summary_pref_group_all_display) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_subscriptions_24dp),
                            title = stringResource(R.string.title_pref_group_all_display),
                            summary = stringResource(R.string.summary_pref_group_all_display),
                            checked = groupAllDisplay,
                            onCheckedChange = {
                                groupAllDisplay = it
                                SettingsChangeManager.makeSetupGroupTab()
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_dynamic_color, R.string.summary_pref_dynamic_color) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_image_24dp),
                            title = stringResource(R.string.title_pref_dynamic_color),
                            summary = stringResource(R.string.summary_pref_dynamic_color),
                            checked = dynamicColor,
                            onCheckedChange = {
                                dynamicColor = it
                                ThemeManager.setDynamicColorEnabled(it)
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_language, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_translate_24dp),
                            title = stringResource(R.string.title_language),
                            entries = languageEntries,
                            values = languageValues,
                            selectedValue = language,
                            onSelected = {
                                language = it
                                AppLocaleManager.setApplicationLanguage(it)
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_ui_mode_night, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_flash_off_24dp),
                            title = stringResource(R.string.title_pref_ui_mode_night),
                            entries = uiModeNightEntries,
                            values = uiModeNightValues,
                            selectedValue = uiModeNight,
                            onSelected = {
                                uiModeNight = it
                                ThemeManager.setThemeMode(it)
                            },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_vpn_settings,
            iconVector = Icons.Filled.VpnKey, entries = listOf(
                SettingsEntry(R.string.per_app_proxy_settings, null) { shape ->
                    val context = LocalContext.current
                    SettingsMenuItem(icon = painterResource(R.drawable.ic_per_apps_24dp), title = stringResource(R.string.per_app_proxy_settings), onClick = { context.startActivity(Intent(context, PerAppProxyActivity::class.java)) }, shape = shape)
                },
                SettingsEntry(R.string.title_pref_ipv6_enabled, R.string.summary_pref_ipv6_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_routing_24dp),
                            title = stringResource(R.string.title_pref_ipv6_enabled),
                            summary = stringResource(R.string.summary_pref_ipv6_enabled),
                            checked = ipv6Enabled,
                            onCheckedChange = { ipv6Enabled = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_prefer_ipv6, R.string.summary_pref_prefer_ipv6) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_select_all_24dp),
                            title = stringResource(R.string.title_pref_prefer_ipv6),
                            summary = stringResource(R.string.summary_pref_prefer_ipv6),
                            checked = preferIpv6,
                            onCheckedChange = { preferIpv6 = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_local_dns_enabled, R.string.summary_pref_local_dns_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_lock_24dp),
                            title = stringResource(R.string.title_pref_local_dns_enabled),
                            summary = stringResource(R.string.summary_pref_local_dns_enabled),
                            checked = localDns,
                            enabled = isVpn,
                            onCheckedChange = { localDns = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_fake_dns_enabled, R.string.summary_pref_fake_dns_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_privacy_24dp),
                            title = stringResource(R.string.title_pref_fake_dns_enabled),
                            summary = stringResource(R.string.summary_pref_fake_dns_enabled),
                            checked = fakeDns,
                            enabled = isVpn && localDns,
                            onCheckedChange = { fakeDns = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_vpn_dns, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_vpn_dns),
                            value = vpnDns,
                            enabled = isVpn && !localDns,
                            onValueChanged = { vpnDns = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_append_http_proxy, R.string.summary_pref_append_http_proxy) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_share_24dp),
                            title = stringResource(R.string.title_pref_append_http_proxy),
                            summary = stringResource(R.string.summary_pref_append_http_proxy),
                            checked = appendHttpProxy,
                            enabled = effectiveLocalProxy,
                            onCheckedChange = { appendHttpProxy = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_vpn_bypass_lan, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_outline_filter_alt_24),
                            title = stringResource(R.string.title_pref_vpn_bypass_lan),
                            entries = bypassLanEntries,
                            values = bypassLanValues,
                            selectedValue = vpnBypassLan,
                            enabled = isVpn,
                            onSelected = { vpnBypassLan = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_vpn_interface_address, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_settings_24dp),
                            title = stringResource(R.string.title_pref_vpn_interface_address),
                            entries = interfaceAddrEntries,
                            values = interfaceAddrValues,
                            selectedValue = vpnInterfaceAddress,
                            enabled = isVpn,
                            onSelected = { vpnInterfaceAddress = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_vpn_mtu, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_vpn_mtu),
                            value = vpnMtu,
                            enabled = isVpn,
                            keyboardNumber = true,
                            onValueChanged = { vpnMtu = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_use_hev_tunnel, R.string.summary_pref_use_hev_tunnel) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_play_24dp),
                            title = stringResource(R.string.title_pref_use_hev_tunnel),
                            summary = stringResource(R.string.summary_pref_use_hev_tunnel),
                            checked = useHevTun,
                            enabled = isVpn,
                            onCheckedChange = {
                                useHevTun = it
                                if (it && !enableLocalProxy) {
                                    enableLocalProxy = true
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_hev_tunnel_loglevel, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_logcat_24dp),
                            title = stringResource(R.string.title_pref_hev_tunnel_loglevel),
                            entries = hevLogEntries,
                            values = hevLogValues,
                            selectedValue = hevTunLogLevel,
                            enabled = hevTunEnabled,
                            onSelected = { hevTunLogLevel = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_hev_tunnel_rw_timeout, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_restore_24dp),
                            title = stringResource(R.string.title_pref_hev_tunnel_rw_timeout),
                            value = hevTunRwTimeout,
                            enabled = hevTunEnabled,
                            keyboardNumber = true,
                            onValueChanged = { hevTunRwTimeout = it },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_core_settings,
            iconVector = Icons.Filled.Settings, entries = listOf(
                SettingsEntry(R.string.title_user_asset_setting, null) { shape ->
                    val context = LocalContext.current
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_file_24dp),
                        title = stringResource(R.string.title_user_asset_setting),
                        onClick = { context.startActivity(Intent(context, UserAssetActivity::class.java)) },
                        shape = shape
                    )
                },
                SettingsEntry(R.string.title_pref_sniffing_enabled, R.string.summary_pref_sniffing_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_search_24dp),
                            title = stringResource(R.string.title_pref_sniffing_enabled),
                            summary = stringResource(R.string.summary_pref_sniffing_enabled),
                            checked = sniffingEnabled,
                            onCheckedChange = { sniffingEnabled = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_route_only_enabled, R.string.summary_pref_route_only_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_routing_24dp),
                            title = stringResource(R.string.title_pref_route_only_enabled),
                            summary = stringResource(R.string.summary_pref_route_only_enabled),
                            checked = routeOnlyEnabled,
                            onCheckedChange = { routeOnlyEnabled = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_enable_local_proxy, R.string.summary_pref_enable_local_proxy) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_share_24dp),
                            title = stringResource(R.string.title_pref_enable_local_proxy),
                            summary = stringResource(R.string.summary_pref_enable_local_proxy),
                            checked = enableLocalProxy,
                            enabled = !localProxyForced,
                            onCheckedChange = {
                                if (!localProxyForced) {
                                    enableLocalProxy = it
                                    if (!it && appendHttpProxy) {
                                        appendHttpProxy = false
                                    }
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_proxy_sharing_enabled, R.string.summary_pref_proxy_sharing_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_promotion_24dp),
                            title = stringResource(R.string.title_pref_proxy_sharing_enabled),
                            summary = stringResource(R.string.summary_pref_proxy_sharing_enabled),
                            checked = proxySharing,
                            enabled = effectiveLocalProxy,
                            onCheckedChange = { proxySharing = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_dynamic_socks_port, R.string.summary_pref_dynamic_socks_port) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_restore_24dp),
                            title = stringResource(R.string.title_pref_dynamic_socks_port),
                            summary = stringResource(R.string.summary_pref_dynamic_socks_port),
                            checked = dynamicSocksPort,
                            enabled = effectiveLocalProxy,
                            onCheckedChange = { dynamicSocksPort = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_socks_port, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_socks_port),
                            value = socksPort,
                            enabled = effectiveLocalProxy && !dynamicSocksPort,
                            keyboardNumber = true,
                            onValueChanged = { socksPort = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_socks_username, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_about_24dp),
                            title = stringResource(R.string.title_pref_socks_username),
                            value = socksUsername,
                            enabled = effectiveLocalProxy,
                            onValueChanged = { socksUsername = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_socks_password, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_lock_24dp),
                            title = stringResource(R.string.title_pref_socks_password),
                            value = socksPassword,
                            enabled = effectiveLocalProxy,
                            isPassword = true,
                            onValueChanged = { socksPassword = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_socks_enable_udp, R.string.summary_pref_socks_enable_udp) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_flash_on_24dp),
                            title = stringResource(R.string.title_pref_socks_enable_udp),
                            summary = stringResource(R.string.summary_pref_socks_enable_udp),
                            checked = socksEnableUdp,
                            enabled = effectiveLocalProxy,
                            onCheckedChange = { socksEnableUdp = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_remote_dns, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_cloud_download_24dp),
                            title = stringResource(R.string.title_pref_remote_dns),
                            value = remoteDns,
                            onValueChanged = { remoteDns = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_domestic_dns, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_domestic_dns),
                            value = domesticDns,
                            onValueChanged = { domesticDns = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_dns_hosts, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_file_24dp),
                            title = stringResource(R.string.title_pref_dns_hosts),
                            value = dnsHosts,
                            onValueChanged = { dnsHosts = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_core_loglevel, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_logcat_24dp),
                            title = stringResource(R.string.title_core_loglevel),
                            entries = coreLogLevelEntries,
                            values = coreLogLevelValues,
                            selectedValue = coreLogLevel,
                            onSelected = { coreLogLevel = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_outbound_domain_resolve_method, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_outline_filter_alt_24),
                            title = stringResource(R.string.title_outbound_domain_resolve_method),
                            entries = outboundResolveEntries,
                            values = outboundResolveValues,
                            selectedValue = outboundResolveMethod,
                            onSelected = { outboundResolveMethod = it },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_mux_settings,
            iconVector = Icons.Filled.Hub, entries = listOf(
                SettingsEntry(R.string.title_pref_mux_enabled, R.string.summary_pref_mux_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_play_24dp),
                            title = stringResource(R.string.title_pref_mux_enabled),
                            summary = stringResource(R.string.summary_pref_mux_enabled),
                            checked = mux,
                            onCheckedChange = { mux = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_mux_concurrency, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_mux_concurrency),
                            value = muxConcurrency,
                            enabled = mux,
                            keyboardNumber = true,
                            onValueChanged = { muxConcurrency = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_mux_xudp_concurrency, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_mux_xudp_concurrency),
                            value = muxXudpConcurrency,
                            enabled = mux,
                            keyboardNumber = true,
                            onValueChanged = { muxXudpConcurrency = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_mux_xudp_quic, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_lock_24dp),
                            title = stringResource(R.string.title_pref_mux_xudp_quic),
                            entries = xudpQuicEntries,
                            values = xudpQuicValues,
                            selectedValue = muxXudpQuic,
                            enabled = mux && muxXudpConcurrencyInt >= 0,
                            onSelected = { muxXudpQuic = it },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_fragment_settings,
            iconVector = Icons.Filled.Extension, entries = listOf(
                SettingsEntry(R.string.title_pref_fragment_enabled, null) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_copy),
                            title = stringResource(R.string.title_pref_fragment_enabled),
                            checked = fragment,
                            onCheckedChange = { fragment = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_fragment_packets, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_file_24dp),
                            title = stringResource(R.string.title_pref_fragment_packets),
                            entries = fragmentPacketsEntries,
                            values = fragmentPacketsValues,
                            selectedValue = fragmentPackets,
                            enabled = fragment,
                            onSelected = { fragmentPackets = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_fragment_length, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_fragment_length),
                            value = fragmentLength,
                            enabled = fragment,
                            onValueChanged = { fragmentLength = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_fragment_interval, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_restore_24dp),
                            title = stringResource(R.string.title_pref_fragment_interval),
                            value = fragmentInterval,
                            enabled = fragment,
                            onValueChanged = { fragmentInterval = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_fragment_maxsplit, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_fragment_maxsplit),
                            value = fragmentMaxSplit,
                            enabled = fragment,
                            keyboardNumber = true,
                            onValueChanged = { fragmentMaxSplit = it },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_observatory_settings,
            iconVector = Icons.Filled.Visibility, entries = listOf(
                SettingsEntry(R.string.title_pref_observatory_least_ping_interval, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_restore_24dp),
                            title = stringResource(R.string.title_pref_observatory_least_ping_interval),
                            value = observatoryLeastPingInterval,
                            onValueChanged = {
                                viewModel.validateObservatoryDuration(it)?.let { value ->
                                    observatoryLeastPingInterval = value
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_observatory_least_load_interval, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_observatory_least_load_interval),
                            value = observatoryLeastLoadInterval,
                            onValueChanged = {
                                viewModel.validateObservatoryDuration(it)?.let { value ->
                                    observatoryLeastLoadInterval = value
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_observatory_least_load_method, null) { shape ->
                        SettingsListItem(
                            icon = painterResource(R.drawable.ic_outline_filter_alt_24),
                            title = stringResource(R.string.title_pref_observatory_least_load_method),
                            entries = observatoryLeastLoadMethodEntries,
                            values = observatoryLeastLoadMethodValues,
                            selectedValue = observatoryLeastLoadMethod,
                            onSelected = { observatoryLeastLoadMethod = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_observatory_least_load_sampling, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_observatory_least_load_sampling),
                            value = observatoryLeastLoadSampling,
                            keyboardNumber = true,
                            onValueChanged = {
                                viewModel.validateObservatorySampling(it)?.let { value ->
                                    observatoryLeastLoadSampling = value
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_observatory_least_load_timeout, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_save_24dp),
                            title = stringResource(R.string.title_pref_observatory_least_load_timeout),
                            value = observatoryLeastLoadTimeout,
                            onValueChanged = {
                                viewModel.validateObservatoryDuration(it)?.let { value ->
                                    observatoryLeastLoadTimeout = value
                                }
                            },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_advanced,
            iconVector = Icons.Filled.MoreHoriz, entries = listOf(
                SettingsEntry(R.string.title_pref_is_booted, R.string.summary_pref_is_booted) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_play_24dp),
                            title = stringResource(R.string.title_pref_is_booted),
                            summary = stringResource(R.string.summary_pref_is_booted),
                            checked = isBooted,
                            onCheckedChange = { isBooted = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_system_vpn_settings, R.string.summary_system_vpn_settings) { shape ->
                    if (systemVpnSettingsAvailable) {
                        SettingsMenuItem(
                            icon = painterResource(R.drawable.ic_settings_24dp),
                            title = stringResource(R.string.title_system_vpn_settings),
                            subtitle = stringResource(R.string.summary_system_vpn_settings),
                            onClick = onSystemVpnSettingsClicked,
                            shape = shape
                        )
                    }
                },
                SettingsEntry(R.string.title_pref_delay_test_url, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_cloud_download_24dp),
                            title = stringResource(R.string.title_pref_delay_test_url),
                            value = delayTestUrl,
                            onValueChanged = { delayTestUrl = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_real_ping_concurrency, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_edit_24dp),
                            title = stringResource(R.string.title_pref_real_ping_concurrency),
                            value = realPingConcurrency,
                            keyboardNumber = true,
                            onValueChanged = { realPingConcurrency = it },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_pref_ip_api_url, null) { shape ->
                        SettingsEditItem(
                            icon = painterResource(R.drawable.ic_description_24dp),
                            title = stringResource(R.string.title_pref_ip_api_url),
                            value = ipApiUrl,
                            onValueChanged = { ipApiUrl = it },
                        shape = shape
                        )
                },
            )
        ),
        SettingsSection(
            R.string.title_mode_settings,
            iconVector = Icons.Filled.Tune, entries = listOf(
                SettingsEntry(R.string.title_mode, null) { shape ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = if (com.v2ray.md.ui.compose.LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_mode),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SingleSelectButtonGroup(
                                    options = modeEntries,
                                    selectedIndex = modeValues.indexOf(mode),
                                    onSelect = { mode = modeValues[it] },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                },
                SettingsEntry(R.string.title_mode_help, null) { shape ->
                        SettingsMenuItem(
                            icon = painterResource(R.drawable.ic_about_24dp),
                            title = stringResource(R.string.title_mode_help),
                            onClick = onModeHelpClicked,
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_root_mode_enabled, R.string.summary_root_mode_enabled) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_lock_24dp),
                            title = stringResource(R.string.title_root_mode_enabled),
                            summary = stringResource(R.string.summary_root_mode_enabled),
                            checked = enableRootMode,
                            onCheckedChange = { newValue ->
                                if (newValue && !RootManager.cachedRoot()) {
                                    viewModel.checkAndRequestRoot {
                                        enableRootMode = true
                                    }
                                } else {
                                    enableRootMode = newValue
                                }
                            },
                        shape = shape
                        )
                },
                SettingsEntry(R.string.title_root_lan_sharing, R.string.summary_root_lan_sharing) { shape ->
                        SettingsSwitchItem(
                            icon = painterResource(R.drawable.ic_share_24dp),
                            title = stringResource(R.string.title_root_lan_sharing),
                            summary = stringResource(R.string.summary_root_lan_sharing),
                            checked = lanSharing,
                            onCheckedChange = { newValue ->
                                if (newValue && !RootManager.cachedRoot()) {
                                    viewModel.checkAndRequestRoot {
                                        lanSharing = true
                                    }
                                } else {
                                    lanSharing = newValue
                                }
                            },
                        shape = shape
                        )
                },
            )
        )
    )

    val openSection = sections.find { it.titleRes == openSectionRes }
    BackHandler(enabled = openSectionRes != null) { openSectionRes = null }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
            if (openSectionRes != null) {
                val openSection = sections.find { it.titleRes == openSectionRes }
                val p = openSection?.iconVector?.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) } ?: openSection?.iconRes?.let { painterResource(it) }
                LargeFlexibleTopAppBar(
                    title = {
                        Text(stringResource(openSectionRes ?: R.string.title_settings), modifier = Modifier.padding(start = 8.dp))
                    },
                    navigationIcon = {
                        MorphFilledTonalIconButton(
                            onClick = { if (openSectionRes != null) openSectionRes = null else onBackClick() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(R.string.acc_back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            } else {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(stringResource(R.string.title_settings), modifier = Modifier.padding(start = 8.dp))
                    },
                    navigationIcon = {
                        MorphFilledTonalIconButton(
                            onClick = { if (openSectionRes != null) openSectionRes = null else onBackClick() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(R.string.acc_back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
                AnimatedVisibility(
                    visible = isLoading,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(scrollState)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp)
            ) {
                if (searchQuery.isNotBlank()) {
                    sections.forEach { section ->
                        val matches = section.entries.filter { it.matches(searchQuery) }
                        if (matches.isNotEmpty()) {
                            SegmentedColumn {
                                matches.forEach { entry ->
                                    item { shape -> entry.content(shape) }
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = openSection,
                        transitionSpec = {
                            // Into a sub-page: new content slides from the end;
                            // back to overview: slides back from the start.
                            if (targetState != null) {
                                (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width / 3 } + fadeOut()) using
                                    SizeTransform(clip = false)
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width / 3 } + fadeOut()) using
                                    SizeTransform(clip = false)
                            }
                        },
                        label = "settings-section"
                    ) { section ->
                        if (section == null) {
                            SegmentedColumn {
                                sections.forEach { overview ->
                                    item(key = overview.titleRes) { shape ->
                                        SettingsMenuItem(
                                            icon = overview.iconVector?.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) } ?: overview.iconRes?.let { painterResource(it) },
                                            title = stringResource(overview.titleRes),
                                            onClick = { openSectionRes = overview.titleRes },
                                            shape = shape
                                        )
                                    }
                                }
                            }
                        } else {
                            SegmentedColumn {
                                section.entries.forEach { entry ->
                                    item { shape -> entry.content(shape) }
                                }
                            }
                        }
                    }
                }
            Spacer(modifier = Modifier.height(24.dp))
            NavigationBarsSpacer()
            }
        }
    }
}
}
