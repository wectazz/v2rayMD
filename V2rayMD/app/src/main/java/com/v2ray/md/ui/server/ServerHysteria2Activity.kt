package com.v2ray.md.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.v2ray.md.AppConfig
import com.v2ray.md.R
import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.EConfigType
import com.v2ray.md.extension.toast
import com.v2ray.md.ui.compose.FormTextField
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SettingsSwitchItem

class ServerHysteria2Activity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.HYSTERIA2

    @Composable
    override fun ScreenContent() {
        val scope = rememberCoroutineScope()
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(
                initialConfig = initialConfig
            )
        }.apply {
            configType = EConfigType.HYSTERIA2
        }

        ServerEditorScaffold(
            title = serverConfigType.toString(),
            onSaveClick = { saveServer(uiState) }
        ) {
            CommonBasicFields(uiState)
            Hysteria2ProtocolFields(uiState)

        }
    }

    override fun validateProtocolConfig(config: ProfileItem): Boolean {
        if (config.password.isNullOrBlank()) {
            toast(R.string.server_lab_id3)
            return false
        }
        if (config.security.isNullOrBlank()) {
            config.security = AppConfig.TLS
        }
        return true
    }

    @Composable
    private fun Hysteria2ProtocolFields(state: ServerUiState) {
        FormTextField(
            stringResource(R.string.server_lab_id3),
            state.password,
            { state.password = it }
        )
        FormTextField(
            stringResource(R.string.server_obfs_password),
            state.obfsPassword,
            { state.obfsPassword = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_port_hop),
            state.portHopping,
            { state.portHopping = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_port_hop_interval),
            state.portHoppingInterval,
            { state.portHoppingInterval = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_bandwidth_down),
            state.bandwidthDown,
            { state.bandwidthDown = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_bandwidth_up),
            state.bandwidthUp,
            { state.bandwidthUp = it }
        )
        SegmentedColumn {
            item { shape ->
                SettingsSwitchItem(
                    title = stringResource(R.string.server_lab_allow_insecure),
                    checked = state.allowInsecure,
                    onCheckedChange = { state.allowInsecure = it },
                    shape = shape
                )
            }
        }
        FormTextField(
            stringResource(R.string.server_lab_sni),
            state.sni,
            { state.sni = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_ech_config_list),
            state.echConfigList,
            { state.echConfigList = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_pinned_ca256),
            state.pinnedCA256,
            { state.pinnedCA256 = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_final_mask),
            state.finalMask,
            { state.finalMask = it }
        )
    }
}

