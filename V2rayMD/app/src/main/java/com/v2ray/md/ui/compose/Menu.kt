package com.v2ray.md.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    onSelected: (T) -> Unit
) {
    if (expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            items.forEach { item ->
                ListItem(
                    headlineContent = { Text(stringResource(labelRes(item))) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismissRequest()
                                    onSelected(item)
                                }
                            }
                        }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
