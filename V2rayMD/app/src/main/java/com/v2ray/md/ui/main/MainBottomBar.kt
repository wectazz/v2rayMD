package com.v2ray.md.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.v2ray.md.R
import com.v2ray.md.ui.compose.AppDivider
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainCombinedBottomBar(
    isRunning: Boolean,
    displayText: String,
    onAction: (MainAction) -> Unit
) {
    val configuration = LocalConfiguration.current
    val fullWidth = configuration.screenWidthDp.dp - 32.dp // 16dp horizontal padding on each side
    
    val fabWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isRunning) 72.dp else fullWidth,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "fabWidth"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isRunning,
            modifier = Modifier.weight(1f),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable(onClick = { onAction(MainAction.TestCurrentServer) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
        
        val spacerWidth by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isRunning) 16.dp else 0.dp,
            label = "spacerWidth"
        )
        Spacer(modifier = Modifier.width(spacerWidth))

        MainConnectFab(
            isRunning = isRunning,
            onAction = onAction,
            modifier = Modifier.width(fabWidth).height(72.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainConnectFab(
    isRunning: Boolean,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isRunning) 20.dp else 36.dp, // 36dp for circle (72dp size)
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "fabShape"
    )

    androidx.compose.material3.FilledIconToggleButton(
        checked = isRunning,
        onCheckedChange = { onAction(MainAction.ToggleService) },
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        Icon(
            painter = if (isRunning) painterResource(R.drawable.ic_stop_24dp)
            else painterResource(R.drawable.ic_play_24dp),
            contentDescription = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start),
            modifier = Modifier.size(36.dp)
        )
    }
}
