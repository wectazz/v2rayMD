package com.v2ray.md.ui.compose

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> AppBottomSheetMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<T>,
    labelRes: (T) -> Int,
    onSelected: (T) -> Unit
) {
    if (expanded) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            windowInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars
        ) {
            items.forEach { item ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(labelRes(item))) },
                    modifier = androidx.compose.ui.Modifier
                        .androidx.compose.foundation.layout.fillMaxWidth()
                        .androidx.compose.foundation.clickable {
                            onSelected(item)
                            onDismissRequest()
                        }
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.height(16.dp))
        }
    }
}
