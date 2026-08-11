package top.hzchu.applog.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.hzchu.applog.model.AppInfo

@Composable
fun AppIcon(
    packageName: String,
    appType: AppInfo.AppType,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    val context = LocalContext.current
    val icon by produceState<Drawable?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }
    }
    
    val placeholderPainter = rememberVectorPainter(
        image = if (appType == AppInfo.AppType.SYSTEM)
            Icons.Filled.Android else Icons.Filled.Smartphone
    )

    Box(
        modifier = modifier
            .size(iconSize)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(icon)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter
        )
    }
}
