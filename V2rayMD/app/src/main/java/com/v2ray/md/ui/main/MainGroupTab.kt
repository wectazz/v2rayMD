package com.v2ray.md.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.dto.GroupMapItem
import com.v2ray.md.dto.entities.ServersCache
import kotlinx.coroutines.flow.StateFlow

private const val MAX_SEGMENTED_GROUPS = 4

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex)
    if (groups.size <= MAX_SEGMENTED_GROUPS) {
        SingleChoiceSegmentedButtonRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            groups.forEachIndexed { index, group ->
                val serverFlow = remember(group.id, mainViewModel) {
                    mainViewModel.serversForGroup(group.id)
                }
                val servers by serverFlow.collectAsStateWithLifecycle()
                val text = if (group.id.isEmpty()) {
                    group.remarks
                } else {
                    "${group.remarks} (${servers.size})"
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = groups.size),
                    onClick = { onTabClick(index) },
                    selected = index == selectedIndex,
                    label = {
                        Text(
                            text = text,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    } else {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier.fillMaxWidth(),
            edgePadding = 16.dp
        ) {
            groups.forEachIndexed { index, group ->
                val serverFlow = remember(group.id, mainViewModel) {
                    mainViewModel.serversForGroup(group.id)
                }
                GroupTabItem(
                    group = group,
                    selected = index == selectedIndex,
                    serverFlow = serverFlow,
                    onClick = { onTabClick(index) }
                )
            }
        }
    }
}

@Composable
private fun GroupTabItem(
    group: GroupMapItem,
    selected: Boolean,
    serverFlow: StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val servers by serverFlow.collectAsStateWithLifecycle()
    val text = if (group.id.isEmpty()) {
        group.remarks
    } else {
        "${group.remarks} (${servers.size})"
    }

    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 48.dp),
        text = {
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
