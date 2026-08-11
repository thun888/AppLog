package top.hzchu.applog.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.hzchu.applog.R
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.ui.components.AppItem
import top.hzchu.applog.utils.ApkHelper
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    onAppClick: (AppInfo, AppInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val showSystem by viewModel.showSystemApps.collectAsState()
    val showUser by viewModel.showUserApps.collectAsState()
    val extractProgress by viewModel.extractingProgress.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

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
                title = { Text(stringResource(R.string.tab_apps)) },
                actions = {
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.prepareCommit() },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.commit))
            }
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
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                    items(apps, key = { it.packageName }) { app ->
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
    }
}
