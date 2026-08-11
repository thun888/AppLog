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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NOTES = "applog_notes"
        private const val PREFS_REMOTE = "applog_remote_secure"
        private const val PREFS_DEBOUNCE = "applog_debounce"
        private const val PREFS_SETTINGS = "applog_settings"
        private const val PREFS_GIT_IDENTITY = "applog_git_identity"

        private const val KEY_NOTES_JSON = "notes_json"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_REMOTE_USER = "remote_username"
        private const val KEY_REMOTE_PASS = "remote_password"
        private const val KEY_DEBOUNCE_THRESHOLD = "debounce_threshold"
        private const val KEY_AUTO_SCAN = "auto_scan_on_start"
        private const val KEY_GIT_AUTHOR_NAME = "git_author_name"
        private const val KEY_GIT_AUTHOR_EMAIL = "git_author_email"
    }

    private val appScanner = AppScanner(application)
    val gitManager = GitManager(application)

    // --- UI State ---

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _showSystemApps = MutableStateFlow(true)
    val showSystemApps = _showSystemApps.asStateFlow()

    private val _showUserApps = MutableStateFlow(true)
    val showUserApps = _showUserApps.asStateFlow()

    val filteredApps = combine(_apps, _showSystemApps, _showUserApps) { apps, showSystem, showUser ->
        apps.filter { app ->
            if (app.appType == AppInfo.AppType.SYSTEM) showSystem else showUser
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

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

    // --- Notes ---

    private val notesPrefs =
        application.getSharedPreferences(PREFS_NOTES, Context.MODE_PRIVATE)

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

    init {
        loadNotes()
        viewModelScope.launch {
            gitManager.init()
            loadBranches()
            loadHistory()
            if (getAutoScanOnStart()) {
                scanApps()
            }
        }
    }

    // --- Scanning ---

    fun scanApps() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val scanned = withContext(Dispatchers.IO) { appScanner.scanAllApps() }
                val notes = _notesMap.value
                _apps.value = scanned.map { app ->
                    app.copy(note = notes[app.packageName] ?: "")
                }
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.scan_failed, e.message)
            } finally {
                _isScanning.value = false
            }
        }
    }

    // --- Git Operations ---

    fun prepareCommit() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                if (_apps.value.isEmpty()) {
                    val scanned = withContext(Dispatchers.IO) { appScanner.scanAllApps() }
                    _apps.value = scanned.map { app ->
                        app.copy(note = _notesMap.value[app.packageName] ?: "")
                    }
                }
                val lastContent = gitManager.getSnapshotContent().getOrDefault("")
                val lastApps = AppListSerializer.deserialize(lastContent)
                val lastMap = AppListSerializer.buildAppMap(lastApps)
                val currentMap = AppListSerializer.buildAppMap(_apps.value)

                val added = _apps.value.count { it.packageName !in lastMap }
                val removed = lastApps.count { it.packageName !in currentMap }
                var updated = 0
                var noteChanged = 0

                for (app in _apps.value) {
                    val old = lastMap[app.packageName] ?: continue
                    if (old.versionCode != app.versionCode || 
                        old.versionName != app.versionName ||
                        old.signatureSha256 != app.signatureSha256) {
                        updated++
                    } else if (old.note != app.note) {
                        noteChanged++
                    }
                }

                if (added == 0 && removed == 0 && updated == 0 && noteChanged == 0) {
                    _toastMessage.value = getApplication<Application>().getString(R.string.no_changes)
                    return@launch
                }

                _pendingAutoMessage.value = gitManager.generateCommitMessage(added, removed, updated, noteChanged)
                _showCommitDialog.value = true
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.error_prefix, e.message)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun dismissCommitDialog() {
        _showCommitDialog.value = false
    }

    fun performCommit(customMessage: String) {
        val finalMessage = customMessage.ifBlank { _pendingAutoMessage.value }
        _showCommitDialog.value = false

        viewModelScope.launch {
            _isScanning.value = true
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
            } finally {
                _isScanning.value = false
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
                        scanned.map { app -> app.copy(note = notes[app.packageName] ?: "") }.also {
                            _apps.value = it
                        }
                    }

                    val headContent = gitManager.getSnapshotContent().getOrThrow()
                    val headApps = AppListSerializer.deserialize(headContent)
                    
                    val mapHead = AppListSerializer.buildAppMap(headApps)
                    val mapCurrent = AppListSerializer.buildAppMap(currentApps)

                    val added = currentApps.filter { it.packageName !in mapHead }
                    val removed = headApps.filter { it.packageName !in mapCurrent }
                    val updated = mutableListOf<Pair<AppInfo, AppInfo>>()
                    val noteChanged = mutableListOf<Pair<AppInfo, AppInfo>>()

                    for (appCurrent in currentApps) {
                        val appHead = mapHead[appCurrent.packageName] ?: continue
                        if (appHead.versionCode != appCurrent.versionCode || 
                            appHead.versionName != appCurrent.versionName ||
                            appHead.signatureSha256 != appCurrent.signatureSha256) {
                            updated.add(appHead to appCurrent)
                        } else if (appHead.note != appCurrent.note) {
                            noteChanged.add(appHead to appCurrent)
                        }
                    }

                    _currentDetailCommit.value = CommitInfo(
                        id = "CURRENT",
                        shortId = "CURRENT",
                        message = getApplication<Application>().getString(R.string.current_status),
                        author = "",
                        timestamp = System.currentTimeMillis()
                    )
                    _detailApps.value = currentApps
                    _detailDiffResult.value = DiffResult(
                        added = added, 
                        removed = removed, 
                        updated = updated,
                        noteChanged = noteChanged
                    )
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

                    val mapParent = AppListSerializer.buildAppMap(parentApps)
                    val mapCurrent = AppListSerializer.buildAppMap(currentApps)

                    val added = currentApps.filter { it.packageName !in mapParent }
                    val removed = parentApps.filter { it.packageName !in mapCurrent }
                    val updated = mutableListOf<Pair<AppInfo, AppInfo>>()
                    val noteChanged = mutableListOf<Pair<AppInfo, AppInfo>>()

                    for (appCurrent in currentApps) {
                        val appParent = mapParent[appCurrent.packageName] ?: continue
                        if (appParent.versionCode != appCurrent.versionCode || 
                            appParent.versionName != appCurrent.versionName ||
                            appParent.signatureSha256 != appCurrent.signatureSha256) {
                            updated.add(appParent to appCurrent)
                        } else if (appParent.note != appCurrent.note) {
                            noteChanged.add(appParent to appCurrent)
                        }
                    }

                    _currentDetailCommit.value = commit
                    _detailApps.value = currentApps
                    _detailDiffResult.value = DiffResult(
                        added = added, 
                        removed = removed, 
                        updated = updated,
                        noteChanged = noteChanged
                    )
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

    // --- Notes ---

    fun updateNote(packageName: String, note: String) {
        val currentNotes = _notesMap.value.toMutableMap()
        if (note.isBlank()) currentNotes.remove(packageName)
        else currentNotes[packageName] = note
        _notesMap.value = currentNotes
        saveNotes()
        _apps.value = _apps.value.map { app ->
            if (app.packageName == packageName) app.copy(note = note) else app
        }
    }

    private fun loadNotes() {
        val json = notesPrefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        try {
            val map = mutableMapOf<String, String>()
            val trimmed = json.trim().removeSurrounding("{", "}")
            if (trimmed.isNotBlank()) {
                for (entry in trimmed.split(",")) {
                    val kv = entry.split(":", limit = 2)
                    if (kv.size == 2) {
                        val k = kv[0].trim().removeSurrounding("\"")
                        val v = kv[1].trim().removeSurrounding("\"")
                        if (k.isNotEmpty()) map[k] = v
                    }
                }
            }
            _notesMap.value = map
        } catch (_: Exception) {
            _notesMap.value = emptyMap()
        }
    }

    private fun saveNotes() {
        val json = buildString {
            append("{")
            _notesMap.value.entries.forEachIndexed { i, entry ->
                if (i > 0) append(",")
                append("\"" + entry.key + "\":\"" + entry.value + "\"")
            }
            append("}")
        }
        notesPrefs.edit().putString(KEY_NOTES_JSON, json).apply()
    }

    // --- Push / Pull ---

    fun pushToRemote(remoteUrl: String, username: String, password: String, force: Boolean) {
        viewModelScope.launch {
            _toastMessage.value = getApplication<Application>().getString(R.string.pushing)
            try {
                val result = gitManager.pushToRemote(remoteUrl, username, password, force)
                _toastMessage.value = if (result.isSuccess) getApplication<Application>().getString(R.string.push_success) 
                    else getApplication<Application>().getString(R.string.push_error, result.exceptionOrNull()?.message)
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.push_error, e.message)
            }
        }
    }

    fun pullFromRemote(remoteUrl: String, username: String, password: String, force: Boolean) {
        viewModelScope.launch {
            _toastMessage.value = getApplication<Application>().getString(R.string.pulling)
            try {
                val result = gitManager.pullFromRemote(remoteUrl, username, password, force)
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
        gitIdentityPrefs.edit()
            .putString(KEY_GIT_AUTHOR_NAME, authorName)
            .putString(KEY_GIT_AUTHOR_EMAIL, authorEmail)
            .apply()
    }

    fun getGitIdentity(): Pair<String, String> {
        return Pair(
            gitIdentityPrefs.getString(KEY_GIT_AUTHOR_NAME, "") ?: "",
            gitIdentityPrefs.getString(KEY_GIT_AUTHOR_EMAIL, "") ?: ""
        )
    }

    // --- Remote Config ---

    fun saveRemoteConfig(url: String, username: String, password: String) {
        remotePrefs.edit()
            .putString(KEY_REMOTE_URL, url)
            .putString(KEY_REMOTE_USER, username)
            .putString(KEY_REMOTE_PASS, password)
            .apply()
    }

    fun getRemoteConfig(): Triple<String, String, String> {
        return Triple(
            remotePrefs.getString(KEY_REMOTE_URL, "") ?: "",
            remotePrefs.getString(KEY_REMOTE_USER, "") ?: "",
            remotePrefs.getString(KEY_REMOTE_PASS, "") ?: ""
        )
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
    }

    fun toggleUserApps(show: Boolean) {
        _showUserApps.value = show
    }

    override fun onCleared() {
        super.onCleared()
        gitManager.close()
    }
}
