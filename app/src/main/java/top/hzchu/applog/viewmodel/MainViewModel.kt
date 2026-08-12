package top.hzchu.applog.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.hzchu.applog.R
import top.hzchu.applog.git.CommitInfo
import top.hzchu.applog.git.GitManager
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.model.DiffResult
import top.hzchu.applog.receiver.PackageChangeReceiver
import top.hzchu.applog.scanner.AppScanner
import top.hzchu.applog.serializer.AppListSerializer

enum class AppSortOrder {
    NAME, PACKAGE_NAME, INSTALL_TIME, UPDATE_TIME
}

enum class AppGrouping {
    NONE, TAGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_REMOTE = "applog_remote_secure"
        private const val PREFS_DEBOUNCE = "applog_debounce"
        private const val PREFS_SETTINGS = "applog_settings"
        private const val PREFS_GIT_IDENTITY = "applog_git_identity"

        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_REMOTE_USER = "remote_username"
        private const val KEY_REMOTE_PASS = "remote_password"
        private const val KEY_DEBOUNCE_THRESHOLD = "debounce_threshold"
        private const val KEY_AUTO_SCAN = "auto_scan_on_start"
        private const val KEY_GIT_AUTHOR_NAME = "git_author_name"
        private const val KEY_GIT_AUTHOR_EMAIL = "git_author_email"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_SHOW_USER_APPS = "show_user_apps"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_IGNORE_SSL = "ignore_ssl_errors"
    }

    private val appScanner = AppScanner(application)
    val gitManager = GitManager(application)

    // --- UI State ---

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _showSystemApps = MutableStateFlow(
        application.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_SYSTEM_APPS, true)
    )
    val showSystemApps = _showSystemApps.asStateFlow()

    private val _showUserApps = MutableStateFlow(
        application.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_USER_APPS, true)
    )
    val showUserApps = _showUserApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(AppSortOrder.NAME)
    val sortOrder = _sortOrder.asStateFlow()

    private val _grouping = MutableStateFlow(AppGrouping.NONE)
    val grouping = _grouping.asStateFlow()

    val filteredApps = combine(
        _apps, _showSystemApps, _showUserApps, _searchQuery, _sortOrder
    ) { apps, showSystem, showUser, query, sort ->
        apps.filter { app ->
            val matchesFilter = if (app.appType == AppInfo.AppType.SYSTEM) showSystem else showUser
            val matchesSearch = query.isBlank() || 
                    app.appName.contains(query, ignoreCase = true) || 
                    app.packageName.contains(query, ignoreCase = true) || 
                    app.note.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }.let { filtered ->
            when (sort) {
                AppSortOrder.NAME -> filtered.sortedBy { it.appName.lowercase() }
                AppSortOrder.PACKAGE_NAME -> filtered.sortedBy { it.packageName.lowercase() }
                AppSortOrder.INSTALL_TIME -> filtered.sortedByDescending { it.firstInstallTime }
                AppSortOrder.UPDATE_TIME -> filtered.sortedByDescending { it.lastUpdateTime }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedApps = combine(filteredApps, _grouping) { apps, grouping ->
        when (grouping) {
            AppGrouping.NONE -> mapOf("" to apps)
            AppGrouping.TAGS -> {
                val result = mutableMapOf<String, MutableList<AppInfo>>()
                apps.forEach { app ->
                    val tags = app.tags.split(", ").filter { it.isNotEmpty() }
                    if (tags.isEmpty()) {
                        result.getOrPut("") { mutableListOf() }.add(app)
                    } else {
                        tags.forEach { tag ->
                            result.getOrPut(tag) { mutableListOf() }.add(app)
                        }
                    }
                }
                result.mapValues { it.value.toList() }.toSortedMap { a, b ->
                    if (a == "") 1 else if (b == "") -1 else a.lowercase().compareTo(b.lowercase())
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(
        application.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_LAUNCH, true)
    )
    val isFirstLaunch = _isFirstLaunch.asStateFlow()

    private val _commits = MutableStateFlow<List<CommitInfo>>(emptyList())
    val commits: StateFlow<List<CommitInfo>> = _commits.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _canLoadMoreHistory = MutableStateFlow(true)
    val canLoadMoreHistory: StateFlow<Boolean> = _canLoadMoreHistory.asStateFlow()

    private var historyPageSize = 20
    private var historyCurrentOffset = 0

    private val _branches = MutableStateFlow<List<String>>(emptyList())
    val branches: StateFlow<List<String>> = _branches.asStateFlow()

    private val _currentBranch = MutableStateFlow(GitManager.DEFAULT_BRANCH)
    val currentBranch: StateFlow<String> = _currentBranch.asStateFlow()

    private val _unpushedCount = MutableStateFlow(0)
    val unpushedCount: StateFlow<Int> = _unpushedCount.asStateFlow()

    private val _diffResult = MutableStateFlow<DiffResult?>(null)
    val diffResult: StateFlow<DiffResult?> = _diffResult.asStateFlow()

    private val _isComputingDiff = MutableStateFlow(false)
    val isComputingDiff: StateFlow<Boolean> = _isComputingDiff.asStateFlow()

    private val _detailDiffResult = MutableStateFlow<DiffResult?>(null)
    val detailDiffResult: StateFlow<DiffResult?> = _detailDiffResult.asStateFlow()

    private val _detailApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val detailApps: StateFlow<List<AppInfo>> = _detailApps.asStateFlow()

    private val _currentDetailCommit = MutableStateFlow<CommitInfo?>(null)
    val currentDetailCommit: StateFlow<CommitInfo?> = _currentDetailCommit.asStateFlow()

    private val _showCommitDialog = MutableStateFlow(false)
    val showCommitDialog: StateFlow<Boolean> = _showCommitDialog.asStateFlow()

    private val _pendingAutoMessage = MutableStateFlow("")
    val pendingAutoMessage: StateFlow<String> = _pendingAutoMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _extractingProgress = MutableStateFlow<Float?>(null)
    val extractingProgress: StateFlow<Float?> = _extractingProgress.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun startExtraction(context: Context, packageName: String, appName: String) {
        viewModelScope.launch {
            _extractingProgress.value = 0f
            val result = withContext(Dispatchers.IO) {
                top.hzchu.applog.utils.ApkHelper.extractApk(context, packageName, appName) { progress ->
                    _extractingProgress.value = progress
                }
            }
            _extractingProgress.value = null
            if (result.isSuccess) {
                showToast(context.getString(R.string.extract_success))
            } else {
                showToast(context.getString(R.string.extract_failed, result.exceptionOrNull()?.message))
            }
        }
    }

    // --- Notes ---

    private val remotePrefs by lazy {
        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            application,
            PREFS_REMOTE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _notesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val notesMap: StateFlow<Map<String, String>> = _notesMap.asStateFlow()

    private val _tagsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagsMap: StateFlow<Map<String, String>> = _tagsMap.asStateFlow()

    /** 所有已使用过的独立标签（去重、排序），供输入提示用 */
    val allTags: StateFlow<List<String>> = _tagsMap
        .map { map ->
            map.values
                .flatMap { it.split(", ").filter { tag -> tag.isNotEmpty() } }
                .distinct()
                .sortedBy { it.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (!_isFirstLaunch.value) {
                initializeAfterOnboarding()
            }
        }
    }

    private suspend fun initializeAfterOnboarding() {
        gitManager.init()
        loadMetadataFromWorkingFile()
        loadBranches()
        loadHistory()
        if (getAutoScanOnStart()) {
            scanApps()
        }
    }

    fun completeOnboarding(
        url: String,
        username: String,
        password: String,
        initialBranch: String,
        authorName: String = "",
        authorEmail: String = "",
        ignoreSsl: Boolean = false
    ) {
        viewModelScope.launch {
            val finalName = authorName.ifBlank { "AppLog" }
            val finalEmail = authorEmail.ifBlank { "applog@local" }
            val finalBranch = initialBranch.ifBlank { GitManager.DEFAULT_BRANCH }
            
            saveGitIdentity(finalName, finalEmail)
            saveRemoteConfig(url, username, password)
            setIgnoreSslErrors(ignoreSsl)
            
            gitManager.init(finalBranch).onSuccess {
                _isFirstLaunch.value = false
                getApplication<Application>().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                initializeAfterOnboarding()
            }.onFailure {
                showToast(getApplication<Application>().getString(R.string.error_prefix, it.message))
            }
        }
    }

    // --- Scanning ---

    fun scanApps() {
        viewModelScope.launch {
            try {
                scanAndUpdateApps()
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.scan_failed, e.message)
            }
        }
    }

    // --- Git Operations ---

    private fun computeDiff(currentApps: List<AppInfo>, previousApps: List<AppInfo>): DiffResult {
        val mapPrev = AppListSerializer.buildAppMap(previousApps)
        val mapCurr = AppListSerializer.buildAppMap(currentApps)

        val added = currentApps.filter { it.packageName !in mapPrev }
        val removed = previousApps.filter { it.packageName !in mapCurr }
        val updated = mutableListOf<Pair<AppInfo, AppInfo>>()
        val noteChanged = mutableListOf<Pair<AppInfo, AppInfo>>()
        val tagsChanged = mutableListOf<Pair<AppInfo, AppInfo>>()

        for (appCurr in currentApps) {
            val appPrev = mapPrev[appCurr.packageName] ?: continue
            if (isAppVersionChanged(appPrev, appCurr)) {
                updated.add(appPrev to appCurr)
            }
            if (appPrev.tags != appCurr.tags) {
                tagsChanged.add(appPrev to appCurr)
            }
            if (appPrev.note != appCurr.note) {
                noteChanged.add(appPrev to appCurr)
            }
        }

        return DiffResult(added = added, removed = removed, updated = updated, noteChanged = noteChanged, tagsChanged = tagsChanged)
    }

    private data class DiffCounts(
        val added: Int = 0,
        val removed: Int = 0,
        val updated: Int = 0,
        val noteChanged: Int = 0,
        val tagsChanged: Int = 0
    ) {
        val hasChanges get() = added > 0 || removed > 0 || updated > 0 || noteChanged > 0 || tagsChanged > 0
    }

    private suspend fun computeDiffCounts(): DiffCounts {
        val lastApps = getLastSnapshotApps()
        val diff = computeDiff(_apps.value, lastApps)
        return DiffCounts(diff.added.size, diff.removed.size, diff.updated.size, diff.noteChanged.size, diff.tagsChanged.size)
    }

    private fun isAppVersionChanged(oldApp: AppInfo, newApp: AppInfo): Boolean {
        return oldApp.versionCode != newApp.versionCode ||
                oldApp.versionName != newApp.versionName ||
                oldApp.signatureSha256 != newApp.signatureSha256
    }

    private suspend fun getLastSnapshotApps(): List<AppInfo> {
        val content = gitManager.getSnapshotContent().getOrDefault("")
        return AppListSerializer.deserialize(content)
    }
    fun prepareCommit() {
        viewModelScope.launch {
            try {
                if (_apps.value.isEmpty()) {
                    scanAndUpdateApps()
                }

                var counts = computeDiffCounts()

                // 无变更但数据可能已过时 → 刷新后重试一次
                if (!counts.hasChanges && _apps.value.isNotEmpty()) {
                    scanAndUpdateApps()
                    counts = computeDiffCounts()
                }

                if (!counts.hasChanges) {
                    _toastMessage.value = getApplication<Application>().getString(R.string.no_changes)
                    return@launch
                }

                _pendingAutoMessage.value = gitManager.generateCommitMessage(
                    counts.added, counts.removed, counts.updated, counts.noteChanged, counts.tagsChanged
                )
                _showCommitDialog.value = true
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, e.message)
            }
        }
    }

    private suspend fun scanAndUpdateApps() {
        _isScanning.value = true
        try {
            val notes = _notesMap.value
            val tags = _tagsMap.value
            val scanned = withContext(Dispatchers.IO) { appScanner.scanAllApps() }
            _apps.value = scanned.map { app ->
                app.copy(
                    note = notes[app.packageName] ?: "",
                    tags = tags[app.packageName] ?: ""
                )
            }
        } finally {
            _isScanning.value = false
        }
    }

    fun dismissCommitDialog() {
        _showCommitDialog.value = false
    }

    fun performCommit(customMessage: String) {
        val finalMessage = customMessage.ifBlank { _pendingAutoMessage.value }
        _showCommitDialog.value = false

        viewModelScope.launch {
            try {
                val currentContent = AppListSerializer.serialize(_apps.value)
                val (authorName, authorEmail) = getGitIdentity()
                val result = gitManager.commitSnapshot(
                    content = currentContent,
                    message = finalMessage,
                    authorName = authorName.ifBlank { "AppLog" },
                    authorEmail = authorEmail.ifBlank { "applog@local" }
                )
                if (result.isSuccess) {
                    val commitId = result.getOrDefault("")
                    if (commitId.isNotEmpty()) {
                        _toastMessage.value = getApplication<Application>().getString(R.string.committed, finalMessage)
                    } else {
                        _toastMessage.value = getApplication<Application>().getString(R.string.no_changes)
                    }
                    loadHistory()
                    PackageChangeReceiver.resetCounter(getApplication())
                } else {
                    _toastMessage.value = getApplication<Application>().getString(R.string.commit_failed, result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, e.message)
            }
        }
    }

    fun commitChanges() {
        prepareCommit()
    }

    fun loadHistory() {
        viewModelScope.launch {
            historyCurrentOffset = 0
            _canLoadMoreHistory.value = true
            loadHistoryInternal(append = false)
        }
    }

    fun loadMoreHistory() {
        if (_isLoadingHistory.value || !_canLoadMoreHistory.value) return
        viewModelScope.launch {
            loadHistoryInternal(append = true)
        }
    }

    private suspend fun loadHistoryInternal(append: Boolean) {
        _isLoadingHistory.value = true
        gitManager.getCommitHistory(skip = historyCurrentOffset, maxCount = historyPageSize).onSuccess {
            if (append) {
                _commits.value = _commits.value + it
            } else {
                _commits.value = it
            }
            historyCurrentOffset += it.size
            _canLoadMoreHistory.value = it.size >= historyPageSize
        }.onFailure {
            if (!append) _commits.value = emptyList()
            _canLoadMoreHistory.value = false
        }
        _currentBranch.value = gitManager.getCurrentBranch()
        _unpushedCount.value = gitManager.getUnpushedCount().getOrDefault(0)
        loadMetadataFromWorkingFile()
        _isLoadingHistory.value = false
    }

    fun loadBranches() {
        viewModelScope.launch {
            gitManager.getBranches().onSuccess {
                _branches.value = it
            }
        }
    }

    fun switchBranch(branchName: String) {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            gitManager.checkoutBranch(branchName).onSuccess {
                _currentBranch.value = branchName
                loadBranches() // Refresh list to update selection markers
                historyCurrentOffset = 0
                _canLoadMoreHistory.value = true
                loadHistoryInternal(append = false)
            }.onFailure {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, it.message)
                _isLoadingHistory.value = false
            }
        }
    }

    fun createBranch(branchName: String, isOrphan: Boolean = false) {
        viewModelScope.launch {
            gitManager.createBranch(branchName, isOrphan).onSuccess {
                loadBranches()
                if (isOrphan) {
                    // Orphan branch automatically checkouts, so refresh history
                    historyCurrentOffset = 0
                    _canLoadMoreHistory.value = true
                    loadHistoryInternal(append = false)
                }
                _toastMessage.value = getApplication<Application>().getString(R.string.branch_created, branchName)
            }.onFailure {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, it.message)
            }
        }
    }

    fun deleteBranch(branchName: String) {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            gitManager.deleteBranch(branchName, force = true).onSuccess {
                loadBranches()
                _toastMessage.value = getApplication<Application>().getString(R.string.branch_deleted, branchName)
            }.onFailure {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, it.message)
            }
            _isLoadingHistory.value = false
        }
    }

    fun loadCommitDetail(commitId: String) {
        viewModelScope.launch {
            _isComputingDiff.value = true
            try {
                if (commitId == "CURRENT") {
                    // Reuse cached apps if available, otherwise scan
                    val currentApps = if (_apps.value.isNotEmpty()) {
                        _apps.value
                    } else {
                        val scanned = withContext(Dispatchers.IO) { appScanner.scanAllApps() }
                        val notes = _notesMap.value
                        val tags = _tagsMap.value
                        scanned.map { app -> app.copy(note = notes[app.packageName] ?: "", tags = tags[app.packageName] ?: "") }.also {
                            _apps.value = it
                        }
                    }

                    val headContent = gitManager.getSnapshotContent().getOrThrow()
                    val headApps = AppListSerializer.deserialize(headContent)
                    val diff = computeDiff(currentApps, headApps)

                    _currentDetailCommit.value = CommitInfo(
                        id = "CURRENT",
                        shortId = "CURRENT",
                        message = getApplication<Application>().getString(R.string.current_status),
                        author = "",
                        timestamp = System.currentTimeMillis()
                    )
                    _detailApps.value = currentApps
                    _detailDiffResult.value = diff
                } else {
                    val commit = _commits.value.find { it.id == commitId } ?: return@launch
                    val parentId = gitManager.getParentCommitId(commitId)
                    
                    val currentContent = gitManager.getSnapshotContent(commitId).getOrThrow()
                    val currentApps = AppListSerializer.deserialize(currentContent)
                    
                    val parentApps = if (parentId != null) {
                        val parentContent = gitManager.getSnapshotContent(parentId).getOrThrow()
                        AppListSerializer.deserialize(parentContent)
                    } else {
                        emptyList()
                    }

                    val diff = computeDiff(currentApps, parentApps)

                    _currentDetailCommit.value = commit
                    _detailApps.value = currentApps
                    _detailDiffResult.value = diff
                }
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.diff_failed, e.message)
            } finally {
                _isComputingDiff.value = false
            }
        }
    }

    // --- Tagging ---

    fun createTag(commitId: String, tagName: String, message: String) {
        viewModelScope.launch {
            gitManager.createTag(commitId, tagName, message).onSuccess {
                _toastMessage.value = getApplication<Application>().getString(R.string.tag_created, tagName)
                loadHistory()
            }.onFailure {
                _toastMessage.value = getApplication<Application>().getString(R.string.tag_failed, it.message)
            }
        }
    }

    // --- Notes & Tags ---

    /**
     * 标签确定性归一化：按逗号分割、去空白、排序、去重、重新拼接
     */
    private fun normalizeTags(raw: String): String {
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase() }
            .joinToString(", ")
    }

    /**
     * 从 Git 工作目录的 apps_snapshot.txt 加载备注和标签
     */
    private fun loadMetadataFromWorkingFile() {
        try {
            val file = java.io.File(
                getApplication<Application>().filesDir,
                GitManager.REPO_DIR + "/" + GitManager.SNAPSHOT_FILE
            )
            if (!file.exists()) return
            val apps = AppListSerializer.deserialize(file.readText())
            _notesMap.value = apps
                .filter { it.note.isNotEmpty() }
                .associate { it.packageName to it.note }
            _tagsMap.value = apps
                .filter { it.tags.isNotEmpty() }
                .associate { it.packageName to it.tags }
        } catch (_: Exception) {
            // 文件不存在或损坏时，保持现有 map
        }
    }

    /**
     * 将内存中的备注和标签写回工作文件
     * 仅更新 note 和 tags 字段，保留文件中其他应用字段不变
     */
    private fun syncMetadataToWorkingFile() {
        try {
            val file = java.io.File(
                getApplication<Application>().filesDir,
                GitManager.REPO_DIR + "/" + GitManager.SNAPSHOT_FILE
            )
            val existingApps = if (file.exists()) {
                AppListSerializer.deserialize(file.readText())
            } else {
                emptyList()
            }
            val notes = _notesMap.value
            val tags = _tagsMap.value
            val updatedApps = existingApps.map { app ->
                var updated = app
                val newNote = notes[app.packageName]
                if (newNote != null) updated = updated.copy(note = newNote)
                val newTags = tags[app.packageName]
                if (newTags != null) updated = updated.copy(tags = newTags)
                updated
            }
            file.writeText(AppListSerializer.serialize(updatedApps))
        } catch (_: Exception) {
            // 写入失败不影响内存状态
        }
    }

    fun updateAppMetadata(packageName: String, note: String, tags: String) {
        val normalizedTags = normalizeTags(tags)

        val currentNotes = _notesMap.value.toMutableMap()
        if (note.isBlank()) currentNotes.remove(packageName)
        else currentNotes[packageName] = note
        _notesMap.value = currentNotes

        val currentTags = _tagsMap.value.toMutableMap()
        if (normalizedTags.isBlank()) currentTags.remove(packageName)
        else currentTags[packageName] = normalizedTags
        _tagsMap.value = currentTags

        syncMetadataToWorkingFile()

        _apps.value = _apps.value.map { app ->
            if (app.packageName == packageName) app.copy(note = note, tags = normalizedTags) else app
        }
    }

    fun updateNote(packageName: String, note: String) {
        val existingTags = _tagsMap.value[packageName] ?: ""
        updateAppMetadata(packageName, note, existingTags)
    }

    fun updateTags(packageName: String, tags: String) {
        val existingNote = _notesMap.value[packageName] ?: ""
        updateAppMetadata(packageName, existingNote, tags)
    }

    // --- Push / Pull ---

    fun pushToRemote(remoteUrl: String, username: String, password: String, force: Boolean) {
        viewModelScope.launch {
            _toastMessage.value = getApplication<Application>().getString(R.string.pushing)
            try {
                val ignoreSsl = isIgnoreSslErrors()
                val result = gitManager.pushToRemote(remoteUrl, username, password, force, ignoreSsl)
                if (result.isSuccess) {
                    _toastMessage.value = getApplication<Application>().getString(R.string.push_success)
                    loadHistory()
                } else {
                    _toastMessage.value = getApplication<Application>().getString(R.string.push_error, result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.push_error, e.message)
            }
        }
    }

    fun pullFromRemote(remoteUrl: String, username: String, password: String, force: Boolean) {
        viewModelScope.launch {
            _toastMessage.value = getApplication<Application>().getString(R.string.pulling)
            try {
                val ignoreSsl = isIgnoreSslErrors()
                val result = gitManager.pullFromRemote(remoteUrl, username, password, force, ignoreSsl)
                if (result.isSuccess) {
                    _toastMessage.value = getApplication<Application>().getString(R.string.pull_success)
                    loadHistory()
                } else {
                    _toastMessage.value = getApplication<Application>().getString(R.string.pull_failed, result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.pull_error, e.message)
            }
        }
    }

    fun pushWithSavedConfig(force: Boolean = false) {
        val (url, user, pass) = getRemoteConfig()
        if (url.isBlank()) {
            showToast(getApplication<Application>().getString(R.string.error_prefix, "Remote URL not set"))
            return
        }
        pushToRemote(url, user, pass, force)
    }

    fun pullWithSavedConfig(force: Boolean = false) {
        val (url, user, pass) = getRemoteConfig()
        if (url.isBlank()) {
            showToast(getApplication<Application>().getString(R.string.error_prefix, "Remote URL not set"))
            return
        }
        pullFromRemote(url, user, pass, force)
    }

    // --- Debounce ---

    fun getDebounceCount(): Int = PackageChangeReceiver.getCounter(getApplication())
    fun resetDebounceCounter() = PackageChangeReceiver.resetCounter(getApplication())

    fun getDebounceThreshold(): Int {
        return getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_DEBOUNCE, Context.MODE_PRIVATE)
            .getInt(KEY_DEBOUNCE_THRESHOLD, 5)
    }

    fun setDebounceThreshold(threshold: Int) {
        getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_DEBOUNCE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DEBOUNCE_THRESHOLD, threshold).apply()
    }

    fun getAutoScanOnStart(): Boolean {
        return getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_SCAN, false)
    }

    fun setAutoScanOnStart(enabled: Boolean) {
        getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_SCAN, enabled).apply()
    }

    // --- Git Identity ---

    private val gitIdentityPrefs by lazy {
        getApplication<Application>().getSharedPreferences(PREFS_GIT_IDENTITY, Context.MODE_PRIVATE)
    }

    fun saveGitIdentity(authorName: String, authorEmail: String) {
        remotePrefs.edit()
            .putString(KEY_GIT_AUTHOR_NAME, authorName)
            .putString(KEY_GIT_AUTHOR_EMAIL, authorEmail)
            .apply()
    }

    fun getGitIdentity(): Pair<String, String> {
        return Pair(
            remotePrefs.getString(KEY_GIT_AUTHOR_NAME, "") ?: "",
            remotePrefs.getString(KEY_GIT_AUTHOR_EMAIL, "") ?: ""
        )
    }

    // --- Remote Config ---

    fun saveRemoteConfig(url: String, username: String, password: String) {
        remotePrefs.edit()
            .putString(KEY_REMOTE_URL, url.trim())
            .putString(KEY_REMOTE_USER, username.trim())
            .putString(KEY_REMOTE_PASS, password.trim())
            .apply()
    }

    fun getRemoteConfig(): Triple<String, String, String> {
        return Triple(
            remotePrefs.getString(KEY_REMOTE_URL, "") ?: "",
            remotePrefs.getString(KEY_REMOTE_USER, "") ?: "",
            remotePrefs.getString(KEY_REMOTE_PASS, "") ?: ""
        )
    }

    fun isIgnoreSslErrors(): Boolean {
        return remotePrefs.getBoolean(KEY_IGNORE_SSL, false)
    }
    fun setIgnoreSslErrors(ignore: Boolean) {
        remotePrefs.edit().putBoolean(KEY_IGNORE_SSL, ignore).apply()
    }

    fun resetRepository() {
        viewModelScope.launch {
            gitManager.resetRepository().onSuccess {
                _isFirstLaunch.value = true
                getApplication<Application>().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
                _apps.value = emptyList()
                _commits.value = emptyList()
                showToast(getApplication<Application>().getString(R.string.reset_success))
            }.onFailure {
                showToast(getApplication<Application>().getString(R.string.error_prefix, it.message))
            }
        }
    }

    // --- Restore Helper ---

    fun generateRestoreScript(removedApps: List<AppInfo>): String {
        return buildString {
            appendLine("# ADB Batch Install Script")
            appendLine("# Generated: " + System.currentTimeMillis())
            appendLine()
            for (app in removedApps) {
                appendLine("# " + app.appName + " (" + app.packageName + ")")
                appendLine("# Source: " + app.installerPackageName.ifEmpty { "unknown" })
                appendLine("# Last version: " + app.versionName + " (" + app.versionCode + ")")
                appendLine("# adb install " + app.packageName + ".apk")
                appendLine()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        getApplication<Application>()
            .getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_SYSTEM_APPS, show).apply()
    }

    fun toggleUserApps(show: Boolean) {
        _showUserApps.value = show
        getApplication<Application>()
            .getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_USER_APPS, show).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: AppSortOrder) {
        _sortOrder.value = order
    }

    fun setGrouping(grouping: AppGrouping) {
        _grouping.value = grouping
    }

    override fun onCleared() {
        super.onCleared()
        gitManager.close()
    }
}
