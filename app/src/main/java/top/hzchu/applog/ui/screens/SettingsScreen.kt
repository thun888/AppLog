package top.hzchu.applog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import top.hzchu.applog.R
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToBranches: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (remoteUrl, remoteUser, remotePass) = viewModel.getRemoteConfig()
    var url by remember { mutableStateOf(remoteUrl) }
    var username by remember { mutableStateOf(remoteUser) }
    var password by remember { mutableStateOf(remotePass) }
    var threshold by remember { mutableStateOf(viewModel.getDebounceThreshold().toString()) }
    val debounceCount = viewModel.getDebounceCount()
    var autoScan by remember { mutableStateOf(viewModel.getAutoScanOnStart()) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_settings)) })
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
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.branch_management)) },
                leadingContent = { Icon(Icons.Filled.AccountTree, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToBranches() }
            )
            
            HorizontalDivider()

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

            // Debounce Settings
            Text(stringResource(R.string.debounce_settings), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.current_counter, debounceCount, threshold.toIntOrNull() ?: 0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it },
                label = { Text(stringResource(R.string.notification_threshold)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val t = threshold.toIntOrNull() ?: 5
                viewModel.setDebounceThreshold(t)
                threshold = t.toString()
            }) {
                Text(stringResource(R.string.save_threshold))
            }

            // Remote Config
            Text(stringResource(R.string.remote_repository), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.remote_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_token)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.saveRemoteConfig(url, username, password)
            }) {
                Text(stringResource(R.string.save_remote_config))
            }

            // Push / Pull
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pushToRemote(url, username, password, force = false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.push))
                }
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pullFromRemote(url, username, password, force = false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.pull))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pushToRemote(url, username, password, force = true)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.force_push))
                }
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pullFromRemote(url, username, password, force = true)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.force_pull))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
