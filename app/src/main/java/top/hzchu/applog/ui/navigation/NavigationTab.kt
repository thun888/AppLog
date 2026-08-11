package top.hzchu.applog.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import top.hzchu.applog.R

enum class NavigationTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    APPS(R.string.tab_apps, Icons.Filled.Apps),
    HISTORY(R.string.tab_history, Icons.Filled.History),
    DIFF(R.string.tab_diff, Icons.Filled.Compare),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings)
}
