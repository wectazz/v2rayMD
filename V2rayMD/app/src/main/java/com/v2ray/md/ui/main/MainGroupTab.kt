package com.v2ray.md.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.dto.GroupMapItem

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex)
    // Counts are hoisted: ButtonGroup content is a non-composable registration DSL,
    // so all composition (remember/collect) happens here.
    val labels = groups.map { group ->
        val serverFlow = remember(group.id, mainViewModel) {
            mainViewModel.serversForGroup(group.id)
        }
        val servers by serverFlow.collectAsStateWithLifecycle()
        if (group.id.isEmpty()) {
            group.remarks
        } else {
            "${group.remarks} (${servers.size})"
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ButtonGroup(
            overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
        ) {
            labels.forEachIndexed { index, text ->
                toggleableItem(
                    checked = index == selectedIndex,
                    label = text,
                    onCheckedChange = { onTabClick(index) },
                )
            }
        }
    }
}
