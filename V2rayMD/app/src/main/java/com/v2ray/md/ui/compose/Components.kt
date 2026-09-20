package com.v2ray.md.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.v2ray.md.R
import com.v2ray.md.util.AppIconFetcher
import sh.calvin.reorderable.ReorderableCollectionItemScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    title: String,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    searchPlaceholder: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                if (isSearchActive) {
                    SearchInputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = searchPlaceholder
                    )
                } else {
                    Text(text = title)
                }
            },
            navigationIcon = {
                if (navigationIcon != null) {
                    navigationIcon()
                } else {
                    MorphIconButton(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = if (isSearchActive) onSearchClose else onBackClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.acc_back)
                        )
                    }
                }
            },
            actions = actions
        )
        AnimatedVisibility(
            visible = isLoading,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String?
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            placeholder = { if (placeholder != null) Text(placeholder, style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        if (query.isNotEmpty()) {
            MorphIconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), stringResource(R.string.logcat_clear))
            }
        }
    }
}

@Composable
fun AppListItem(
    appName: String,
    packageName: String,
    icon: Any?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LocalSegmentedItemShape.current,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val model = remember(icon, packageName) {
                if (icon != null) {
                    icon
                } else {
                    val data = "appicon:$packageName"
                    ImageRequest.Builder(context)
                        .data(data)
                        .fetcherFactory(AppIconFetcher.Factory(context))
                        .build()
                }
            }

            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_image_24dp),
                fallback = painterResource(R.drawable.ic_image_24dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun ItemDivider() {
    AppDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    val color = if (LocalDarkTheme.current) dividerColorDark else dividerColorLight
    HorizontalDivider(modifier = modifier.fillMaxWidth(), thickness = 1.dp, color = color)
}

@Composable
fun NavigationBarsSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
fun NavigationBarsBottomPadding(): PaddingValues {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(bottom = bottom)
}

@Composable
fun VersionInfoBlock(
    versionText: String,
    appIdText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = versionText, style = MaterialTheme.typography.bodySmall)
        if (appIdText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = appIdText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun reorderableElevation(isDragging: Boolean) = animateDpAsState(
    targetValue = if (isDragging) 4.dp else 0.dp,
    label = "ReorderableElevation"
)

@Composable
fun ReorderableCollectionItemScope.reorderableDragHandle(): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    return Modifier.longPressDraggableHandle(
        onDragStarted = {
            // Platform haptics honor the user's touch-feedback setting.
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    )
}

@Composable
fun ReorderableListItem(
    scope: ReorderableCollectionItemScope,
    isDragging: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val elevation by reorderableElevation(isDragging)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = elevation,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(with(scope) { reorderableDragHandle() }),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun ReorderableGridItem(
    scope: ReorderableCollectionItemScope,
    isDragging: Boolean,
    content: @Composable () -> Unit
) {
    val elevation by reorderableElevation(isDragging)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(with(scope) { reorderableDragHandle() }),
        shadowElevation = elevation,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        content()
    }
}

@Composable
fun MorphIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 1000f),
        label = "radius"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        ) {
            content()
        }
    }
}

@Composable
fun MorphFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 1000f),
        label = "radius"
    )

    val containerColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}

/**
 * Single-select connected toggle row from the ButtonGroup family.
 * Unlike ButtonGroup itself, a pressed item expands WITHOUT compressing its
 * neighbours: siblings keep their width and slide aside (the parent is
 * expected to scroll when the row overflows). Wrap in a horizontalScroll Row
 * when the count can exceed the width.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SingleSelectButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSources = remember(options.size) {
        List(options.size) { MutableInteractionSource() }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val pressed by interactionSources[index].collectIsPressedAsState()
            // Width gained only by the pressed item; neighbours stay rigid.
            val pressExpand by animateDpAsState(
                targetValue = if (pressed) 8.dp else 0.dp,
                label = "press-expand"
            )
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(index) },
                shapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size),
                interactionSource = interactionSources[index]
            ) {
                Spacer(Modifier.size(pressExpand))
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_done),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_file_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                }
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(pressExpand))
            }
        }
    }
}
