package com.v2ray.md.ui.checkupdate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.BuildConfig
import com.v2ray.md.R
import com.v2ray.md.core.CoreNativeManager
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.NavigationBarsSpacer
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SettingsMenuItem
import com.v2ray.md.ui.compose.SettingsSwitchItem
import com.v2ray.md.ui.compose.VersionInfoBlock
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.Utils

class CheckUpdateActivity : BaseComponentActivity() {

    private val viewModel: CheckUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.checkForUpdates()
        }
    }

    @Composable
    override fun ScreenContent() {
        CheckUpdateScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpdateScreen(
    viewModel: CheckUpdateViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val checkPreRelease by viewModel.checkPreRelease.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.update_check_for_update)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(R.string.acc_back)
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedColumn {
                item { shape ->
                    SettingsSwitchItem(
                        icon = painterResource(R.drawable.ic_source_code_24dp),
                        title = stringResource(R.string.update_check_pre_release),
                        checked = checkPreRelease,
                        onCheckedChange = { viewModel.toggleCheckPreRelease(it) },
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_check_update_24dp),
                        title = stringResource(R.string.update_check_for_update),
                        onClick = { viewModel.checkForUpdates() },
                        shape = shape
                    )
                }
            }
            VersionInfoBlock(versionText = versionText)
            NavigationBarsSpacer()
        }
    }

    if (showUpdateDialog && updateResult != null) {
        val result = updateResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { Text(stringResource(R.string.update_new_version_found, result.latestVersion ?: "")) },
            text = {
                val scrollState = rememberScrollState()
                Text(
                    text = result.releaseNotes.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .verticalScrollbar(scrollState)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpdateDialog()
                    result.downloadUrl?.let { Utils.openUri(context, it) }
                }) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
