package top.hzchu.applog.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import top.hzchu.applog.ui.utils.pluralStringResource
import kotlinx.coroutines.launch
import top.hzchu.applog.R
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.ui.components.AlphabetIndexBar
import top.hzchu.applog.ui.components.DiffAddedColor
import top.hzchu.applog.ui.components.DiffItemAdded
import top.hzchu.applog.ui.components.DiffItemRemoved
import top.hzchu.applog.ui.components.DiffItemUpdated
import top.hzchu.applog.ui.components.DiffRemovedColor
import top.hzchu.applog.ui.components.DiffUpdatedColor
import top.hzchu.applog.utils.PinyinUtils
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAppClick: (top.hzchu.applog.model.AppInfo, top.hzchu.applog.model.AppInfo?) -> Unit,
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
                DetailContentView(
                    diff = diff!!, 
                    allApps = apps, 
                    viewModel = viewModel,
                    onAppClick = onAppClick
                )
            }
        }
    }
}

@Composable
private fun DetailContentView(
    diff: top.hzchu.applog.model.DiffResult,
    allApps: List<top.hzchu.applog.model.AppInfo>,
    viewModel: MainViewModel,
    onAppClick: (top.hzchu.applog.model.AppInfo, top.hzchu.applog.model.AppInfo?) -> Unit
) {
    val showSystem by viewModel.showSystemApps.collectAsState()
    val showUser by viewModel.showUserApps.collectAsState()
    val isIndexBarEnabled by viewModel.showIndexBar.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun AppInfo.shouldShow() = if (appType == AppInfo.AppType.SYSTEM) showSystem else showUser

    val addedPackages = diff.added.map { it.packageName }.toSet()
    val updatedPackages = diff.updated.map { it.second.packageName }.toSet()

    val appsToShow = remember(allApps, addedPackages, updatedPackages, showSystem, showUser) {
        allApps.filter {
            it.packageName !in addedPackages && it.packageName !in updatedPackages
        }.filter { it.shouldShow() }.sortedBy { PinyinUtils.getPinyin(it.appName) }
    }

    val showIndexBar = isIndexBarEnabled && isExpanded && appsToShow.isNotEmpty()

    val alphabet = remember(appsToShow, showIndexBar) {
        if (!showIndexBar) emptyList()
        else appsToShow.map { app ->
            PinyinUtils.getFirstLetter(app.appName).toString()
        }.distinct().sortedBy { if (it == "#") "{" else it }
    }

    val indexMap = remember(appsToShow, showIndexBar) {
        if (!showIndexBar) emptyMap()
        else {
            val map = mutableMapOf<String, Int>()
            // We need to find the actual index in the LazyColumn.
            // Items before the expanded list:
            // 1 Summary
            // if (added) 1 header + N items
            // if (updated) 1 divider + 1 header + N items
            // if (removed) 1 divider + 1 header + N items
            // if (noteChanged) 1 divider + 1 header + N items
            // if (tagsChanged) 1 divider + 1 header + N items
            // 1 divider + 1 unchanged toggle
            var baseIndex = 1
            if (diff.added.isNotEmpty()) baseIndex += 1 + diff.added.size
            if (diff.updated.isNotEmpty()) baseIndex += 2 + diff.updated.size
            if (diff.removed.isNotEmpty()) baseIndex += 2 + diff.removed.size
            if (diff.noteChanged.isNotEmpty()) baseIndex += 2 + diff.noteChanged.size
            if (diff.tagsChanged.isNotEmpty()) baseIndex += 2 + diff.tagsChanged.size
            baseIndex += 2 // divider and toggle

            appsToShow.forEachIndexed { index, app ->
                val char = PinyinUtils.getFirstLetter(app.appName).toString()
                if (!map.containsKey(char)) {
                    map[char] = baseIndex + index
                }
            }
            map
        }
    }

    val summary = buildList {
        if (diff.added.isNotEmpty()) add(pluralStringResource(R.plurals.diff_summary_added, diff.added.size, diff.added.size))
        if (diff.removed.isNotEmpty()) add(pluralStringResource(R.plurals.diff_summary_removed, diff.removed.size, diff.removed.size))
        if (diff.updated.isNotEmpty()) add(pluralStringResource(R.plurals.diff_summary_updated, diff.updated.size, diff.updated.size))
        if (diff.noteChanged.isNotEmpty()) add(pluralStringResource(R.plurals.diff_summary_notes, diff.noteChanged.size, diff.noteChanged.size))
        if (diff.tagsChanged.isNotEmpty()) add(pluralStringResource(R.plurals.diff_summary_tags, diff.tagsChanged.size, diff.tagsChanged.size))
    }.joinToString(", ").ifEmpty { stringResource(R.string.no_changes) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = if (showIndexBar) 36.dp else 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = summary,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

        // 1. Added
        if (diff.added.isNotEmpty()) {
            item {
                Text(
                    text = pluralStringResource(R.plurals.diff_added, diff.added.size, diff.added.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffAddedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.added) { app -> 
                DiffItemAdded(app = app, onClick = { onAppClick(app, null) }) 
            }
        }

        // 2. Updated
        if (diff.updated.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = pluralStringResource(R.plurals.diff_updated, diff.updated.size, diff.updated.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffUpdatedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.updated) { (old, new) ->
                DiffItemUpdated(old = old, new = new, onClick = { onAppClick(new, old) })
            }
        }

        // 3. Removed
        if (diff.removed.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = pluralStringResource(R.plurals.diff_removed, diff.removed.size, diff.removed.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = DiffRemovedColor,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.removed) { app -> 
                DiffItemRemoved(app = app, onClick = { onAppClick(app, null) }) 
            }
//            item {
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(12.dp),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Button(
//                        onClick = {
//                            viewModel.generateRestoreScript(diff.removed)
//                        },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = DiffRemovedColor
//                        )
//                    ) {
//                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(stringResource(R.string.copy_adb_script))
//                    }
//                }
//            }
        }

        // 4. Note Changed
        if (diff.noteChanged.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = pluralStringResource(R.plurals.diff_notes, diff.noteChanged.size, diff.noteChanged.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.noteChanged) { (old, new) ->
                top.hzchu.applog.ui.components.DiffItemNoteChanged(
                    old = old,
                    new = new,
                    onClick = { onAppClick(new, old) }
                )
            }
        }

        // 5. Tags Changed
        if (diff.tagsChanged.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = pluralStringResource(R.plurals.diff_tags, diff.tagsChanged.size, diff.tagsChanged.size),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(diff.tagsChanged) { (old, new) ->
                top.hzchu.applog.ui.components.DiffItemTagsChanged(
                    old = old,
                    new = new,
                    onClick = { onAppClick(new, old) }
                )
            }
        }

        // 6. Unchanged (The rest of the full list)
        item {
            val unchangedCount = androidx.compose.runtime.remember(allApps, addedPackages, updatedPackages, showSystem, showUser) {
                allApps.count { it.packageName !in addedPackages && it.packageName !in updatedPackages && it.shouldShow() }
            }

            if (unchangedCount > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.apps_count, unchangedCount, unchangedCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) 
                            Icons.Filled.KeyboardArrowDown 
                        else 
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (isExpanded) {
            items(appsToShow, key = { it.packageName }) { app ->
                top.hzchu.applog.ui.components.AppItem(app = app, onClick = { onAppClick(app, null) })
            }
        }
    }

    if (showIndexBar && alphabet.isNotEmpty()) {
        AlphabetIndexBar(
            alphabet = alphabet,
            onIndexSelected = { char ->
                indexMap[char]?.let { index ->
                    coroutineScope.launch {
                        listState.scrollToItem(index)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .width(24.dp)
        )
    }
}
}

