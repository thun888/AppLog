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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
fun CommitDetailScreen(
    viewModel: MainViewModel,
    @Suppress("UNUSED_PARAMETER") commitId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diff by viewModel.detailDiffResult.collectAsState()
    val apps by viewModel.detailApps.collectAsState()
    val isComputing by viewModel.isComputingDiff.collectAsState()
    val commit by viewModel.currentDetailCommit.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = commit?.message ?: stringResource(R.string.loading),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (commit?.id != "CURRENT") {
                            Text(
                                text = commit?.shortId ?: "",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            } else if (diff == null) {
                Text(
                    text = stringResource(R.string.diff_failed, "No data"),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                DetailContentView(diff = diff!!, allApps = apps, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun DetailContentView(
    diff: top.hzchu.applog.model.DiffResult,
    allApps: List<top.hzchu.applog.model.AppInfo>,
    viewModel: MainViewModel
) {
    val addedPackages = diff.added.map { it.packageName }.toSet()
    val updatedPackages = diff.updated.map { it.second.packageName }.toSet()
    val removedPackages = diff.removed.map { it.packageName }.toSet()
    
    val unchangedApps = allApps.filter { 
        it.packageName !in addedPackages && it.packageName !in updatedPackages 
    }

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

        // 1. Added
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

        // 2. Updated
        if (diff.updated.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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

        // 3. Removed
        if (diff.removed.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                            viewModel.generateRestoreScript(diff.removed)
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

        // 4. Note Changed
        if (diff.noteChanged.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.diff_notes, diff.noteChanged.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.noteChanged) { (old, new) ->
                top.hzchu.applog.ui.components.DiffItemUpdated(old = old, new = new)
            }
        }

        // 5. Unchanged (The rest of the full list)
        if (unchangedApps.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.apps_count, unchangedApps.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(unchangedApps) { app ->
                top.hzchu.applog.ui.components.AppItem(app = app, onClick = {})
            }
        }
    }
}
