package com.v2ray.md.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.md.R
import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.EConfigType
import com.v2ray.md.extension.isComplexType
import com.v2ray.md.ui.compose.AppDropdownMenuItems
import androidx.compose.runtime.rememberCoroutineScope
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SettingsMenuItem
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.v2ray.md.ui.compose.BottomSheetGroup
import com.v2ray.md.ui.compose.GroupedAppBottomSheetMenu

enum class ImportMenuGroup(
    override val titleRes: Int,
    override val iconVector: ImageVector
) : BottomSheetGroup {
    METHOD(R.string.title_import_method, Icons.Filled.Add),
    MANUAL(R.string.title_add_manual, Icons.Filled.Edit)
}

private enum class ImportMenuAction(
    @StringRes val labelRes: Int,
    val iconVector: ImageVector,
    val group: ImportMenuGroup,
    val action: MainAction
) {
    QRCode(R.string.menu_item_import_config_qrcode, Icons.Filled.QrCode, ImportMenuGroup.METHOD, MainAction.ImportQRcode),
    Clipboard(R.string.menu_item_import_config_clipboard, Icons.Filled.ContentPaste, ImportMenuGroup.METHOD, MainAction.ImportClipboard),
    LocalFile(R.string.menu_item_import_config_local, Icons.Filled.Folder, ImportMenuGroup.METHOD, MainAction.ImportConfigLocal),
    PolicyGroup(R.string.menu_item_import_config_policy_group, Icons.Filled.AccountTree, ImportMenuGroup.METHOD, MainAction.ImportManually(EConfigType.POLICYGROUP.value)),
    ProxyChain(R.string.menu_item_import_config_proxy_chain, Icons.Filled.Link, ImportMenuGroup.METHOD, MainAction.ImportManually(EConfigType.PROXYCHAIN.value)),
    Vmess(R.string.menu_item_import_config_manually_vmess, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.VMESS.value)),
    Vless(R.string.menu_item_import_config_manually_vless, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.VLESS.value)),
    Shadowsocks(R.string.menu_item_import_config_manually_ss, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.SHADOWSOCKS.value)),
    Socks(R.string.menu_item_import_config_manually_socks, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.SOCKS.value)),
    Http(R.string.menu_item_import_config_manually_http, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.HTTP.value)),
    Trojan(R.string.menu_item_import_config_manually_trojan, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.TROJAN.value)),
    WireGuard(R.string.menu_item_import_config_manually_wireguard, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.WIREGUARD.value)),
    Hysteria2(R.string.menu_item_import_config_manually_hysteria2, Icons.Filled.Add, ImportMenuGroup.MANUAL, MainAction.ImportManually(EConfigType.HYSTERIA2.value))
}

enum class MainMoreMenuGroup(
    override val titleRes: Int,
    override val iconVector: ImageVector
) : BottomSheetGroup {
    ACTIONS(R.string.title_actions, Icons.Filled.Build),
    TESTS(R.string.title_tests, Icons.Filled.Speed),
    DELETE(R.string.title_delete, Icons.Filled.Delete)
}

enum class MainMoreMenuAction(
    @StringRes val labelRes: Int,
    val iconVector: ImageVector,
    val group: MainMoreMenuGroup
) {
    RestartService(R.string.title_service_restart, Icons.Filled.Refresh, MainMoreMenuGroup.ACTIONS),
    UpdateSubscriptions(R.string.title_sub_update, Icons.Filled.CloudDownload, MainMoreMenuGroup.ACTIONS),
    ExportAll(R.string.title_export_all, Icons.Filled.Share, MainMoreMenuGroup.ACTIONS),
    LocateSelected(R.string.title_locate_selected_config, Icons.Filled.Place, MainMoreMenuGroup.ACTIONS),
    SortByTestResults(R.string.title_sort_by_test_results, Icons.Filled.Sort, MainMoreMenuGroup.ACTIONS),
    
    TestAll(R.string.title_ping_all_server, Icons.Filled.NetworkPing, MainMoreMenuGroup.TESTS),
    TestAllRealPing(R.string.title_real_ping_all_server, Icons.Filled.Speed, MainMoreMenuGroup.TESTS),
    
    DeleteDuplicate(R.string.title_del_duplicate_config, Icons.Filled.DeleteOutline, MainMoreMenuGroup.DELETE),
    DeleteInvalid(R.string.title_del_invalid_config, Icons.Filled.DeleteSweep, MainMoreMenuGroup.DELETE),
    DeleteAll(R.string.title_del_all_config, Icons.Filled.DeleteForever, MainMoreMenuGroup.DELETE)
}

internal enum class ServerMenuAction(
    @StringRes val labelRes: Int,
    val isShareAction: Boolean,
    val supportsComplexProfiles: Boolean,
    val iconVector: ImageVector
) {
    ShareQRCode(R.string.share_method_qrcode, isShareAction = true, supportsComplexProfiles = false, Icons.Filled.QrCode),
    ShareClipboard(R.string.share_method_clipboard, isShareAction = true, supportsComplexProfiles = false, Icons.Filled.ContentPaste),
    ShareFullContent(R.string.share_method_full_content, isShareAction = true, supportsComplexProfiles = true, Icons.Filled.Share),
    Edit(R.string.action_edit, isShareAction = false, supportsComplexProfiles = true, Icons.Filled.Edit),
    Delete(R.string.action_delete, isShareAction = false, supportsComplexProfiles = true, Icons.Filled.Delete),
}

internal fun serverMenuActions(
    isComplexProfile: Boolean,
    includeManagementActions: Boolean,
): List<ServerMenuAction> = ServerMenuAction.entries.filter { action ->
    (includeManagementActions || action.isShareAction) && (!isComplexProfile || action.supportsComplexProfiles)
}

@Composable
fun ImportMenuContent(expanded: Boolean, onDismissRequest: () -> Unit, onAction: (MainAction) -> Unit) = GroupedAppBottomSheetMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    items = ImportMenuAction.entries,
    groupBy = { it.group },
    labelRes = { it.labelRes },
    iconVector = { it.iconVector },
    onSelected = { onAction(it.action) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuContent(expanded: Boolean, onDismissRequest: () -> Unit, onSelected: (MainMoreMenuAction) -> Unit) {
    if (expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        var fastActionsExpanded by remember { mutableStateOf(false) }
        var managementExpanded by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                SegmentedColumn {
                    item { shape ->
                        SettingsMenuItem(
                            icon = painterResource(R.drawable.ic_play_24dp),
                            title = stringResource(R.string.title_fast_actions),
                            onClick = { fastActionsExpanded = !fastActionsExpanded },
                            shape = shape
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = fastActionsExpanded) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.title_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        SegmentedColumn {
                            MainMoreMenuAction.entries.filter { it.group == MainMoreMenuGroup.ACTIONS }.forEach { item ->
                                item { shape ->
                                    SettingsMenuItem(
                                        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(item.iconVector),
                                        title = stringResource(item.labelRes),
                                        onClick = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismissRequest()
                                                    onSelected(item)
                                                }
                                            }
                                        },
                                        shape = shape
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        SegmentedColumn {
                            MainMoreMenuAction.entries.filter { it.group == MainMoreMenuGroup.TESTS }.forEach { item ->
                                item { shape ->
                                    SettingsMenuItem(
                                        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(item.iconVector),
                                        title = stringResource(item.labelRes),
                                        onClick = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismissRequest()
                                                    onSelected(item)
                                                }
                                            }
                                        },
                                        shape = shape
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SegmentedColumn {
                    item { shape ->
                        SettingsMenuItem(
                            icon = painterResource(R.drawable.ic_settings_24dp),
                            title = stringResource(R.string.title_management),
                            onClick = { managementExpanded = !managementExpanded },
                            shape = shape
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = managementExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        SegmentedColumn {
                            MainMoreMenuAction.entries.filter { it.group == MainMoreMenuGroup.DELETE }.forEach { item ->
                                item { shape ->
                                    SettingsMenuItem(
                                        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(item.iconVector),
                                        title = stringResource(item.labelRes),
                                        onClick = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismissRequest()
                                                    onSelected(item)
                                                }
                                            }
                                        },
                                        shape = shape
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMethodDialog(
    guid: String,
    profile: ProfileItem,
    more: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
    onRemove: (String) -> Unit,
) {
    val menuActions = serverMenuActions(
        isComplexProfile = profile.configType.isComplexType(),
        includeManagementActions = more,
    )
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        SegmentedColumn(modifier = Modifier.navigationBarsPadding()) {
            menuActions.forEach { action ->
                item { shape ->
                    SettingsMenuItem(
                        title = stringResource(action.labelRes),
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                    when (action) {
                                        ServerMenuAction.ShareQRCode -> onAction(MainAction.ShareQRCode(guid))
                                        ServerMenuAction.ShareClipboard -> onAction(MainAction.ShareClipboard(guid))
                                        ServerMenuAction.ShareFullContent -> onAction(MainAction.ShareFullContent(guid))
                                        ServerMenuAction.Edit -> onAction(MainAction.EditServer(guid, profile))
                                        ServerMenuAction.Delete -> onRemove(guid)
                                    }
                                }
                            }
                        },
                        shape = shape
                    )
                }
            }
        }
    }
}
