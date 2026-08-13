package top.hzchu.applog.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import top.hzchu.applog.R
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.ui.components.AlphabetIndexBar
import top.hzchu.applog.ui.components.AppItem
import top.hzchu.applog.utils.ApkHelper
import top.hzchu.applog.utils.PinyinUtils
import top.hzchu.applog.viewmodel.AppGrouping
import top.hzchu.applog.viewmodel.AppSortOrder
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    onAppClick: (AppInfo, AppInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState()
    val groupedApps by viewModel.groupedApps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val showSystem by viewModel.showSystemApps.collectAsState()
    val showUser by viewModel.showUserApps.collectAsState()
    val extractProgress by viewModel.extractingProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val currentGrouping by viewModel.grouping.collectAsState()
    val isIndexBarEnabled by viewModel.showIndexBar.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    if (extractProgress != null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.extract_apk),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { extractProgress ?: 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${((extractProgress ?: 0f) * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { 
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false 
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            }
                        )
                    } else {
                        Text(stringResource(R.string.tab_apps))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                        
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                AppSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(when(order) {
                                            AppSortOrder.NAME -> R.string.sort_name
                                            AppSortOrder.PACKAGE_NAME -> R.string.sort_package
                                            AppSortOrder.INSTALL_TIME -> R.string.sort_install_time
                                            AppSortOrder.UPDATE_TIME -> R.string.sort_update_time
                                        })) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        },
                                        trailingIcon = if (currentSort == order) {
                                            { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { showGroupMenu = true }) {
                                Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.group_by))
                            }
                            DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                                AppGrouping.entries.forEach { grouping ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(when(grouping) {
                                            AppGrouping.NONE -> R.string.group_none
                                            AppGrouping.TAGS -> R.string.group_tags
                                        })) },
                                        onClick = {
                                            viewModel.setGrouping(grouping)
                                            showGroupMenu = false
                                        },
                                        trailingIcon = if (currentGrouping == grouping) {
                                            { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.filter))
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = showSystem, onCheckedChange = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.show_system_apps))
                                        }
                                    },
                                    onClick = { viewModel.toggleSystemApps(!showSystem) }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = showUser, onCheckedChange = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.show_user_apps))
                                        }
                                    },
                                    onClick = { viewModel.toggleUserApps(!showUser) }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.scanApps() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (apps.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.tap_to_scan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                val showIndexBar = isIndexBarEnabled &&
                        currentGrouping == AppGrouping.NONE &&
                        (currentSort == AppSortOrder.NAME || currentSort == AppSortOrder.PACKAGE_NAME)

                val alphabet = remember(apps, currentSort, showIndexBar) {
                    if (!showIndexBar) emptyList()
                    else apps.map { app ->
                        val char = if (currentSort == AppSortOrder.NAME) {
                            PinyinUtils.getFirstLetter(app.appName)
                        } else {
                            val firstChar = app.packageName.firstOrNull() ?: '#'
                            if (firstChar.isLetter()) firstChar.uppercaseChar() else '#'
                        }
                        char.toString()
                    }.distinct().sortedBy { if (it == "#") "{" else it }
                }

                val indexMap = remember(apps, currentSort, showIndexBar) {
                    if (!showIndexBar) emptyMap()
                    else {
                        val map = mutableMapOf<String, Int>()
                        apps.forEachIndexed { index, app ->
                            val char = if (currentSort == AppSortOrder.NAME) {
                                PinyinUtils.getFirstLetter(app.appName)
                            } else {
                                val firstChar = app.packageName.firstOrNull() ?: '#'
                                if (firstChar.isLetter()) firstChar.uppercaseChar() else '#'
                            }
                            val key = char.toString()
                            if (!map.containsKey(key)) {
                                map[key] = index + 1
                            }
                        }
                        map
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = if (showIndexBar) 36.dp else 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.apps_count, apps.size),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        groupedApps.forEach { (groupName, groupApps) ->
                            if (currentGrouping != AppGrouping.NONE) {
                                stickyHeader {
                                    val isCollapsed = groupName in collapsedGroups
                                    Card(
                                        onClick = {
                                            collapsedGroups = if (isCollapsed) {
                                                collapsedGroups - groupName
                                            } else {
                                                collapsedGroups + groupName
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = groupName.ifBlank { stringResource(R.string.no_tags) },
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = if (isCollapsed) {
                                                    Icons.Filled.KeyboardArrowDown
                                                } else {
                                                    Icons.Filled.KeyboardArrowUp
                                                },
                                                contentDescription = stringResource(
                                                    if (isCollapsed) R.string.expand_group else R.string.collapse_group
                                                ),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (currentGrouping == AppGrouping.NONE || groupName !in collapsedGroups) {
                                items(groupApps, key = { "${groupName}_${it.packageName}" }) { app ->
                                    Box {
                                        AppItem(
                                            app = app,
                                            onClick = { onAppClick(app, null) },
                                            onLongClick = { menuApp = app }
                                        )

                                        DropdownMenu(
                                            expanded = menuApp == app,
                                            onDismissRequest = { menuApp = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.open_app)) },
                                                onClick = {
                                                    menuApp = null
                                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                                    if (launchIntent != null) {
                                                        context.startActivity(launchIntent)
                                                    } else {
                                                        viewModel.showToast(context.getString(R.string.app_not_openable))
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit_note)) },
                                                onClick = {
                                                    menuApp = null
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                                                    context.startActivity(intent)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.extract_apk)) },
                                                onClick = {
                                                    menuApp = null
                                                    viewModel.startExtraction(context, app.packageName, app.appName)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.share_apk)) },
                                                onClick = {
                                                    menuApp = null
                                                    ApkHelper.shareApk(context, app.packageName, app.appName)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showIndexBar && alphabet.isNotEmpty()) {
                        AlphabetIndexBar(
                            alphabet = alphabet,
                            onIndexSelected = { char ->
                                indexMap[char]?.let { index ->
                                    coroutineScope.launch {
                                        listState.scrollToItem(index)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                                .width(24.dp)
                        )
                    }
                }
            }
        }
    }
}
