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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
    val canLoadMore by viewModel.canLoadMoreHistory.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }
    var taggingCommitId by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var tagMessage by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-load more when scrolling near the bottom
    LaunchedEffect(listState, commits.size, canLoadMore, isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filter { it != null && it >= commits.size - 2 }
            .distinctUntilChanged()
            .collect {
                if (canLoadMore && !isLoading) {
                    viewModel.loadMoreHistory()
                }
            }
    }

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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    CommitItem(
                        commit = CommitInfo(
                            id = "CURRENT",
                            shortId = "CURRENT",
                            message = stringResource(R.string.current_status),
                            author = "",
                            timestamp = System.currentTimeMillis()
                        ),
                        isSelected = false,
                        onClick = { onCommitClick("CURRENT") }
                    )
                }

                if (commits.isEmpty() && !isLoading) {
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

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (canLoadMore) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMoreHistory() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(stringResource(R.string.load_more))
                            }
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
