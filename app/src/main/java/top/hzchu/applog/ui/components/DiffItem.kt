package top.hzchu.applog.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import top.hzchu.applog.R
import top.hzchu.applog.model.AppInfo
import top.hzchu.applog.model.DiffResult

val DiffAddedColor = Color(0xFF2E7D32)
val DiffRemovedColor = Color(0xFFC62828)
val DiffUpdatedColor = Color(0xFF1565C0)

@Composable
fun DiffItemAdded(app: AppInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+",
            color = DiffAddedColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(
            packageName = app.packageName,
            appType = app.appType,
            iconSize = 32.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = app.appName,
                color = DiffAddedColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = DiffAddedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.version_format, app.versionName, app.versionCode),
                color = DiffAddedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DiffItemRemoved(app: AppInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "-",
            color = DiffRemovedColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(
            packageName = app.packageName,
            appType = app.appType,
            iconSize = 32.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = app.appName,
                color = DiffRemovedColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = DiffRemovedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.version_was, app.versionName, app.versionCode),
                color = DiffRemovedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DiffItemUpdated(old: AppInfo, new: AppInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (new.versionCode > old.versionCode) "\u2191" else "\u2193",
            color = DiffUpdatedColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(
            packageName = new.packageName,
            appType = new.appType,
            iconSize = 32.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = new.appName,
                color = DiffUpdatedColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = new.packageName,
                color = DiffUpdatedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "v${old.versionName} \u2192 v${new.versionName}",
                color = DiffUpdatedColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
