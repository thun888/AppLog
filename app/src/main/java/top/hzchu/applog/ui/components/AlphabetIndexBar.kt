package top.hzchu.applog.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetIndexBar(
    alphabet: List<String>,
    onIndexSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var totalHeight by remember { mutableFloatStateOf(0f) }
    val itemHeight = if (alphabet.isNotEmpty() && totalHeight > 0) totalHeight / alphabet.size else 0f

    Column(
        modifier = modifier
            .onGloballyPositioned {
                totalHeight = it.size.height.toFloat()
            }
            .pointerInput(alphabet) {
                detectTapGestures { offset ->
                    if (itemHeight > 0) {
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.size - 1)
                        onIndexSelected(alphabet[index])
                    }
                }
            }
            .pointerInput(alphabet) {
                detectDragGestures { change, _ ->
                    if (itemHeight > 0) {
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.size - 1)
                        onIndexSelected(alphabet[index])
                    }
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { char ->
            Text(
                text = char,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}
