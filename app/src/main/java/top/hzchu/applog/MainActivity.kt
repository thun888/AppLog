package top.hzchu.applog

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

@Composable
fun AppDetailRow(label: String, value: String, isChanged: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isChanged) DiffUpdatedColor else MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
            color = if (isChanged) DiffUpdatedColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogApp() {
    val viewModel: MainViewModel = viewModel()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    if (isFirstLaunch) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            top.hzchu.applog.ui.screens.SetupScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    var selectedTab by remember { mutableStateOf(NavigationTab.APPS) }
    var selectedCommitId by remember { mutableStateOf<String?>(null) }
    var isManagingBranches by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showCommitDialog by viewModel.showCommitDialog.collectAsState()
    val pendingAutoMessage by viewModel.pendingAutoMessage.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

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
            if (selectedCommitId == null && !isManagingBranches) {
                NavigationBar {
                    NavigationTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == NavigationTab.APPS && selectedCommitId == null && !isManagingBranches) {
                FloatingActionButton(
                    onClick = { viewModel.prepareCommit() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.commit))
                }
            }
        }
    ) { innerPadding ->
        if (selectedCommitId != null) {
            CommitDetailScreen(
                viewModel = viewModel,
                commitId = selectedCommitId!!,
                onBack = { selectedCommitId = null },
                onAppClick = { app, prev ->
                    editingApp = app
                    previousAppForDiff = prev
                    isDetailEditable = false
                    editingNoteText = app.note
                    editingTagsList = app.tags.split(", ").filter { it.isNotEmpty() }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (isManagingBranches) {
            BranchScreen(
                viewModel = viewModel,
                onBack = { isManagingBranches = false },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedTab) {
                NavigationTab.APPS -> AppsScreen(
                    viewModel = viewModel,
                    onAppClick = { app, prev ->
                        editingApp = app
                        previousAppForDiff = prev
                        isDetailEditable = true
                        editingNoteText = app.note
                        editingTagsList = app.tags.split(", ").filter { it.isNotEmpty() }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    onCommitClick = { id ->
                        viewModel.loadCommitDetail(id)
                        selectedCommitId = id
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToBranches = { isManagingBranches = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Note editing dialog
    if (editingApp != null) {
        val app = editingApp!!
        val prev = previousAppForDiff
        val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { editingApp = null },
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
                    AppDetailRow(
                        label = stringResource(R.string.app_info_installer),
                        value = app.installerPackageName.ifEmpty { "无" },
                        isChanged = prev != null && prev.installerPackageName != app.installerPackageName
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
                            onValueChange = { editingNoteText = it },
                            label = { Text(stringResource(R.string.app_info_note)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TagChipInput(
                            tags = editingTagsList,
                            onTagsChanged = { editingTagsList = it },
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
                            editingApp = null
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                } else {
                    TextButton(onClick = { editingApp = null }) {
                        Text(stringResource(R.string.save).let { "OK" }) // Or just OK
                    }
                }
            },
            dismissButton = {
                if (isDetailEditable) {
                    TextButton(
                        onClick = { editingApp = null }
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
                        onValueChange = { commitMessageText = it },
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