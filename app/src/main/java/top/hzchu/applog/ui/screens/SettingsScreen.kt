package top.hzchu.applog.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val (remoteUrl, remoteUser, remotePass) = viewModel.getRemoteConfig()
    var url by remember { mutableStateOf(remoteUrl) }
    var username by remember { mutableStateOf(remoteUser) }
    var password by remember { mutableStateOf(remotePass) }
    var threshold by remember { mutableStateOf(viewModel.getDebounceThreshold().toString()) }
    val debounceCount = viewModel.getDebounceCount()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Settings") })
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
            // Debounce Settings
            Text("Debounce Settings", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Current counter: " + debounceCount + " / " + threshold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it },
                label = { Text("Notification threshold") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val t = threshold.toIntOrNull() ?: 5
                viewModel.setDebounceThreshold(t)
                threshold = t.toString()
            }) {
                Text("Save Threshold")
            }

            // Remote Config
            Text("Remote Repository", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Remote URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password / Token") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.saveRemoteConfig(url, username, password)
            }) {
                Text("Save Remote Config")
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
                    Text("Push")
                }
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pullFromRemote(url, username, password, force = false)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pull")
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
                    Text("Force Push")
                }
                Button(
                    onClick = {
                        viewModel.saveRemoteConfig(url, username, password)
                        viewModel.pullFromRemote(url, username, password, force = true)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force Pull")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
