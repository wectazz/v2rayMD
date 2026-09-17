package com.v2ray.md.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    // Single scrollable row for any group count: the style never changes,
    // extra groups scroll horizontally instead of switching to tabs.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SingleChoiceSegmentedButtonRow {
            groups.forEachIndexed { index, group ->
                // Hoisted here (not in a separate function): SegmentedButton is a
                // scope extension and only resolves inside the row content.
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
    }
}
