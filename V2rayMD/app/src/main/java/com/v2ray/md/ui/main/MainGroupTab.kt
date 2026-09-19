package com.v2ray.md.ui.main

import androidx.compose.foundation.background
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
import com.v2ray.md.ui.compose.SingleSelectButtonGroup

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
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedTabIndex.coerceIn(0, groups.lastIndex)
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { onTabClick(index) },
                    label = { androidx.compose.material3.Text(label) },
                    leadingIcon = if (selected) {
                        { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(com.v2ray.md.R.drawable.ic_action_done), contentDescription = null) }
                    } else {
                        { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(com.v2ray.md.R.drawable.ic_file_24dp), contentDescription = null) }
                    },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                        labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        iconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = null
                )
            }
        }
    }
}
