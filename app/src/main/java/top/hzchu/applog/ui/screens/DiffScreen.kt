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
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import top.hzchu.applog.R
import top.hzchu.applog.ui.components.DiffAddedColor
import top.hzchu.applog.ui.components.DiffItemAdded
import top.hzchu.applog.ui.components.DiffItemRemoved
import top.hzchu.applog.ui.components.DiffItemUpdated
import top.hzchu.applog.ui.components.DiffRemovedColor
import top.hzchu.applog.ui.components.DiffUpdatedColor
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val diff by viewModel.diffResult.collectAsState()
    val isComputing by viewModel.isComputingDiff.collectAsState()
    val selected1 by viewModel.selectedCommit1.collectAsState()
    val selected2 by viewModel.selectedCommit2.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_diff)) },
                actions = {
                    IconButton(
                        onClick = { viewModel.computeDiffWithCurrent() },
                        enabled = selected1 != null
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.scan))
                    }
                    IconButton(
                        onClick = { viewModel.computeDiff() },
                        enabled = selected1 != null && selected2 != null
                    ) {
                        Icon(Icons.Filled.Compare, contentDescription = stringResource(R.string.compute_diff))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (isComputing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (diff == null || diff!!.isEmpty) {
                EmptyDiffView { viewModel.computeDiff() }
            } else {
                DiffContentView(diff = diff!!, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EmptyDiffView(onCompute: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_diff),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.select_in_history),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCompute) {
            Text(stringResource(R.string.compute_diff))
        }
    }
}

@Composable
private fun DiffContentView(
    diff: top.hzchu.applog.model.DiffResult,
    viewModel: MainViewModel
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = diff.summary,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (diff.added.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.diff_added, diff.added.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffAddedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.added) { app -> DiffItemAdded(app) }
        }

        if (diff.removed.isNotEmpty()) {
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.diff_removed, diff.removed.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffRemovedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.removed) { app -> DiffItemRemoved(app) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val script = viewModel.generateRestoreScript(diff.removed)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiffRemovedColor
                        )
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.copy_adb_script))
                    }
                }
            }
        }

        if (diff.updated.isNotEmpty()) {
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.diff_updated, diff.updated.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffUpdatedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.updated) { (old, new) ->
                DiffItemUpdated(old = old, new = new)
            }
        }
    }
}
