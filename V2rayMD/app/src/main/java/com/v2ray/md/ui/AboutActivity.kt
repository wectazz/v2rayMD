package com.v2ray.md.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.v2ray.md.AppConfig
import com.v2ray.md.BuildConfig
import com.v2ray.md.R
import com.v2ray.md.core.CoreNativeManager
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.NavigationBarsSpacer
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.SettingsMenuItem
import com.v2ray.md.ui.compose.VersionInfoBlock
import com.v2ray.md.util.Utils

class AboutActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            onBackClick = { finish() },
            onTranslatorsClick = {
                startActivity(Intent(this, TranslatorsActivity::class.java))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onTranslatorsClick: () -> Unit
) {
    val context = LocalContext.current
    var showOssDialog by remember { mutableStateOf(false) }

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"
    val appIdText = BuildConfig.APPLICATION_ID
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_about), modifier = Modifier.padding(start = 8.dp)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.acc_back)
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
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedColumn {
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_source_code_24dp),
                        title = stringResource(R.string.title_source_code),
                        onClick = { Utils.openUri(context, AppConfig.APP_URL) },
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.license_24px),
                        title = stringResource(R.string.title_oss_license),
                        onClick = { showOssDialog = true },
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_translate_24dp),
                        title = stringResource(R.string.title_translators),
                        onClick = onTranslatorsClick,
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_feedback_24dp),
                        title = stringResource(R.string.title_pref_feedback),
                        onClick = { Utils.openUri(context, AppConfig.APP_ISSUES_URL) },
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_telegram_24dp),
                        title = stringResource(R.string.title_tg_channel),
                        onClick = { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) },
                        shape = shape
                    )
                }
                item { shape ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_privacy_24dp),
                        title = stringResource(R.string.title_privacy_policy),
                        onClick = { Utils.openUri(context, AppConfig.APP_PRIVACY_POLICY) },
                        shape = shape
                    )
                }
            }
            VersionInfoBlock(
                versionText = versionText,
                appIdText = appIdText
            )
            NavigationBarsSpacer()
        }
    }

    if (showOssDialog) {
        AlertDialog(
            onDismissRequest = { showOssDialog = false },
            title = { Text(stringResource(R.string.title_oss_license), modifier = Modifier.padding(start = 8.dp)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadUrl("file:///android_asset/open_source_licenses.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showOssDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}
