package com.v2ray.md.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.v2ray.md.dto.ProfileDiffEntry
import com.v2ray.md.dto.SubscriptionUpdateResult
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.md.R

@Composable
fun ConfirmDialog(
    title: String? = null,
    message: String,
    confirmText: String = stringResource(R.string.action_ok),
    dismissText: String? = stringResource(R.string.action_cancel),
    confirmIcon: @Composable (() -> Unit)? = null,
    messageTextAlign: TextAlign = TextAlign.Start,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }

    LaunchedEffect(dismissText) {
        if (dismissText != null) dismissFocusRequester.requestFocus()
        else confirmFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let {
            {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = messageTextAlign
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                modifier = Modifier.focusRequester(confirmFocusRequester)
            ) {
                confirmIcon?.invoke()
                if (confirmIcon != null) Spacer(Modifier.width(8.dp))
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let { text ->
            {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(dismissFocusRequester)
                ) {
                    Text(text)
                }
            }
        },
    )
}

@Composable
fun DeleteConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemName: String? = null,
) {
    ConfirmDialog(
        message = if (itemName.isNullOrBlank()) message else "$message\n\n$itemName",
        messageTextAlign = TextAlign.Center,
        confirmText = stringResource(R.string.action_delete),
        confirmIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

data class InputField(
    val label: String,
    val value: String,
    val singleLine: Boolean = true,
    val visualTransformation: VisualTransformation = VisualTransformation.None
)

@Composable
fun InputDialog(
    title: String,
    fields: List<InputField>,
    onFieldChange: (Int, String) -> Unit,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { onFieldChange(index, it) },
                        label = { Text(field.label) },
                        singleLine = false,
                        maxLines = 5,
                        visualTransformation = field.visualTransformation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
    )
}

@Composable
fun QRCodeDialog(
    bitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    if (bitmap == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.acc_qr_code),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/**
 * When showRadio is true, displays RadioButton (single selection mode);
 * otherwise, plain clickable list mode.
 * The selectedOption parameter is used to highlight the selected item only when showRadio is true.
 *
 * The options render as a segmented column, matching the Settings screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectListDialog(
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    selectedOption: T? = null,
    showRadio: Boolean = false
) {
    // BasicAlertDialog instead of AlertDialog: the stock dialog pads content by 24dp on
    // every side, which left the list narrow. The title is centred and the insets are
    // compact here.
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
                val scrollState = rememberScrollState()
                SegmentedColumn(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .verticalScrollbar(scrollState)
                ) {
                    options.forEach { option ->
                        item { _ ->
                            val isSelected = option == selectedOption
                            // Base parameters only affect unselected rows: the selected row
                            // reads its own selected* colours, so those are set separately.
                            // Values mirror BakaSU's settings dialog: surfaceBright rows on a
                            // darker dialog, primaryContainer for the selection.
                            val colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTrailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            // The selected item keeps the regular segment outline; only the
                            // colour changes (selectedShape would add extra rounding).
                            val shapes = segmentedItemShapes().let { it.copy(selectedShape = it.shape) }
                            val label: @Composable () -> Unit = {
                                Text(
                                    text = optionText(option),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // onClick is passed to the item itself: only then does it track
                            // interactions and morph to pressedShape/focusedShape while held.
                            if (showRadio) {
                                SegmentedListItem(
                                    selected = isSelected,
                                    onClick = { onSelected(option) },
                                    shapes = shapes,
                                    colors = colors,
                                    trailingContent = {
                                        RadioButton(selected = isSelected, onClick = null)
                                    },
                                ) {
                                    label()
                                }
                            } else {
                                SegmentedListItem(
                                    onClick = { onSelected(option) },
                                    shapes = shapes,
                                    colors = colors,
                                ) {
                                    label()
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

/** Per-item shapes for a [SegmentedListItem] placed inside a [SegmentedColumn]. */
@Composable
private fun segmentedItemShapes(): ListItemShapes {
    val position = LocalSegmentedPosition.current
    return if (position != null) {
        ListItemDefaults.segmentedShapes(index = position.index, count = position.count)
    } else {
        ListItemDefaults.segmentedShapes(index = 0, count = 1)
    }
}

/**
 * Shows added/removed profiles after a subscription update, MikuRay-style.
 * Rows are prefixed with the subscription name when several updated at once.
 */
@Composable
fun SubUpdateDiffDialog(
    result: SubscriptionUpdateResult,
    onDismiss: () -> Unit
) {
    val subNames = (result.addedProfiles.map { it.subscriptionName } +
        result.deletedProfiles.map { it.subscriptionName }).distinct()
    val multipleSubs = subNames.size > 1
    fun format(entries: List<ProfileDiffEntry>): String = entries.joinToString("\n") { entry ->
        if (multipleSubs) "• [${entry.subscriptionName}] ${entry.profileName}" else "• ${entry.profileName}"
    }
    val message = buildString {
        if (result.addedProfiles.isNotEmpty()) {
            append(stringResource(R.string.sub_update_diff_added, format(result.addedProfiles)))
        }
        if (result.deletedProfiles.isNotEmpty()) {
            if (isNotEmpty()) append("\n\n")
            append(stringResource(R.string.sub_update_diff_deleted, format(result.deletedProfiles)))
        }
        if (isEmpty()) {
            append(stringResource(R.string.sub_update_diff_empty))
        }
    }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subNames.size == 1) stringResource(R.string.title_sub_update_diff, subNames.first())
                else stringResource(R.string.title_sub_update),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
