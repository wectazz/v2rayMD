package com.v2ray.md.ui.userasset

import android.os.Bundle
import android.text.TextUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.v2ray.md.AppConfig
import com.v2ray.md.R
import com.v2ray.md.dto.entities.AssetUrlItem
import com.v2ray.md.extension.toast
import com.v2ray.md.extension.toastSuccess
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.DeleteConfirmDialog
import com.v2ray.md.ui.compose.FormCard
import com.v2ray.md.ui.compose.FormTextField
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.NavigationBarsSpacer
import com.v2ray.md.util.LogUtil
import com.v2ray.md.util.Utils
import java.io.File

class UserAssetUrlActivity : BaseComponentActivity() {
    companion object {
        const val ASSET_URL_QRCODE = "ASSET_URL_QRCODE"
    }

    private val extDir by lazy { File(Utils.userAssetPath(this)) }
    private val editAssetId by lazy { intent.getStringExtra("assetId").orEmpty() }
    private lateinit var initialRemarks: String
    private lateinit var initialUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetItem = MmkvManager.decodeAsset(editAssetId)
        val assetUrlQrcode = intent.getStringExtra(ASSET_URL_QRCODE)
        val assetNameQrcode = File(assetUrlQrcode.toString()).name

        when {
            assetItem != null -> {
                initialRemarks = assetItem.remarks
                initialUrl = assetItem.url
            }

            assetUrlQrcode != null -> {
                initialRemarks = assetNameQrcode
                initialUrl = assetUrlQrcode
            }

            else -> {
                initialRemarks = ""
                initialUrl = ""
            }
        }
    }

    @Composable
    override fun ScreenContent() {
        UserAssetUrlScreen(
            editAssetId = editAssetId,
            initialRemarks = initialRemarks,
            initialUrl = initialUrl,
            onBackClick = { finish() },
            onSave = { r, u -> saveServer(r, u) },
            onDelete = { deleteServer() }
        )
    }

    private fun saveServer(remarks: String, url: String): Boolean {
        val assetList = MmkvManager.decodeAssetUrls()
        if (assetList.any { it.assetUrl.remarks == remarks && it.guid != editAssetId }) {
            toast(R.string.msg_remark_is_duplicate)
            return false
        }
        if (TextUtils.isEmpty(remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }
        if (TextUtils.isEmpty(url)) {
            toast(R.string.title_url)
            return false
        }

        var assetItem = MmkvManager.decodeAsset(editAssetId)
        var assetId = editAssetId
        if (assetItem != null) {
            val file = extDir.resolve(assetItem.remarks)
            if (file.exists()) {
                try {
                    file.delete()
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to delete asset file: ${file.path}", e)
                }
            }
        } else {
            assetId = Utils.getUuid()
            assetItem = AssetUrlItem()
        }

        assetItem.remarks = remarks
        assetItem.url = url

        MmkvManager.encodeAsset(assetId, assetItem)
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    private fun deleteServer(): Boolean {
        if (editAssetId.isNotEmpty()) {
            MmkvManager.removeAssetUrl(editAssetId)
            finish()
        }
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAssetUrlScreen(
    editAssetId: String,
    initialRemarks: String,
    initialUrl: String,
    onBackClick: () -> Unit,
    onSave: (String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var remarks by rememberSaveable(editAssetId, initialRemarks) { mutableStateOf(initialRemarks) }
    var url by rememberSaveable(editAssetId, initialUrl) { mutableStateOf(initialUrl) }
    var showDeleteConfirm by rememberSaveable(editAssetId) { mutableStateOf(false) }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_user_asset_add_url), modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    MorphFilledTonalIconButton( onClick = onBackClick, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.acc_back)
                        )
                    }
                },
                actions = {
                    if (editAssetId.isNotEmpty()) {
                        MorphFilledTonalIconButton( onClick = { showDeleteConfirm = true }) {
                            Icon(
                                painterResource(R.drawable.ic_delete_24dp),
                                contentDescription = stringResource(R.string.acc_delete)
                            )
                        }
                    }
                    MorphFilledTonalIconButton( onClick = { onSave(remarks, url) }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_fab_check),
                            contentDescription = stringResource(R.string.acc_save)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            FormCard {
                FormTextField(
                    label = stringResource(R.string.sub_setting_remarks),
                    value = remarks,
                    onValueChange = { remarks = it }
                )
                FormTextField(
                    label = stringResource(R.string.title_url),
                    value = url,
                    onValueChange = { url = it }
                )
            }
            NavigationBarsSpacer()
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_asset_source),
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
