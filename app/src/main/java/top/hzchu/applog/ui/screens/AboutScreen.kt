package top.hzchu.applog.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import top.hzchu.applog.R
import top.hzchu.applog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestVersion by viewModel.latestVersion.collectAsState()
    val isChecking by viewModel.isCheckingUpdates.collectAsState()
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersion = "v${packageInfo.versionName}"
    val appIcon = context.packageManager.getApplicationIcon(context.packageName)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Icon using Coil to support AdaptiveIconDrawable
            AsyncImage(
                model = appIcon,
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.current_version, currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.github_repo)) },
                supportingContent = { Text("https://github.com/thun888/AppLog") },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thun888/AppLog"))
                        context.startActivity(intent)
                    }
            )
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isChecking) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.checking_updates))
            } else {
                val isNewer = latestVersion?.let { compareVersions(it, currentVersion) } ?: false

                if (latestVersion != null) {
                    Text(
                        text = if (isNewer) 
                            stringResource(R.string.new_version_available, latestVersion!!)
                        else 
                            stringResource(R.string.already_latest),
                        color = if (isNewer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isNewer) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (isNewer) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thun888/AppLog/releases/latest"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.update_now))
                    }
                } else {
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.check_updates))
                    }
                }
            }
        }
    }
}

/**
 * Compare vx.y.z versions. Returns true if latest > current.
 */
private fun compareVersions(latest: String, current: String): Boolean {
    val l = latest.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val c = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    
    for (i in 0 until minOf(l.size, c.size)) {
        if (l[i] > c[i]) return true
        if (l[i] < c[i]) return false
    }
    return l.size > c.size
}
