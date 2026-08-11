package top.hzchu.applog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import top.hzchu.applog.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagChipDisplay(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) {
        Text(
            text = "无",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = {},
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagChipInput(
    tags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    allExistingTags: List<String>,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    var inputText by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    // 本次会话中新增的标签（allExistingTags 中没有的），归入建议池
    var extraTags by remember { mutableStateOf(setOf<String>()) }

    // 切换编辑目标时清空输入
    LaunchedEffect(tags) {
        inputText = ""
        showDropdown = false
    }

    val suggestions = remember(inputText, tags, allExistingTags, extraTags) {
        if (inputText.isBlank()) emptyList()
        else (allExistingTags.toSet() + extraTags)
            .filter { it.contains(inputText, ignoreCase = true) && it !in tags }
            .sortedBy { it.lowercase() }
            .take(6)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                if (newValue.contains(",")) {
                    val parts = newValue.split(",")
                    val processed = mutableListOf<String>()
                    for (part in parts) {
                        val trimmed = part.trim()
                        if (trimmed.isNotEmpty() && trimmed !in tags && trimmed !in processed) {
                            processed.add(trimmed)
                            if (trimmed !in allExistingTags) extraTags += trimmed
                        }
                    }
                    if (processed.isNotEmpty()) {
                        onTagsChanged((tags + processed).sortedBy { it.lowercase() })
                    }
                    inputText = ""
                    showDropdown = false
                } else {
                    inputText = newValue
                    showDropdown = newValue.isNotEmpty()
                }
            },
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.tags_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty() && trimmed !in tags) {
                        onTagsChanged((tags + trimmed).sortedBy { it.lowercase() })
                        if (trimmed !in allExistingTags) extraTags += trimmed
                    }
                    inputText = ""
                    showDropdown = false
                }
            )
        )

        DropdownMenu(
            expanded = showDropdown && suggestions.isNotEmpty(),
            onDismissRequest = { showDropdown = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        if (suggestion !in tags) {
                            onTagsChanged((tags + suggestion).sortedBy { it.lowercase() })
                            if (suggestion !in allExistingTags) extraTags += suggestion
                        }
                        inputText = ""
                        showDropdown = false
                    }
                )
            }
        }
    }

    if (tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.remove_tag, tag),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    onTagsChanged((tags - tag).sortedBy { it.lowercase() })
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}
