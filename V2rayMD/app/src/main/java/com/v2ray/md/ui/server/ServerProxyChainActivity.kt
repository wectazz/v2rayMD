package com.v2ray.md.ui.server

import android.os.Bundle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.v2ray.md.R
import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.EConfigType
import com.v2ray.md.extension.isComplexType
import com.v2ray.md.extension.moveItem
import com.v2ray.md.extension.toast
import com.v2ray.md.extension.toastSuccess
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.handler.SettingsManager
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.DeleteConfirmDialog
import com.v2ray.md.ui.compose.FormCard
import com.v2ray.md.ui.compose.FormDropdownField
import com.v2ray.md.ui.compose.FormTextField
import com.v2ray.md.ui.compose.MorphIconButtonShapes
import com.v2ray.md.ui.compose.reorderableDragHandle
import com.v2ray.md.ui.compose.verticalScrollbar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

class ServerProxyChainActivity : BaseComponentActivity() {

    private val editGuid by lazy { intent.getStringExtra("guid").orEmpty() }
    private val isRunning by lazy {
        intent.getBooleanExtra("isRunning", false)
                && editGuid.isNotEmpty()
                && editGuid == MmkvManager.getSelectServer()
    }
    private val subscriptionId by lazy { intent.getStringExtra("subscriptionId") }

    private lateinit var allRemarks: List<String>
    private lateinit var initialRemarks: String
    private lateinit var initialMembers: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        allRemarks = SettingsManager.getProfileRemarks(
            excludeConfigTypes = setOf(EConfigType.CUSTOM, EConfigType.POLICYGROUP, EConfigType.PROXYCHAIN)
        )
        val config = MmkvManager.decodeServerConfig(editGuid)
        initialRemarks = config?.remarks ?: ""
        initialMembers = config?.proxyChainProfiles?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf("", "")
    }

    @Composable
    override fun ScreenContent() {
        ProxyChainScreen(
            editGuid = editGuid,
            isRunning = isRunning,
            initialRemarks = initialRemarks,
            initialMembers = initialMembers,
            allRemarks = allRemarks,
            onBackClick = { finish() },
            onSave = { remarks, members -> saveServer(remarks, members) },
            onDelete = { deleteServer() }
        )
    }

    private fun saveServer(
        remarks: String,
        members: List<String>
    ): Boolean {
        if (remarks.isBlank()) {
            toast(R.string.server_lab_remarks)
            return false
        }

        val chainMembers = members
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (chainMembers.size != members.size) {
            toast(R.string.server_proxy_chain_members_unselected)
            return false
        }

        if (chainMembers.size < 2) {
            toast(R.string.server_proxy_chain_members_insufficient)
            return false
        }

        val invalidMembers = chainMembers.filter { member ->
            val profile = SettingsManager.getServerViaRemarks(member)
            profile == null || profile.configType.isComplexType()
        }

        if (invalidMembers.isNotEmpty()) {
            toast(
                getString(
                    R.string.server_proxy_chain_members_invalid,
                    invalidMembers.joinToString(", ")
                )
            )
            return false
        }

        val config =
            MmkvManager.decodeServerConfig(editGuid)
                ?: ProfileItem.create(EConfigType.PROXYCHAIN)

        config.remarks = remarks.trim()
        config.proxyChainProfiles =
            chainMembers.joinToString(",")

        config.description =
            chainMembers.joinToString(" -> ")

        if (
            config.subscriptionId.isEmpty() &&
            !subscriptionId.isNullOrEmpty()
        ) {
            config.subscriptionId = subscriptionId.orEmpty()
        }

        val savedGuid = MmkvManager.encodeServerConfig(
            editGuid,
            config
        )

        toastSuccess(R.string.toast_success)

        ProfileEditorResult.run {
            finishSaved(
                guid = savedGuid,
                restartService = isRunning
            )
        }

        return true
    }

    private fun deleteServer(): Boolean {
        if (editGuid.isEmpty()) {
            return false
        }

        if (editGuid == MmkvManager.getSelectServer()) {
            toast(R.string.toast_action_not_allowed)
            return false
        }

        MmkvManager.removeServer(editGuid)

        ProfileEditorResult.run {
            finishDeleted(editGuid)
        }

        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyChainScreen(
    editGuid: String,
    isRunning: Boolean,
    initialRemarks: String,
    initialMembers: List<String>,
    allRemarks: List<String>,
    onBackClick: () -> Unit,
    onSave: (String, List<String>) -> Boolean,
    onDelete: () -> Unit
) {
    var remarks by rememberSaveable { mutableStateOf(initialRemarks) }
    var members by rememberSaveable { mutableStateOf(initialMembers) }
    var memberKeys by rememberSaveable { mutableStateOf(List(initialMembers.size) { UUID.randomUUID().toString() }) }
    var showProfileDeleteConfirm by remember { mutableStateOf(false) }
    var memberToDeleteKey by rememberSaveable { mutableStateOf<String?>(null) }
    val showDelete = editGuid.isNotEmpty() && !isRunning

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = memberKeys.indexOf(from.key)
        val toIndex = memberKeys.indexOf(to.key)
        if (fromIndex != -1 && toIndex != -1) {
            val reordered = members.toMutableList()
            val reorderedKeys = memberKeys.toMutableList()
            if (reordered.moveItem(fromIndex, toIndex)) {
                reorderedKeys.moveItem(fromIndex, toIndex)
                members = reordered
                memberKeys = reorderedKeys
            }
        }
    }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(EConfigType.PROXYCHAIN.toString(), modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    FilledTonalIconButton(shapes = MorphIconButtonShapes, onClick = onBackClick) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.acc_back)
                        )
                    }
                },
                actions = {
                    if (showDelete) {
                        FilledTonalIconButton(shapes = MorphIconButtonShapes, onClick = { showProfileDeleteConfirm = true }) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = stringResource(R.string.acc_delete))
                        }
                    }
                    FilledTonalIconButton(shapes = MorphIconButtonShapes, onClick = { onSave(remarks, members) }) {
                        Icon(painterResource(R.drawable.ic_fab_check), contentDescription = stringResource(R.string.acc_save))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    members = members + ""
                    memberKeys = memberKeys + UUID.randomUUID().toString()
                },
                modifier = Modifier
                    .offset(y = -20.dp)
                    .navigationBarsPadding()
            ) {
                Icon(painterResource(R.drawable.ic_add_24dp), contentDescription = stringResource(R.string.acc_add_member))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScrollbar(lazyListState),
            contentPadding = PaddingValues(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 36.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "remarks_field") {
                FormCard {
                    FormTextField(
                        label = stringResource(R.string.server_lab_remarks),
                        value = remarks,
                        onValueChange = { remarks = it }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.server_proxy_chain_members),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(items = members, key = { index, _ -> memberKeys[index] }) { index, member ->
                val memberKey = memberKeys[index]
                ReorderableItem(reorderableState, key = memberKey) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    Surface(shadowElevation = elevation) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(with(this) { reorderableDragHandle() })
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .width(10.dp)
                            )
                            FormDropdownField(
                                label = stringResource(R.string.server_lab_remarks),
                                placeholder = stringResource(R.string.server_proxy_chain_member_unselected),
                                value = member,
                                options = allRemarks,
                                onValueChange = { newVal ->
                                    members = members.toMutableList().also { it[index] = newVal }
                                },
                                editable = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(shapes = MorphIconButtonShapes, onClick = {
                                if (member.isBlank()) {
                                    val (remainingMembers, remainingKeys) = withoutProxyChainMember(members, memberKeys, memberKey)
                                    members = remainingMembers
                                    memberKeys = remainingKeys
                                } else {
                                    memberToDeleteKey = memberKey
                                }
                            }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete_24dp),
                                    contentDescription = stringResource(R.string.acc_remove)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDeleteConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_profile),
            onConfirm = { showProfileDeleteConfirm = false; onDelete() },
            onDismiss = { showProfileDeleteConfirm = false }
        )
    }
    memberToDeleteKey?.let { memberKey ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_proxy_chain_member),
            onConfirm = {
                val (remainingMembers, remainingKeys) = withoutProxyChainMember(members, memberKeys, memberKey)
                members = remainingMembers
                memberKeys = remainingKeys
                memberToDeleteKey = null
            },
            onDismiss = { memberToDeleteKey = null }
        )
    }
}
