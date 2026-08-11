package top.hzchu.applog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
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
fun DiffItemAdded(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
fun DiffItemRemoved(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
fun DiffItemUpdated(
    old: AppInfo,
    new: AppInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val versionChanged = old.versionCode != new.versionCode || old.versionName != new.versionName
        val noteChanged = old.note != new.note

        Text(
            text = if (versionChanged) {
                if (new.versionCode > old.versionCode) "\u2191" else "\u2193"
            } else "~",
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
            if (versionChanged) {
                Text(
                    text = "v${old.versionName} \u2192 v${new.versionName}",
                    color = DiffUpdatedColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (noteChanged) {
                Text(
                    text = if (old.note.isEmpty()) {
                        stringResource(R.string.note_added, new.note)
                    } else if (new.note.isEmpty()) {
                        stringResource(R.string.note_removed_diff, old.note)
                    } else {
                        "${old.note} \u2192 ${new.note}"
                    },
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DiffItemNoteChanged(
    old: AppInfo,
    new: AppInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
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
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = new.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (old.note.isEmpty()) {
                    stringResource(R.string.note_added, new.note)
                } else if (new.note.isEmpty()) {
                    stringResource(R.string.note_removed_diff, old.note)
                } else {
                    "${old.note} \u2192 ${new.note}"
                },
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DiffItemTagsChanged(
    old: AppInfo,
    new: AppInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
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
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = new.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (old.tags.isEmpty()) {
                    stringResource(R.string.tags_added, new.tags)
                } else if (new.tags.isEmpty()) {
                    stringResource(R.string.tags_removed_diff, old.tags)
                } else {
                    "${old.tags} \u2192 ${new.tags}"
                },
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
