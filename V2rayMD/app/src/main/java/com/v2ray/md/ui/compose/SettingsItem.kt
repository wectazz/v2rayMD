package com.v2ray.md.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.md.R

/**
 * M3 expressive settings group: stack of single segmented list items with the
 * spec [ListItemDefaults.SegmentedGap] spacing. Each row draws its own container.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content
    )
}

/** Check mark for switch thumbs, following the M3 selected-switch pattern. */
@Composable
fun SwitchCheckThumb(checked: Boolean) {
    Icon(
        painter = painterResource(
            if (checked) R.drawable.ic_action_done
            else android.R.drawable.ic_menu_close_clear_cancel
        ),
        contentDescription = null,
        modifier = Modifier.size(SwitchDefaults.IconSize)
    )
}

@Composable
fun PreferenceGroupHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun CollapsiblePreferenceGroupHeader(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_expand_more_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .rotate(if (expanded) 180f else 0f)
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: Painter?,
    title: String,
    description: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    // Explicit container: the default segmented container is near-invisible
    // against the background in dark theme. Highest in dark, High in light
    // keeps blocks clearly tonal on both.
    val colors = ListItemDefaults.segmentedColors(
        containerColor = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val leadingContent: @Composable (() -> Unit)? = if (icon != null) {
        {
            Icon(
                painter = icon,
                contentDescription = null
            )
        }
    } else {
        null
    }
    val supportingContent: @Composable (() -> Unit)? = if (!description.isNullOrEmpty()) {
        { Text(text = description) }
    } else {
        null
    }
    if (onClick != null) {
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailing,
            supportingContent = supportingContent,
            colors = colors,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(text = title)
        }
    } else {
        SegmentedListItem(
            shapes = shapes,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailing,
            supportingContent = supportingContent,
            colors = colors,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(text = title)
        }
    }
}

@Composable
private fun TrailingValueText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SettingsEditItem(
    icon: Painter? = null,
    title: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardNumber: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    val description = if (isPassword) {
        if (value.isEmpty()) null else "******"
    } else {
        value.ifEmpty { null }
    }

    SettingsItemRow(
        icon = icon,
        title = title,
        description = null,
        enabled = enabled,
        onClick = if (enabled) {
            { showDialog = true }
        } else null,
        modifier = modifier,
        trailing = description?.let { value ->
            { TrailingValueText(text = value) }
        }
    )

    if (showDialog) {
        var text by remember { mutableStateOf(value) }
        InputDialog(
            title = title,
            fields = listOf(
                InputField(
                    label = title,
                    value = text,
                    visualTransformation = VisualTransformation.None
                )
            ),
            onFieldChange = { _, v -> text = v },
            confirmText = stringResource(R.string.action_ok),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = { showDialog = false; onValueChanged(text) },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun SettingsListItem(
    icon: Painter? = null,
    title: String,
    entries: List<String>,
    values: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val options = entries.zip(values)
    val selectedOption = options.find { it.second == selectedValue } ?: options.firstOrNull()
    val summary = selectedOption?.first.orEmpty()

    SettingsItemRow(
        icon = icon,
        title = title,
        description = null,
        enabled = enabled,
        onClick = if (enabled) {
            { showDialog = true }
        } else null,
        modifier = modifier,
        trailing = summary.takeIf { it.isNotEmpty() }?.let { value ->
            { TrailingValueText(text = value) }
        }
    )

    if (showDialog) {
        SelectListDialog(
            title = title,
            options = options,
            optionText = { it.first },
            selectedOption = selectedOption,
            onSelected = { option ->
                showDialog = false
                onSelected(option.second)
            },
            onDismiss = { showDialog = false },
            showRadio = true
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: Painter? = null,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    SettingsItemRow(
        icon = icon,
        title = title,
        description = subtitle,
        enabled = true,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun SettingsSwitchItem(
    icon: Painter? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingsItemRow(
        icon = icon,
        title = title,
        description = summary,
        enabled = enabled,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else null,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                thumbContent = { SwitchCheckThumb(checked) },
                enabled = enabled
            )
        }
    )
}
