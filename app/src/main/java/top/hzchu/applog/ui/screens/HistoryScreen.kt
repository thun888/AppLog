package top.hzchu.applog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import top.hzchu.applog.ui.utils.pluralStringResource
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
    val unpushedCount by viewModel.unpushedCount.collectAsState()
    val isPushing by viewModel.isPushing.collectAsState()
    val isPulling by viewModel.isPulling.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }
    var taggingCommit by remember { mutableStateOf<CommitInfo?>(null) }
    var tagName by remember { mutableStateOf("") }
    var tagMessage by remember { mutableStateOf("") }
    var showDeleteTagConfirm by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Autoload more when scrolling near the bottom
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentBranch,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (unpushedCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pluralStringResource(R.plurals.unpushed_count, unpushedCount, unpushedCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.pushWithSavedConfig() },
                        enabled = !isPushing && !isPulling
                    ) {
                        if (isPushing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = stringResource(R.string.push))
                        }
                    }
                    IconButton(
                        onClick = { viewModel.pullWithSavedConfig() },
                        enabled = !isPushing && !isPulling
                    ) {
                        if (isPulling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Filled.CloudDownload, contentDescription = stringResource(R.string.pull))
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
                        val isTagging = showTagDialog && taggingCommit?.id == commit.id
                        CommitItem(
                            commit = if (isTagging) taggingCommit!! else commit,
                            isSelected = false,
                            onClick = { onCommitClick(commit.id) },
                            onLongClick = {
                                taggingCommit = commit
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
                                Icon(Icons.Filled.ExpandMore, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.load_more))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTagDialog && taggingCommit != null) {
        AlertDialog(
            onDismissRequest = { 
                showTagDialog = false
                taggingCommit = null
            },
            title = { Text(stringResource(R.string.manage_tags)) },
            text = {
                Column {
                    if (taggingCommit!!.tags.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.existing_tags),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            taggingCommit!!.tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.remove_tag, tag),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    tagToDelete = tag
                                                    showDeleteTagConfirm = true
                                                },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    Text(
                        text = stringResource(R.string.create_tag),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            viewModel.createTag(taggingCommit!!.id, tagName, tagMessage)
                            showTagDialog = false
                            taggingCommit = null
                        }
                    },
                    enabled = tagName.isNotBlank()
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTagDialog = false
                    taggingCommit = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteTagConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteTagConfirm = false },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(stringResource(R.string.delete_tag_confirm, tagToDelete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tagToDelete)
                        // 同步更新本地状态，以便对话框不退出也能看到变化
                        taggingCommit = taggingCommit?.copy(
                            tags = taggingCommit!!.tags.filter { it != tagToDelete }
                        )
                        showDeleteTagConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTagConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
