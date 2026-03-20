package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.nb.neurochat.chat.presentation.ChatAction
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.domain.model.AVAILABLE_MODELS

@Composable
fun SettingsPanel(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            // Connection status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (state.isConnected) "Онлайн" else "Нет сети",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
            }

            item { HorizontalDivider() }

            // Model selection
            item {
                Text(
                    text = "Модель",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            items(AVAILABLE_MODELS) { model ->
                val isSelected = model == state.currentModel
                Text(
                    text = model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(ChatAction.OnSelectModel(model)) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                )
            }

            item { HorizontalDivider() }

            // Temperature
            item {
                Column {
                    Text(
                        text = "Температура: ${state.currentTemperature?.let { ((it * 10).toInt() / 10.0).toString() } ?: "по умолчанию"}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = (state.currentTemperature ?: 0.7).toFloat(),
                        onValueChange = { onAction(ChatAction.OnTemperatureChange(it.toDouble())) },
                        valueRange = 0f..2f,
                        steps = 19,
                    )
                }
            }

            item { HorizontalDivider() }

            // Thinking mode
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Режим мышления",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Switch(
                        checked = state.thinkingEnabled,
                        onCheckedChange = { onAction(ChatAction.OnThinkingToggle(it)) },
                    )
                }
            }

            item { HorizontalDivider() }

            // System prompt
            item {
                var promptText by remember(state.systemPrompt) {
                    mutableStateOf(state.systemPrompt ?: "")
                }
                Column {
                    Text(
                        text = "Системный промпт",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = {
                            promptText = it
                            onAction(ChatAction.OnSystemPromptChange(it.ifBlank { null }))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("Ты умный ассистент...") },
                    )
                }
            }
        }
    }
}
