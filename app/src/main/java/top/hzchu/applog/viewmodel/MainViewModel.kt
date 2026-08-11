package top.hzchu.applog.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val appScanner = AppScanner(application)
    val gitManager = GitManager(application)

    // --- UI State ---

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _commits = MutableStateFlow<List<CommitInfo>>(emptyList())
    val commits: StateFlow<List<CommitInfo>> = _commits.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _diffResult = MutableStateFlow<DiffResult?>(null)
    val diffResult: StateFlow<DiffResult?> = _diffResult.asStateFlow()

    private val _isComputingDiff = MutableStateFlow(false)
    val isComputingDiff: StateFlow<Boolean> = _isComputingDiff.asStateFlow()

    private val _selectedCommit1 = MutableStateFlow<String?>(null)
    val selectedCommit1: StateFlow<String?> = _selectedCommit1.asStateFlow()

    private val _selectedCommit2 = MutableStateFlow<String?>(null)
    val selectedCommit2: StateFlow<String?> = _selectedCommit2.asStateFlow()

    private val _showCommitDialog = MutableStateFlow(false)
    val showCommitDialog: StateFlow<Boolean> = _showCommitDialog.asStateFlow()

    private val _pendingAutoMessage = MutableStateFlow("")
    val pendingAutoMessage: StateFlow<String> = _pendingAutoMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Notes ---

    private val notesPrefs =
        application.getSharedPreferences("applog_notes", Context.MODE_PRIVATE)

    private val _notesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val notesMap: StateFlow<Map<String, String>> = _notesMap.asStateFlow()

    init {
        loadNotes()
        viewModelScope.launch {
            gitManager.init()
            loadHistory()
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
                val updated = _apps.value.count { app ->
                    val old = lastMap[app.packageName]
                    old != null && (old.versionCode != app.versionCode ||
                            old.versionName != app.versionName)
                }

                if (added == 0 && removed == 0 && updated == 0) {
                    _toastMessage.value = getApplication<Application>().getString(R.string.no_changes)
                    return@launch
                }

                _pendingAutoMessage.value = gitManager.generateCommitMessage(added, removed, updated)
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
                val result = gitManager.commitSnapshot(currentContent, finalMessage)
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
            _isLoadingHistory.value = true
            gitManager.getCommitHistory().onSuccess {
                _commits.value = it
            }.onFailure {
                _commits.value = emptyList()
            }
            _isLoadingHistory.value = false
        }
    }

    fun selectCommitForDiff(commitId: String, slot: Int) {
        if (slot == 1) _selectedCommit1.value = commitId
        else _selectedCommit2.value = commitId
    }

    fun computeDiff() {
        val commit1 = _selectedCommit1.value ?: run {
            _toastMessage.value = getApplication<Application>().getString(R.string.select_first_commit)
            return
        }
        val commit2 = _selectedCommit2.value ?: run {
            _toastMessage.value = getApplication<Application>().getString(R.string.select_second_commit)
            return
        }
        viewModelScope.launch {
            _isComputingDiff.value = true
            try {
                val content1 = gitManager.getSnapshotContent(commit1).getOrThrow()
                val content2 = gitManager.getSnapshotContent(commit2).getOrThrow()
                val apps1 = AppListSerializer.deserialize(content1)
                val apps2 = AppListSerializer.deserialize(content2)
                val map1 = AppListSerializer.buildAppMap(apps1)
                val map2 = AppListSerializer.buildAppMap(apps2)

                val added = apps2.filter { it.packageName !in map1 }
                val removed = apps1.filter { it.packageName !in map2 }
                val updated = mutableListOf<Pair<AppInfo, AppInfo>>()
                val noteChanged = mutableListOf<Pair<AppInfo, AppInfo>>()

                for (app2 in apps2) {
                    val app1 = map1[app2.packageName] ?: continue
                    if (app1.versionCode != app2.versionCode ||
                        app1.versionName != app2.versionName ||
                        app1.signatureSha256 != app2.signatureSha256) {
                        updated.add(app1 to app2)
                    } else if (app1.note != app2.note) {
                        noteChanged.add(app1 to app2)
                    }
                }
                _diffResult.value = DiffResult(
                    added = added, removed = removed,
                    updated = updated, noteChanged = noteChanged
                )
            } catch (e: Exception) {
                _toastMessage.value = getApplication<Application>().getString(R.string.diff_failed, e.message)
            } finally {
                _isComputingDiff.value = false
            }
        }
    }

    fun computeDiffWithCurrent() {
        val commitId = _selectedCommit1.value ?: run {
            _toastMessage.value = getApplication<Application>().getString(R.string.select_historical_commit)
            return
        }
        viewModelScope.launch {
            _isComputingDiff.value = true
            try {
                val content1 = gitManager.getSnapshotContent(commitId).getOrThrow()
                val currentApps = withContext(Dispatchers.IO) { appScanner.scanAllApps() }
                val apps1 = AppListSerializer.deserialize(content1)
                val map1 = AppListSerializer.buildAppMap(apps1)
                val map2 = AppListSerializer.buildAppMap(currentApps)
                val added = currentApps.filter { it.packageName !in map1 }
                val removed = apps1.filter { it.packageName !in map2 }
                val updated = mutableListOf<Pair<AppInfo, AppInfo>>()
                for (app2 in currentApps) {
                    val app1 = map1[app2.packageName] ?: continue
                    if (app1.versionCode != app2.versionCode ||
                        app1.versionName != app2.versionName) {
                        updated.add(app1 to app2)
                    }
                }
                _diffResult.value = DiffResult(
                    added = added, removed = removed, updated = updated
                )
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
        val json = notesPrefs.getString("notes_json", "{}") ?: "{}"
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
        notesPrefs.edit().putString("notes_json", json).apply()
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
            .getSharedPreferences("applog_debounce", Context.MODE_PRIVATE)
            .getInt("debounce_threshold", 5)
    }

    fun setDebounceThreshold(threshold: Int) {
        getApplication<android.app.Application>()
            .getSharedPreferences("applog_debounce", Context.MODE_PRIVATE)
            .edit().putInt("debounce_threshold", threshold).apply()
    }

    // --- Remote Config ---

    fun saveRemoteConfig(url: String, username: String, password: String) {
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("applog_remote", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("remote_url", url)
            .putString("remote_username", username)
            .putString("remote_password", password)
            .apply()
    }

    fun getRemoteConfig(): Triple<String, String, String> {
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("applog_remote", Context.MODE_PRIVATE)
        return Triple(
            prefs.getString("remote_url", "") ?: "",
            prefs.getString("remote_username", "") ?: "",
            prefs.getString("remote_password", "") ?: ""
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

    override fun onCleared() {
        super.onCleared()
        gitManager.close()
    }
}
