@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.v2ray.md.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.R
import com.v2ray.md.dto.AppInfo
import com.v2ray.md.extension.toastSuccess
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.AppDivider
import com.v2ray.md.ui.compose.AppDropdownMenuItems
import com.v2ray.md.ui.compose.lazySegmentColumn
import com.v2ray.md.ui.compose.AppListItem
import com.v2ray.md.ui.compose.ConfirmDialog
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.SearchInputField
import com.v2ray.md.ui.compose.SwitchCheckThumb
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SelectAll(R.string.menu_item_select_all),
    InvertSelection(R.string.menu_item_invert_selection),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
        val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
        val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            blacklist = blacklist,
            perAppProxyEnabled = perAppProxyEnabled,
            bypassApps = bypassApps,
            onBackClick = { finish() },
            onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
            onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
            onToggleApp = { viewModel.toggle(it) },
            onSearch = { viewModel.filterApps(it) },
            onSelectAll = { viewModel.selectAll() },
            onInvertSelection = { viewModel.invertSelection() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    val onInfoClick = { showInfoDialog = true }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                LargeFlexibleTopAppBar(
                    title = {
                        if (showSearch) {
                            SearchInputField(
                                query = searchQuery,
                                onQueryChange = { query ->
                                    searchQuery = query
                                    onSearch(query)
                                },
                                placeholder = stringResource(R.string.menu_item_search)
                            )
                        } else {
                            Text(stringResource(R.string.per_app_proxy_settings), modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    navigationIcon = {
                        if (showSearch) {
                            MorphFilledTonalIconButton( onClick = {
                                searchQuery = ""
                                onSearch("")
                                showSearch = false
                            }, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_back_24dp),
                                    contentDescription = stringResource(R.string.acc_back)
                                )
                            }
                        } else {
                            MorphFilledTonalIconButton( onClick = onBackClick, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_back_24dp),
                                    contentDescription = stringResource(R.string.acc_back)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            MorphFilledTonalIconButton( onClick = { showSearch = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_search_24dp),
                                    contentDescription = stringResource(R.string.acc_search)
                                )
                            }
                        }
                        Box {
                            MorphFilledTonalIconButton( onClick = { showMenu = true }, modifier = Modifier.padding(end = 8.dp)) {
                                Icon(
                                    painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = stringResource(R.string.acc_more)
                                )
                            }
                            com.v2ray.md.ui.compose.AppBottomSheetMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                items = PerAppMenuAction.entries,
                                labelRes = { it.labelRes },
                                onSelected = { action ->
                                    showMenu = false
                                    when (action) {
                                        PerAppMenuAction.SelectAll -> onSelectAll()
                                        PerAppMenuAction.InvertSelection -> onInvertSelection()
                                        PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                        PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                        PerAppMenuAction.ExportSelection -> onExportProxyApp()
                                    }
                                }
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MultiChoiceSegmentedButtonRow(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        SegmentedButton(
                            checked = perAppProxyEnabled,
                            onCheckedChange = onPerAppProxyChanged,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text(stringResource(R.string.per_app_proxy_settings_enable))
                        }
                        SegmentedButton(
                            checked = bypassApps,
                            onCheckedChange = onBypassAppsChanged,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(stringResource(R.string.switch_bypass_apps_mode))
                        }
                    }
                    MorphIconButton( onClick = onInfoClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_about_24dp),
                            contentDescription = stringResource(R.string.acc_per_app_proxy_information),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            AppDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                )
            ) {
                lazySegmentColumn(
                    items = apps,
                    key = { _, it -> it.packageName },
                    noHorizontalPadding = false
                ) { _, app ->
                    val checked = blacklist.contains(app.packageName)
                    AppListItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = null,
                        checked = checked,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        ConfirmDialog(
            message = stringResource(R.string.summary_pref_per_app_proxy),
            dismissText = null,
            onConfirm = {},
            onDismiss = { showInfoDialog = false },
        )
    }
}
