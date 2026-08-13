package top.hzchu.applog.ui.utils

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
fun pluralStringResource(
    @PluralsRes resId: Int,
    quantity: Int,
    vararg formatArgs: Any? = emptyArray()
): String {
    val context = LocalContext.current
    return context.resources.getQuantityString(resId, quantity, *formatArgs)
}
