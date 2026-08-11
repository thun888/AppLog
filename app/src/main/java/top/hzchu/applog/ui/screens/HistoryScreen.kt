package top.hzchu.applog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import top.hzchu.applog.R
import top.hzchu.applog.git.CommitInfo
import top.hzchu.applog.ui.components.CommitItem
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onCommitClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commits by viewModel.commits.collectAsState()
    val isLoading by viewModel.isLoadingHistory.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }
    var taggingCommitId by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var tagMessage by remember { mutableStateOf("") }

    var showBranchMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.tab_history))
                        Text(
                            text = currentBranch,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showBranchMenu = true }) {
                            Icon(Icons.Filled.AccountTree, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showBranchMenu,
                            onDismissRequest = { showBranchMenu = false }
                        ) {
                            branches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = branch,
                                            fontWeight = if (branch == currentBranch) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.switchBranch(branch)
                                        showBranchMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.loadHistory() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan))
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        CommitItem(
                            commit = CommitInfo(
                                id = "CURRENT",
                                shortId = "CUR",
                                message = stringResource(R.string.current_status),
                                author = "",
                                timestamp = System.currentTimeMillis()
                            ),
                            isSelected = false,
                            onClick = { onCommitClick("CURRENT") }
                        )
                    }

                    if (commits.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.no_commits),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(commits, key = { it.id }) { commit ->
                            CommitItem(
                                commit = commit,
                                isSelected = false,
                                onClick = { onCommitClick(commit.id) },
                                onLongClick = {
                                    taggingCommitId = commit.id
                                    tagName = ""
                                    tagMessage = ""
                                    showTagDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text(stringResource(R.string.create_tag)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text(stringResource(R.string.tag_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagMessage,
                        onValueChange = { tagMessage = it },
                        label = { Text(stringResource(R.string.tag_message)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            viewModel.createTag(taggingCommitId, tagName, tagMessage)
                            showTagDialog = false
                        }
                    },
                    enabled = tagName.isNotBlank()
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
