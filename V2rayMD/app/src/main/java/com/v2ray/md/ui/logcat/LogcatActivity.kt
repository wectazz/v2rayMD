@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.v2ray.md.ui.logcat

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.v2ray.md.AppConfig
import com.v2ray.md.R
import com.v2ray.md.extension.toastError
import com.v2ray.md.ui.base.BaseComponentActivity
import com.v2ray.md.ui.compose.ItemDivider
import com.v2ray.md.ui.compose.MorphIconButton
import com.v2ray.md.ui.compose.MorphFilledTonalIconButton
import com.v2ray.md.ui.compose.NavigationBarsBottomPadding
import com.v2ray.md.ui.compose.SearchInputField
import com.v2ray.md.ui.compose.verticalScrollbar
import com.v2ray.md.util.LogUtil
import com.v2ray.md.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogcatActivity : BaseComponentActivity() {
    private val viewModel: LogcatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        LogcatScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onShareLogcat = { shareLogcat() }
        )
    }

    private fun shareLogcat() {
        lifecycleScope.launch(Dispatchers.IO) {
            val logText = viewModel.filteredLogs.value.joinToString("\n")

            val result = try {
                val shareDir = File(cacheDir, "shared_logs").apply {
                    mkdirs()
                }

                shareDir.listFiles()?.forEach { it.delete() }

                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val logFile = File(shareDir, "v2rayMD_logcat_$timestamp.txt")
                logFile.writeText(logText, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    this@LogcatActivity,
                    "${packageName}.cache",
                    logFile
                )

                uri to logFile.name
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to share Logcat", e)
                withContext(Dispatchers.Main) {
                    toastError(R.string.toast_failure)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, result.first)
                    putExtra(Intent.EXTRA_SUBJECT, result.second)
                    putExtra(Intent.EXTRA_TITLE, result.second)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(contentResolver, result.second, result.first)
                }

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.logcat_share)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogcatScreen(
    viewModel: LogcatViewModel,
    onBackClick: () -> Unit,
    onShareLogcat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by viewModel.logEntries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                                onQueryChange = {
                                    searchQuery = it
                                    viewModel.filter(it)
                                },
                                placeholder = stringResource(R.string.menu_item_search)
                            )
                        } else {
                            Text(stringResource(R.string.title_logcat), modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    navigationIcon = {
                        if (showSearch) {
                            MorphFilledTonalIconButton( onClick = {
                                searchQuery = ""
                                viewModel.filter("")
                                showSearch = false
                            }, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_back_24dp),
                                    contentDescription = stringResource(R.string.acc_back)
                                )
                            }
                        } else {
                            MorphFilledTonalIconButton( onClick = onBackClick) {
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
                        MorphFilledTonalIconButton( onClick = { viewModel.copyLogcat() }) {
                            Icon(
                                painterResource(R.drawable.ic_copy),
                                contentDescription = stringResource(R.string.acc_copy_log)
                            )
                        }
                        MorphFilledTonalIconButton( onClick = { onShareLogcat() }) {
                            Icon(
                                painterResource(R.drawable.ic_share_24dp),
                                contentDescription = stringResource(R.string.acc_share_log)
                            )
                        }
                        MorphFilledTonalIconButton( onClick = {
                            scope.launch(Dispatchers.IO) { viewModel.clearLogcat() }
                        }, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(
                                painterResource(R.drawable.ic_delete_24dp),
                                contentDescription = stringResource(R.string.acc_clear_log)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadLogcat() },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    painterResource(R.drawable.ic_restore_24dp),
                    contentDescription = stringResource(R.string.acc_refresh)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = NavigationBarsBottomPadding()
            ) {
                items(items = logs, key = { it.key }) { log ->
                    LogcatItem(log = log.text, onLongClick = { Utils.setClipboard(context, log.text) })
                    ItemDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogcatItem(log: String, onLongClick: () -> Unit) {
    val (tag, content) = if (log.isEmpty()) {
        "" to ""
    } else {
        val parts = log.split("):", limit = 2)
        val tagPart = parts.first().split("(", limit = 2).first().trim()
        val contentPart = if (parts.size > 1) parts.last().trim() else ""
        tagPart to contentPart
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(8.dp)
    ) {
        Text(text = tag, style = MaterialTheme.typography.bodySmall)
        if (content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = content, style = MaterialTheme.typography.bodySmall)
        }
    }
}
