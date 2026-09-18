@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.v2ray.md.ui.subscription

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.AppConfig
import com.v2ray.md.R
import com.v2ray.md.extension.toast
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.handler.MmkvManager.rememberMmkvBool
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.DeleteConfirmDialog
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.QRCodeDialog
import com.v2ray.md.ui.compose.ReorderableListItem
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SelectListDialog
import com.v2ray.md.ui.compose.SettingsSwitchItem
import com.v2ray.md.ui.compose.SwitchCheckThumb
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.Utils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class SubscriptionShareAction(@StringRes val labelRes: Int) {
    QRCode(R.string.share_subscription_qrcode),
    Clipboard(R.string.share_subscription_clipboard)
}

class SubSettingActivity : BaseComponentActivity() {
    private val viewModel: SubscriptionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        SubSettingScreen(
            viewModel = viewModel,
            isLoading = isLoading,
            onBackClick = { finish() },
            onAddClick = { startActivity(Intent(this, SubEditActivity::class.java)) },
            onSubUpdate = { viewModel.updateSubscriptions() },
            onEditSub = { subId ->
                startActivity(Intent(this, SubEditActivity::class.java).putExtra("subId", subId))
            },
            onRemoveSub = { subId -> removeSub(subId) },
            onShareQRCode = viewModel::shareQRCode,
            onShareClipboard = { url ->
                Utils.setClipboard(this, url)
                toast(getString(R.string.toast_success))
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun removeSub(subId: String) {
        viewModel.remove(subId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubSettingScreen(
    viewModel: SubscriptionsViewModel,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onSubUpdate: () -> Unit,
    onEditSub: (String) -> Unit,
    onRemoveSub: (String) -> Unit,
    onShareQRCode: (String) -> Unit,
    onShareClipboard: (String) -> Unit
) {
    val subscriptions by viewModel.subsFlow.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<String?>(null) }
    val confirmRemove = MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE, false)

    var shareTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val qrCodeBitmap by viewModel.qrCode.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.move(from.index, to.index)
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.title_sub_setting), modifier = Modifier.padding(start = 8.dp)) },
                    navigationIcon = {
                        MorphFilledTonalIconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(R.string.acc_back)
                            )
                        }
                    },
                    actions = {
                        MorphFilledTonalIconButton( onClick = onAddClick) {
                            Icon(painterResource(R.drawable.ic_add_24dp), contentDescription = stringResource(R.string.acc_add_subscription))
                        }
                        MorphFilledTonalIconButton(
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_restore_24dp), contentDescription = stringResource(R.string.acc_update_subscriptions))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
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
            itemsIndexed(
                items = subscriptions,
                key = { _, item -> item.guid }
            ) { _, subCache ->
                ReorderableItem(reorderableState, key = subCache.guid) { isDragging ->
                    ReorderableListItem(
                        scope = this,
                        isDragging = isDragging
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
                                Text(
                                    text = subCache.subscription.remarks,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subCache.subscription.url.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subCache.subscription.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Utils.formatTimestamp(subCache.subscription.lastUpdated),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row {
                                    if (subCache.subscription.url.isNotEmpty()) {
                                        MorphIconButton( onClick = {
                                            shareTarget = Pair(subCache.guid, subCache.subscription.url)
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_share_24dp),
                                                contentDescription = stringResource(R.string.acc_share_subscription)
                                            )
                                        }
                                    }
                                    MorphIconButton( onClick = { onEditSub(subCache.guid) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit_24dp),
                                            contentDescription = stringResource(R.string.acc_edit)
                                        )
                                    }
                                    MorphIconButton( onClick = {
                                        if (confirmRemove) removeTarget = subCache.guid
                                        else onRemoveSub(subCache.guid)
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete_24dp),
                                            contentDescription = stringResource(R.string.acc_delete)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Switch(
                                    checked = subCache.subscription.enabled,
                                    onCheckedChange = { checked ->
                                        val updated = subCache.subscription.copy()
                                        updated.enabled = checked
                                        viewModel.update(subCache.guid, updated)
                                    },
                                    thumbContent = { SwitchCheckThumb(subCache.subscription.enabled) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (shareTarget != null) {
        val (_, url) = shareTarget!!
        SelectListDialog(
            options = SubscriptionShareAction.entries,
            optionText = { stringResource(it.labelRes) },
            onSelected = { action ->
                shareTarget = null
                when (action) {
                    SubscriptionShareAction.QRCode -> onShareQRCode(url)
                    SubscriptionShareAction.Clipboard -> onShareClipboard(url)
                }
            },
            onDismiss = { shareTarget = null }
        )
    }

    // QR Code Dialog
    if (qrCodeBitmap != null) {
        QRCodeDialog(
            bitmap = qrCodeBitmap,
            onDismiss = viewModel::dismissQRCode
        )
    }

    if (removeTarget != null) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_subscription_group),
            onConfirm = {
                onRemoveSub(removeTarget!!)
                removeTarget = null
            },
            onDismiss = { removeTarget = null }
        )
    }

    if (showUpdateDialog) {

        var updateSubscription by rememberMmkvBool(AppConfig.PREF_UPDATE_SUBSCRIPTION, true)
        var autoTestAfterUpdateSubscription by rememberMmkvBool(AppConfig.PREF_AUTO_TEST_AFTER_UPDATE_SUBSCRIPTION, false)
        var autoRemoveInvalidAfterTest by rememberMmkvBool(AppConfig.PREF_AUTO_REMOVE_INVALID_AFTER_TEST, false)
        var autoSortAfterTest by rememberMmkvBool(AppConfig.PREF_AUTO_SORT_AFTER_TEST, false)
        var sendHwid by rememberMmkvBool(AppConfig.PREF_SEND_HWID, true)

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            text = {
                val scrollState = rememberScrollState()
                SegmentedColumn(modifier = Modifier.verticalScroll(scrollState)) {
                    item { shape ->
                        SettingsSwitchItem(
                            title = stringResource(R.string.title_sub_update),
                            checked = updateSubscription,
                            onCheckedChange = { updateSubscription = it },
                            shape = shape
                        )
                    }
                    item { shape ->
                        SettingsSwitchItem(
                            title = stringResource(R.string.title_pref_auto_test_after_update_subscription),
                            summary = stringResource(R.string.summary_pref_auto_test_after_update_subscription),
                            checked = autoTestAfterUpdateSubscription,
                            onCheckedChange = { autoTestAfterUpdateSubscription = it },
                            shape = shape
                        )
                    }
                    item { shape ->
                        SettingsSwitchItem(
                            title = stringResource(R.string.title_pref_auto_remove_invalid_after_test),
                            summary = stringResource(R.string.summary_pref_auto_remove_invalid_after_test),
                            checked = autoRemoveInvalidAfterTest,
                            enabled = autoTestAfterUpdateSubscription,
                            onCheckedChange = { autoRemoveInvalidAfterTest = it },
                            shape = shape
                        )
                    }
                    item { shape ->
                        SettingsSwitchItem(
                            title = stringResource(R.string.title_pref_auto_sort_after_test),
                            summary = stringResource(R.string.summary_pref_auto_sort_after_test),
                            checked = autoSortAfterTest,
                            enabled = autoTestAfterUpdateSubscription,
                            onCheckedChange = { autoSortAfterTest = it },
                            shape = shape
                        )
                    }
                    item { shape ->
                        SettingsSwitchItem(
                            title = stringResource(R.string.title_pref_send_hwid),
                            summary = stringResource(R.string.summary_pref_send_hwid),
                            checked = sendHwid,
                            onCheckedChange = { sendHwid = it },
                            shape = shape
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    onSubUpdate()
                }) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
