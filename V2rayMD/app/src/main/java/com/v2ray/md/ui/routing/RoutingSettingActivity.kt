package com.v2ray.md.ui.routing

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.v2ray.md.AppConfig
import com.v2ray.md.R
import com.v2ray.md.dto.entities.RulesetItem
import com.v2ray.md.enums.RoutingType
import com.v2ray.md.extension.toastError
import com.v2ray.md.extension.toastSuccess
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.handler.SettingsManager
import com.v2ray.md.ui.base.HelperBaseComponentActivity
import com.v2ray.md.ui.compose.AppDropdownMenuItems
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.ReorderableListItem
import com.v2ray.md.ui.compose.SelectListDialog
import com.v2ray.md.ui.compose.SettingsListItem
import com.v2ray.md.ui.compose.SwitchCheckThumb
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.JsonUtil
import com.v2ray.md.util.LogUtil
import com.v2ray.md.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class RoutingMenuAction(@StringRes val labelRes: Int) {
    ImportPredefined(R.string.routing_settings_import_predefined_rulesets),
    ImportClipboard(R.string.routing_settings_import_rulesets_from_clipboard),
    ImportQRCode(R.string.routing_settings_import_rulesets_from_qrcode),
    ExportClipboard(R.string.routing_settings_export_rulesets_to_clipboard)
}

private enum class RoutingPreset(val type: RoutingType, @StringRes val labelRes: Int) {
    ChinaWhitelist(RoutingType.WHITE, R.string.routing_preset_china_whitelist),
    ChinaBlacklist(RoutingType.BLACK, R.string.routing_preset_china_blacklist),
    Global(RoutingType.GLOBAL, R.string.routing_preset_global),
    IranWhitelist(RoutingType.WHITE_IRAN, R.string.routing_preset_iran_whitelist),
    RussiaWhitelist(RoutingType.WHITE_RUSSIA, R.string.routing_preset_russia_whitelist)
}

class RoutingSettingActivity : HelperBaseComponentActivity() {
    private val viewModel: RoutingSettingsViewModel by viewModels()
    private val domainStrategyState = MutableStateFlow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        domainStrategyState.value = getDomainStrategy()
    }

    @Composable
    override fun ScreenContent() {
        RoutingSettingScreen(
            viewModel = viewModel,
            domainStrategyState = domainStrategyState,
            onBackClick = { finish() },
            onAddRule = { startActivity(Intent(this, RoutingEditActivity::class.java)) },
            onEditRule = { position ->
                startActivity(Intent(this, RoutingEditActivity::class.java).putExtra("position", position))
            },
            onDomainStrategySelected = { value ->
                MmkvManager.encodeSettings(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, value)
                domainStrategyState.value = value
            },
            onImportPredefined = { type -> importPredefined(type) },
            onImportClipboard = { importFromClipboard() },
            onImportQRcode = { importQRcode() },
            onExportClipboard = { export2Clipboard() }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun getDomainStrategy(): String {
        val strategies = resources.getStringArray(R.array.routing_domain_strategy)
        return MmkvManager.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY) ?: strategies.first()
    }

    private fun importPredefined(type: RoutingType) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SettingsManager.resetRoutingRulesetsFromPresets(this@RoutingSettingActivity, type)
                launch(Dispatchers.Main) {
                    viewModel.reload()
                    toastSuccess(R.string.toast_success)
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import predefined ruleset", e)
            }
        }
    }

    private fun importFromClipboard() {
        val clipboard = try {
            Utils.getClipboard(this)
        } catch (e: Exception) {
            toastError(R.string.toast_failure)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = SettingsManager.resetRoutingRulesets(clipboard)
            withContext(Dispatchers.Main) {
                if (result) {
                    viewModel.reload()
                    toastSuccess(R.string.toast_success)
                } else {
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = SettingsManager.resetRoutingRulesets(scanResult)
                    withContext(Dispatchers.Main) {
                        if (result) {
                            viewModel.reload()
                            toastSuccess(R.string.toast_success)
                        } else {
                            toastError(R.string.toast_failure)
                        }
                    }
                }
            }
        }
    }

    private fun export2Clipboard() {
        val rulesetList = MmkvManager.decodeRoutingRulesets()
        if (rulesetList.isNullOrEmpty()) {
            toastError(R.string.toast_failure)
        } else {
            Utils.setClipboard(this, JsonUtil.toJson(rulesetList))
            toastSuccess(R.string.toast_success)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingSettingScreen(
    viewModel: RoutingSettingsViewModel,
    domainStrategyState: MutableStateFlow<String>,
    onBackClick: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (Int) -> Unit,
    onDomainStrategySelected: (String) -> Unit,
    onImportPredefined: (RoutingType) -> Unit,
    onImportClipboard: () -> Unit,
    onImportQRcode: () -> Unit,
    onExportClipboard: () -> Unit
) {
    val rulesets by viewModel.rulesetsFlow.collectAsStateWithLifecycle()
    val domainStrategy by domainStrategyState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }

    val domainStrategies = stringArrayResource(R.array.routing_domain_strategy).toList()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Lazy list indices include the preceding non-rule content, so resolve the stable rule keys.
        val fromIndex = rulesets.indexOfFirst { it.id == from.key }
        val toIndex = rulesets.indexOfFirst { it.id == to.key }
        viewModel.move(fromIndex, toIndex)
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.routing_settings_title), modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    MorphFilledTonalIconButton( onClick = onBackClick) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.acc_back)
                        )
                    }
                },
                actions = {
                    MorphFilledTonalIconButton( onClick = onAddRule) {
                        Icon(
                            painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.acc_add_rule)
                        )
                    }
                    Box {
                        MorphFilledTonalIconButton( onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(R.string.acc_more)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            AppDropdownMenuItems(RoutingMenuAction.entries, { it.labelRes }) { action ->
                                showMenu = false
                                when (action) {
                                    RoutingMenuAction.ImportPredefined -> showPresetDialog = true
                                    RoutingMenuAction.ImportClipboard -> onImportClipboard()
                                    RoutingMenuAction.ImportQRCode -> onImportQRcode()
                                    RoutingMenuAction.ExportClipboard -> onExportClipboard()
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScrollbar(lazyListState),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "domain_strategy") {
                SettingsListItem(
                    title = stringResource(R.string.routing_settings_domain_strategy),
                    entries = domainStrategies,
                    values = domainStrategies,
                    selectedValue = domainStrategy,
                    onSelected = { onDomainStrategySelected(it) }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.routing_settings_rule_title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            itemsIndexed(
                items = rulesets,
                key = { _, ruleset -> ruleset.id }
            ) { index, ruleset ->
                ReorderableItem(reorderableState, key = ruleset.id) { isDragging ->
                    ReorderableListItem(
                        scope = this,
                        isDragging = isDragging
                    ) {
                        RoutingRulesetItem(
                            ruleset = ruleset,
                            onEdit = { onEditRule(index) },
                            onEnabledChange = { checked ->
                                val updated = ruleset.copy(enabled = checked)
                                viewModel.update(index, updated)
                            }
                        )
                    }
                }
            }
        }
    }


    if (showPresetDialog) {
        SelectListDialog(
            title = stringResource(R.string.routing_settings_import_predefined_rulesets),
            options = RoutingPreset.entries,
            optionText = { stringResource(it.labelRes) },
            onSelected = { preset ->
                showPresetDialog = false
                onImportPredefined(preset.type)
            },
            onDismiss = { showPresetDialog = false }
        )
    }
}

@Composable
private fun RoutingRulesetItem(
    ruleset: RulesetItem,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ruleset.remarks ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ruleset.locked == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_24dp),
                        contentDescription = stringResource(R.string.acc_locked),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val domainIpInfo = (ruleset.domain ?: ruleset.ip ?: ruleset.process ?: ruleset.port)?.toString() ?: ""
            if (domainIpInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = domainIpInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!ruleset.outboundTag.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ruleset.outboundTag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            MorphIconButton( onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_24dp),
                    contentDescription = stringResource(R.string.acc_edit)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Switch(
                checked = ruleset.enabled ?: false,
                onCheckedChange = onEnabledChange,
                thumbContent = { SwitchCheckThumb(ruleset.enabled ?: false) }
            )
        }
    }
}
