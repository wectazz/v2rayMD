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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.md.R
import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.EConfigType
import com.v2ray.md.extension.isComplexType
import com.v2ray.md.ui.compose.AppDropdownMenuItems

private enum class ImportMenuAction(@StringRes val labelRes: Int, val action: MainAction) {
    QRCode(R.string.menu_item_import_config_qrcode, MainAction.ImportQRcode),
    Clipboard(R.string.menu_item_import_config_clipboard, MainAction.ImportClipboard),
    LocalFile(R.string.menu_item_import_config_local, MainAction.ImportConfigLocal),
    PolicyGroup(R.string.menu_item_import_config_policy_group, MainAction.ImportManually(EConfigType.POLICYGROUP.value)),
    ProxyChain(R.string.menu_item_import_config_proxy_chain, MainAction.ImportManually(EConfigType.PROXYCHAIN.value)),
    Vmess(R.string.menu_item_import_config_manually_vmess, MainAction.ImportManually(EConfigType.VMESS.value)),
    Vless(R.string.menu_item_import_config_manually_vless, MainAction.ImportManually(EConfigType.VLESS.value)),
    Shadowsocks(R.string.menu_item_import_config_manually_ss, MainAction.ImportManually(EConfigType.SHADOWSOCKS.value)),
    Socks(R.string.menu_item_import_config_manually_socks, MainAction.ImportManually(EConfigType.SOCKS.value)),
    Http(R.string.menu_item_import_config_manually_http, MainAction.ImportManually(EConfigType.HTTP.value)),
    Trojan(R.string.menu_item_import_config_manually_trojan, MainAction.ImportManually(EConfigType.TROJAN.value)),
    WireGuard(R.string.menu_item_import_config_manually_wireguard, MainAction.ImportManually(EConfigType.WIREGUARD.value)),
    Hysteria2(R.string.menu_item_import_config_manually_hysteria2, MainAction.ImportManually(EConfigType.HYSTERIA2.value))
}

enum class MainMoreMenuAction(@StringRes val labelRes: Int) {
    RestartService(R.string.title_service_restart),
    DeleteAll(R.string.title_del_all_config),
    DeleteDuplicate(R.string.title_del_duplicate_config),
    DeleteInvalid(R.string.title_del_invalid_config),
    ExportAll(R.string.title_export_all),
    LocateSelected(R.string.title_locate_selected_config),
    SortByTestResults(R.string.title_sort_by_test_results),
    TestAll(R.string.title_ping_all_server),
    TestAllRealPing(R.string.title_real_ping_all_server),
    UpdateSubscriptions(R.string.title_sub_update)
}

internal enum class ServerMenuAction(
    @StringRes val labelRes: Int,
    val isShareAction: Boolean,
    val supportsComplexProfiles: Boolean,
) {
    ShareQRCode(R.string.share_method_qrcode, isShareAction = true, supportsComplexProfiles = false),
    ShareClipboard(R.string.share_method_clipboard, isShareAction = true, supportsComplexProfiles = false),
    ShareFullContent(R.string.share_method_full_content, isShareAction = true, supportsComplexProfiles = true),
    Edit(R.string.action_edit, isShareAction = false, supportsComplexProfiles = true),
    Delete(R.string.action_delete, isShareAction = false, supportsComplexProfiles = true),
}

internal fun serverMenuActions(
    isComplexProfile: Boolean,
    includeManagementActions: Boolean,
): List<ServerMenuAction> = ServerMenuAction.entries.filter { action ->
    (includeManagementActions || action.isShareAction) && (!isComplexProfile || action.supportsComplexProfiles)
}

@Composable
fun ImportMenuContent(expanded: Boolean, onDismissRequest: () -> Unit, onAction: (MainAction) -> Unit) = com.v2ray.md.ui.compose.AppBottomSheetMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    items = ImportMenuAction.entries,
    labelRes = { it.labelRes },
    onSelected = { onAction(it.action) }
)

@Composable
fun MoreMenuContent(expanded: Boolean, onDismissRequest: () -> Unit, onSelected: (MainMoreMenuAction) -> Unit) = com.v2ray.md.ui.compose.AppBottomSheetMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    items = MainMoreMenuAction.entries,
    labelRes = { it.labelRes },
    onSelected = onSelected
)

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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            menuActions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            when (action) {
                                ServerMenuAction.ShareQRCode -> onAction(MainAction.ShareQRCode(guid))
                                ServerMenuAction.ShareClipboard -> onAction(MainAction.ShareClipboard(guid))
                                ServerMenuAction.ShareFullContent -> onAction(MainAction.ShareFullContent(guid))
                                ServerMenuAction.Edit -> onAction(MainAction.EditServer(guid, profile))
                                ServerMenuAction.Delete -> onRemove(guid)
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(action.labelRes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
