package com.v2ray.md.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.md.dto.GroupMapItem
import com.v2ray.md.ui.compose.SingleSelectConnectedRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Hoisted here: ButtonGroup-family content is not a composable scope at the
    // call site, so per-group counts are collected where composition applies.
    // Single scrollable row for any group count: the style never changes,
    // extra groups scroll horizontally instead of overflowing into a menu.
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
        SingleSelectConnectedRow(
            options = labels,
            selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
            onSelect = onTabClick
        )
    }
}
