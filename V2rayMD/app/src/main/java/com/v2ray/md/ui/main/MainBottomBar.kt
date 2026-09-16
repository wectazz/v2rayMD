package com.v2ray.md.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.v2ray.md.R
import com.v2ray.md.ui.compose.AppDivider

@Composable
fun MainStatusBar(
    displayText: String,
    onAction: (MainAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = { onAction(MainAction.TestCurrentServer) })
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AppDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = displayText
                }
            )
        }
    }
}

@Composable
fun MainConnectFab(
    isRunning: Boolean,
    onAction: (MainAction) -> Unit
) {
    FloatingActionButton(
        onClick = { onAction(MainAction.ToggleService) }
    ) {
        Icon(
            painter = if (isRunning) painterResource(R.drawable.ic_stop_24dp)
            else painterResource(R.drawable.ic_play_24dp),
            contentDescription = stringResource(
                if (isRunning) R.string.acc_stop else R.string.acc_start
            )
        )
    }
}
