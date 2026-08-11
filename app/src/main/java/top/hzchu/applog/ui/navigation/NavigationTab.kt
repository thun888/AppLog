package top.hzchu.applog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationTab(
    val label: String,
    val icon: ImageVector
) {
    APPS("Apps", Icons.Filled.Apps),
    HISTORY("History", Icons.Filled.History),
    DIFF("Diff", Icons.Filled.Compare),
    SETTINGS("Settings", Icons.Filled.Settings)
}
