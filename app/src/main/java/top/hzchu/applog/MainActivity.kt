package top.hzchu.applog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.ui.components.DiffUpdatedColor
import top.hzchu.applog.ui.components.TagChipDisplay
import top.hzchu.applog.ui.components.TagChipInput
import top.hzchu.applog.ui.navigation.NavigationTab
import top.hzchu.applog.ui.screens.AppsScreen
import top.hzchu.applog.ui.screens.BranchScreen
import top.hzchu.applog.ui.screens.CommitDetailScreen
import top.hzchu.applog.ui.screens.HistoryScreen
import top.hzchu.applog.ui.screens.SettingsScreen
import top.hzchu.applog.ui.theme.AppLogTheme
import top.hzchu.applog.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppLogTheme {
                AppLogApp()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDetailRow(
    label: String,
    value: String,
    isChanged: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(label, value)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            )
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isChanged) DiffUpdatedColor else MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
            color = if (isChanged) DiffUpdatedColor else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (onClick != null) TextDecoration.Underline else TextDecoration.None
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogApp() {
    val viewModel: MainViewModel = viewModel()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (isFirstLaunch) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            top.hzchu.applog.ui.screens.SetupScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showCommitDialog by viewModel.showCommitDialog.collectAsState()
    val pendingAutoMessage by viewModel.pendingAutoMessage.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    var commitMessageText by remember { mutableStateOf("") }
    LaunchedEffect(showCommitDialog) {
        if (showCommitDialog) {
            commitMessageText = ""
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    var editingApp by remember { mutableStateOf<AppInfo?>(null) }
    var previousAppForDiff by remember { mutableStateOf<AppInfo?>(null) }
    var isDetailEditable by remember { mutableStateOf(true) }
    var editingNoteText by remember { mutableStateOf("") }
    var editingTagsList by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val showBottomBar = currentRoute in NavigationTab.entries.map { it.name }
            if (showBottomBar) {
                NavigationBar {
                    NavigationTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.name,
                            onClick = {
                                if (currentRoute != tab.name) {
                                    navController.navigate(tab.name) {
                                        popUpTo(NavigationTab.APPS.name) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == NavigationTab.APPS.name) {
                FloatingActionButton(
                    onClick = { viewModel.prepareCommit() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.commit))
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationTab.APPS.name,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavigationTab.APPS.name) {
                AppsScreen(
                    viewModel = viewModel,
                    onAppClick = { app, prev ->
                        editingApp = app
                        previousAppForDiff = prev
                        isDetailEditable = true
                        editingNoteText = app.note
                        editingTagsList = app.tags.split(", ").filter { it.isNotEmpty() }
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable(NavigationTab.HISTORY.name) {
                HistoryScreen(
                    viewModel = viewModel,
                    onCommitClick = { id ->
                        viewModel.loadCommitDetail(id)
                        navController.navigate("commit_detail/$id")
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable(NavigationTab.SETTINGS.name) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToBranches = { navController.navigate("branches") },
                    onNavigateToAbout = { navController.navigate("about") },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable("commit_detail/{commitId}") { backStackEntry ->
                val commitId = backStackEntry.arguments?.getString("commitId") ?: ""
                CommitDetailScreen(
                    viewModel = viewModel,
                    commitId = commitId,
                    onBack = { navController.popBackStack() },
                    onAppClick = { app, prev ->
                        editingApp = app
                        previousAppForDiff = prev
                        isDetailEditable = false
                        editingNoteText = app.note
                        editingTagsList = app.tags.split(", ").filter { it.isNotEmpty() }
                    }
                )
            }
            composable("branches") {
                BranchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about") {
                top.hzchu.applog.ui.screens.AboutScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    AppDialogs(
        viewModel = viewModel,
        editingApp = editingApp,
        onDismissEditingApp = { editingApp = null },
        previousAppForDiff = previousAppForDiff,
        isDetailEditable = isDetailEditable,
        editingNoteText = editingNoteText,
        onNoteTextChange = { editingNoteText = it },
        editingTagsList = editingTagsList,
        onTagsListChange = { editingTagsList = it },
        showCommitDialog = showCommitDialog,
        commitMessageText = commitMessageText,
        onCommitMessageChange = { commitMessageText = it },
        pendingAutoMessage = pendingAutoMessage,
        allTags = allTags
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialogs(
    viewModel: MainViewModel,
    editingApp: AppInfo?,
    onDismissEditingApp: () -> Unit,
    previousAppForDiff: AppInfo?,
    isDetailEditable: Boolean,
    editingNoteText: String,
    onNoteTextChange: (String) -> Unit,
    editingTagsList: List<String>,
    onTagsListChange: (List<String>) -> Unit,
    showCommitDialog: Boolean,
    commitMessageText: String,
    onCommitMessageChange: (String) -> Unit,
    pendingAutoMessage: String,
    allTags: List<String>
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Note editing dialog
    if (editingApp != null) {
        val app = editingApp
        val prev = previousAppForDiff
        val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        val storeMap = mapOf(
            "com.xiaomi.market" to R.string.store_xiaomi,
            "com.heytap.market" to R.string.store_oppo,
            "com.bbk.appstore" to R.string.store_vivo,
            "com.huawei.appmarket" to R.string.store_huawei,
            "com.meizu.mstore" to R.string.store_meizu,
            "com.android.vending" to R.string.google_play_store,
            "com.sec.android.app.samsungapps" to R.string.store_samsung,
            "com.hihonor.appmarket" to R.string.store_honor,
            "com.lenovo.leos.appstore" to R.string.store_lenovo,
            "com.yulong.android.coolmart" to R.string.store_coolpad,
            "com.coolapk.market" to R.string.store_coolapk,
            "com.wandoujia.phoenix2" to R.string.store_wandoujia,
            "com.tencent.android.qqdownloader" to R.string.store_tencent,
            "com.baidu.appsearch" to R.string.store_baidu,
            "com.qihoo.appstore" to R.string.store_360
        )
        AlertDialog(
            onDismissRequest = onDismissEditingApp,
            title = { Text(app.appName) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppDetailRow(
                        label = stringResource(R.string.app_info_package),
                        value = app.packageName,
                        isChanged = prev != null && prev.packageName != app.packageName
                    )
                    AppDetailRow(
                        label = stringResource(R.string.app_info_version),
                        value = stringResource(R.string.version_format, app.versionName, app.versionCode),
                        isChanged = prev != null && (prev.versionCode != app.versionCode || prev.versionName != app.versionName)
                    )
                    AppDetailRow(
                        label = stringResource(R.string.app_info_install_time),
                        value = sdf.format(Date(app.firstInstallTime)),
                        isChanged = prev != null && prev.firstInstallTime != app.firstInstallTime
                    )
                    AppDetailRow(
                        label = stringResource(R.string.app_info_update_time),
                        value = sdf.format(Date(app.lastUpdateTime)),
                        isChanged = prev != null && prev.lastUpdateTime != app.lastUpdateTime
                    )
                    
                    val storeResId = storeMap[app.installerPackageName]
                    val installerText = if (storeResId != null) {
                        "${app.installerPackageName} (${stringResource(storeResId)})"
                    } else {
                        app.installerPackageName.ifEmpty { "无" }
                    }
                    val onInstallerClick: (() -> Unit)? = if (storeResId != null) {
                        {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
                                intent.setPackage(app.installerPackageName)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                if (app.installerPackageName == "com.android.vending") {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}"))
                                    context.startActivity(webIntent)
                                } else {
                                    // Fallback to general market intent if specific store fails
                                    try {
                                        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
                                        context.startActivity(genericIntent)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    } else null

                    AppDetailRow(
                        label = stringResource(R.string.app_info_installer),
                        value = installerText,
                        isChanged = prev != null && prev.installerPackageName != app.installerPackageName,
                        onClick = onInstallerClick
                    )
                    AppDetailRow(
                        label = stringResource(R.string.app_info_signature),
                        value = app.signatureSha256.ifEmpty { "无" },
                        isChanged = prev != null && prev.signatureSha256 != app.signatureSha256
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isDetailEditable) {
                        OutlinedTextField(
                            value = editingNoteText,
                            onValueChange = onNoteTextChange,
                            label = { Text(stringResource(R.string.app_info_note)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TagChipInput(
                            tags = editingTagsList,
                            onTagsChanged = onTagsListChange,
                            allExistingTags = allTags,
                            label = stringResource(R.string.app_info_tags)
                        )
                    } else {
                        AppDetailRow(
                            label = stringResource(R.string.app_info_note),
                            value = app.note.ifEmpty { "无" },
                            isChanged = prev != null && prev.note != app.note
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_info_tags),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (prev != null && prev.tags != app.tags) DiffUpdatedColor else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        TagChipDisplay(tags = editingTagsList)
                    }
                }
            },
            confirmButton = {
                if (isDetailEditable) {
                    TextButton(
                        onClick = {
                            viewModel.updateAppMetadata(app.packageName, editingNoteText, editingTagsList.joinToString(", "))
                            onDismissEditingApp()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                } else {
                    TextButton(onClick = onDismissEditingApp) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (isDetailEditable) {
                    TextButton(
                        onClick = onDismissEditingApp
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    // Commit message dialog
    if (showCommitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCommitDialog() },
            title = { Text(stringResource(R.string.commit_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.commit_message_placeholder),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = commitMessageText,
                        onValueChange = onCommitMessageChange,
                        placeholder = { Text(pendingAutoMessage) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.performCommit(commitMessageText)
                    }
                ) {
                    Text(stringResource(R.string.commit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissCommitDialog() }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}