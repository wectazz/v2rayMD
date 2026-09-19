package com.v2ray.md.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.md.R
import com.v2ray.md.ui.compose.AppDivider
import com.v2ray.md.ui.compose.LocalDarkTheme
import com.v2ray.md.ui.compose.SegmentedColumn
import com.v2ray.md.ui.compose.verticalScrollbar

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private val primaryDrawerItems = listOf(
    MainDestination.Subscriptions,
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    
    MainDestination.Settings
)

private val secondaryDrawerItems = listOf(
    
    MainDestination.Logcat,
    MainDestination.CheckUpdate,
    MainDestination.BackupRestore,
    MainDestination.About
)

@Composable
private fun DrawerSegmentedItem(
    item: MainDestination,
    index: Int,
    count: Int,
    subscriptionCount: Int,
    onNavigate: (MainDestination) -> Unit
) {
    // Explicit container: the default segmented container is near-invisible
    // against the drawer sheet in dark theme (same as settings rows).
    val colors = ListItemDefaults.segmentedColors(
        containerColor = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainerHigh
    )
    SegmentedListItem(
        onClick = { onNavigate(item) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        com.v2ray.md.ui.compose.ScallopedShape(points = 16, depth = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        trailingContent = if (item == MainDestination.Subscriptions && subscriptionCount > 0) {            {
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(subscriptionCount.toString())
                }
            }
        } else {
            null
        },
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(item.labelRes))
    }
}

@Composable
fun MainMenuSheetContent(
    subscriptionCount: Int = 0,
    onNavigate: (MainDestination) -> Unit
) {
    val drawerScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(drawerScrollState)
            .verticalScrollbar(drawerScrollState)
    ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isDarkTheme = LocalDarkTheme.current
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        colorFilter = if (isDarkTheme) {
                            ColorFilter.tint(Color.White, BlendMode.SrcIn)
                        } else {
                            null
                        }
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                com.v2ray.md.ui.compose.ScallopedShape(points = 16, depth = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                      text = stringResource(R.string.title_app_settings),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.primary
                  )
              }
            SegmentedColumn {
                primaryDrawerItems.forEachIndexed { index, item ->
                    item(key = item) {
                        DrawerSegmentedItem(
                            item = item,
                            index = index,
                            count = primaryDrawerItems.size,
                            subscriptionCount = subscriptionCount,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                com.v2ray.md.ui.compose.ScallopedShape(points = 16, depth = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_about_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                      text = stringResource(R.string.title_tools_and_info),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.primary
                  )
            }
            SegmentedColumn {
                secondaryDrawerItems.forEachIndexed { index, item ->
                    item(key = item) {
                        DrawerSegmentedItem(
                            item = item,
                            index = index,
                            count = secondaryDrawerItems.size,
                            subscriptionCount = 0,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
