package top.hzchu.applog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.hzchu.applog.R
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToBranches: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (remoteUrl, remoteUser, remotePass) = viewModel.getRemoteConfig()
    val (gitAuthor, gitEmail) = viewModel.getGitIdentity()
    var url by remember { mutableStateOf(remoteUrl) }
    var username by remember { mutableStateOf(remoteUser) }
    var password by remember { mutableStateOf(remotePass) }
    var passwordVisible by remember { mutableStateOf(false) }
    var authorName by remember { mutableStateOf(gitAuthor) }
    var authorEmail by remember { mutableStateOf(gitEmail) }
    var ignoreSsl by remember { mutableStateOf(viewModel.isIgnoreSslErrors()) }
    var threshold by remember { mutableStateOf(viewModel.getDebounceThreshold()) }
    val debounceCount = viewModel.getDebounceCount()
    var autoScan by remember { mutableStateOf(viewModel.getAutoScanOnStart()) }
    var pendingForceAction by remember { mutableStateOf<String?>(null) } // "push" or "pull"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.about_title))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Settings
            Text(stringResource(R.string.general_settings), style = MaterialTheme.typography.titleMedium)



            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.auto_scan_on_start))
                Switch(
                    checked = autoScan,
                    onCheckedChange = {
                        autoScan = it
                        viewModel.setAutoScanOnStart(it)
                    }
                )
            }

            HorizontalDivider()

            // Debounce Settings
            Text(stringResource(R.string.debounce_settings), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.current_counter, debounceCount, threshold),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.notification_threshold))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(
                        onClick = {
                            if (threshold > 1) {
                                threshold--
                                viewModel.setDebounceThreshold(threshold)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = threshold.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OutlinedIconButton(
                        onClick = {
                            if (threshold < 50) {
                                threshold++
                                viewModel.setDebounceThreshold(threshold)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }

            HorizontalDivider()

            // Git Identity
            Text(stringResource(R.string.git_identity), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = authorName,
                onValueChange = { authorName = it },
                label = { Text(stringResource(R.string.git_author_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            viewModel.saveGitIdentity(authorName, authorEmail)
                        }
                    }
            )
            OutlinedTextField(
                value = authorEmail,
                onValueChange = { authorEmail = it },
                label = { Text(stringResource(R.string.git_author_email)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            viewModel.saveGitIdentity(authorName, authorEmail)
                        }
                    }
            )

            HorizontalDivider()

            // Remote Config
            Text(stringResource(R.string.remote_repository), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.remote_url)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            viewModel.saveGitIdentity(authorName, authorEmail)
                            viewModel.saveRemoteConfig(url, username, password)
                        }
                    }
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            viewModel.saveGitIdentity(authorName, authorEmail)
                            viewModel.saveRemoteConfig(url, username, password)
                        }
                    }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_token)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            viewModel.saveGitIdentity(authorName, authorEmail)
                            viewModel.saveRemoteConfig(url, username, password)
                        }
                    },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.ignore_ssl), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = ignoreSsl,
                    onCheckedChange = {
                        ignoreSsl = it
                        viewModel.setIgnoreSslErrors(it)
                    }
                )
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.branch_management)) },
                leadingContent = { Icon(Icons.Filled.AccountTree, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToBranches() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveGitIdentity(authorName, authorEmail)
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pushToRemote(url, username, password, force = false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.push))
                }
                Button(
                    onClick = {
                        viewModel.saveGitIdentity(authorName, authorEmail)
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pullFromRemote(url, username, password, force = false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pull))
                }
            }
            
            HorizontalDivider()

            var showResetDialog by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { 
                    Text(
                        stringResource(R.string.reset_repository),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { showResetDialog = true }
            )

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text(stringResource(R.string.confirm_title)) },
                    text = { Text(stringResource(R.string.reset_confirm)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetRepository()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pendingForceAction = "push" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.force_push))
                }
                Button(
                    onClick = { pendingForceAction = "pull" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.force_pull))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (pendingForceAction != null) {
        AlertDialog(
            onDismissRequest = { pendingForceAction = null },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = {
                Text(
                    if (pendingForceAction == "push")
                        stringResource(R.string.confirm_force_push)
                    else
                        stringResource(R.string.confirm_force_pull)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val action = pendingForceAction
                        pendingForceAction = null
                        viewModel.saveGitIdentity(authorName, authorEmail)
                        viewModel.saveRemoteConfig(url, username, password)
                        if (action == "push") {
                            viewModel.pushToRemote(url, username, password, force = true)
                        } else {
                            viewModel.pullFromRemote(url, username, password, force = true)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingForceAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
