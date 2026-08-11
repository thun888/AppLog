package top.hzchu.applog

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.hzchu.applog.ui.navigation.NavigationTab
import top.hzchu.applog.ui.screens.AppsScreen
import top.hzchu.applog.ui.screens.DiffScreen
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
    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

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
            NavigationBar {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == NavigationTab.APPS) {
                FloatingActionButton(
                    onClick = { viewModel.commitChanges() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Commit")
                }
            }
        }
    ) { innerPadding ->
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
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.DIFF -> DiffScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            NavigationTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Note editing dialog
    if (editingPackage != null) {
        val pkg = editingPackage!!
        AlertDialog(
            onDismissRequest = { editingPackage = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text("Note for " + pkg) },
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPackage = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}