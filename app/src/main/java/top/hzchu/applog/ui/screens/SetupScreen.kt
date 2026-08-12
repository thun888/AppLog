package top.hzchu.applog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.hzchu.applog.R
import top.hzchu.applog.git.GitManager
import top.hzchu.applog.viewmodel.MainViewModel

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var remoteUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var initialBranch by remember { mutableStateOf(GitManager.DEFAULT_BRANCH) }
    var authorName by remember { mutableStateOf("") }
    var authorEmail by remember { mutableStateOf("") }
    var ignoreSsl by remember { mutableStateOf(false) }
    
    var showError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.setup_desc),
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.remote_repository), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = remoteUrl,
            onValueChange = { remoteUrl = it; showError = false },
            label = { Text(stringResource(R.string.remote_url)) },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && remoteUrl.isBlank(),
            placeholder = { Text("https://github.com/user/repo.git") }
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; showError = false },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && username.isBlank()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; showError = false },
            label = { Text(stringResource(R.string.password_token)) },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && password.isBlank(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        )

        OutlinedTextField(
            value = initialBranch,
            onValueChange = { initialBranch = it; showError = false },
            label = { Text(stringResource(R.string.setup_git_branch)) },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && initialBranch.isBlank()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.ignore_ssl), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = ignoreSsl, onCheckedChange = { ignoreSsl = it })
        }

        HorizontalDivider()
        
        Text(stringResource(R.string.git_identity) + " " + stringResource(R.string.optional_suffix), style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = authorName,
            onValueChange = { authorName = it },
            label = { Text(stringResource(R.string.git_author_name)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("AppLog") }
        )

        OutlinedTextField(
            value = authorEmail,
            onValueChange = { authorEmail = it },
            label = { Text(stringResource(R.string.git_author_email)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("applog@local") }
        )

        if (showError) {
            Text(
                text = stringResource(R.string.setup_error_empty),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (remoteUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && initialBranch.isNotBlank()) {
                    viewModel.completeOnboarding(
                        url = remoteUrl,
                        username = username,
                        password = password,
                        initialBranch = initialBranch,
                        authorName = authorName,
                        authorEmail = authorEmail,
                        ignoreSsl = ignoreSsl
                    )
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.setup_start))
        }
    }
}
