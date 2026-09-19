package com.v2ray.md.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <T> AppDropdownMenuItems(
    items: List<T>,
    labelRes: (T) -> Int,
    onSelected: (T) -> Unit
) {
    items.forEach { item ->
        DropdownMenuItem(
            text = { Text(stringResource(labelRes(item))) },
            onClick = { onSelected(item) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppBottomSheetMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<T>,
    labelRes: (T) -> Int,
    iconRes: ((T) -> Int)? = null,
    iconVector: ((T) -> ImageVector)? = null,
    onSelected: (T) -> Unit
) {
    if (expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            SegmentedColumn {
                items.forEach { item ->
                    item { shape ->
                        SettingsMenuItem(
                            icon = iconVector?.invoke(item)?.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
                                ?: iconRes?.invoke(item)?.let { painterResource(it) },
                            title = stringResource(labelRes(item)),
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        onDismissRequest()
                                        onSelected(item)
                                    }
                                }
                            },
                            shape = shape
                        )
                    }
                }
            }
        }
    }
}

interface BottomSheetGroup {
    val titleRes: Int
    val iconVector: ImageVector? get() = null
    val iconRes: Int? get() = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T, G : BottomSheetGroup> GroupedAppBottomSheetMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<T>,
    groupBy: (T) -> G,
    labelRes: (T) -> Int,
    iconRes: ((T) -> Int)? = null,
    iconVector: ((T) -> ImageVector)? = null,
    onSelected: (T) -> Unit
) {
    if (expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val groupedItems = items.groupBy(groupBy)
            val scrollState = androidx.compose.foundation.rememberScrollState()
            
            Column(modifier = Modifier) {
                groupedItems.forEach { (group, groupItems) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val p = group.iconVector?.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
                            ?: group.iconRes?.let { painterResource(it) }
                        
                        if (p != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, ScallopedShape(points = 10, depth = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = p,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                    }
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                SegmentedColumn {
                    groupItems.forEach { item ->
                        item { shape ->
                            SettingsMenuItem(
                                icon = iconVector?.invoke(item)?.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
                                    ?: iconRes?.invoke(item)?.let { painterResource(it) },
                                title = stringResource(labelRes(item)),
                                onClick = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            onDismissRequest()
                                            onSelected(item)
                                        }
                                    }
                                },
                                shape = shape
                            )
                        }
                    }
                }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
