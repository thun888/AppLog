package top.hzchu.applog

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.hzchu.applog.ui.navigation.NavigationTab
import top.hzchu.applog.ui.screens.AppsScreen
import top.hzchu.applog.ui.screens.BranchScreen
import top.hzchu.applog.ui.screens.CommitDetailScreen
import top.hzchu.applog.ui.screens.HistoryScreen
import top.hzchu.applog.ui.screens.SettingsScreen
import top.hzchu.applog.ui.theme.AppLogTheme
import top.hzchu.applog.viewmodel.MainViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogApp() {
    val viewModel: MainViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(NavigationTab.APPS) }
    var selectedCommitId by remember { mutableStateOf<String?>(null) }
    var isManagingBranches by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showCommitDialog by viewModel.showCommitDialog.collectAsState()
    val pendingAutoMessage by viewModel.pendingAutoMessage.collectAsState()
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

    var editingPackage by remember { mutableStateOf<String?>(null) }
    var editingNoteText by remember { mutableStateOf("") }

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
                    onAppClick = { pkg ->
                        editingPackage = pkg
                        editingNoteText = viewModel.notesMap.value[pkg] ?: ""
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
    if (editingPackage != null) {
        val pkg = editingPackage!!
        AlertDialog(
            onDismissRequest = { editingPackage = null },
            title = { Text(stringResource(R.string.edit_note)) },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text(stringResource(R.string.note_for, pkg)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateNote(pkg, editingNoteText)
                        editingPackage = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPackage = null }
                ) {
                    Text(stringResource(R.string.cancel))
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