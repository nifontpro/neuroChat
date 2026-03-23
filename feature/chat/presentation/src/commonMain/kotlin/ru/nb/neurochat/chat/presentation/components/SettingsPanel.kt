package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.ChatAction
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.hint_system_prompt
import ru.nb.neurochat.chat.presentation.generated.resources.label_model
import ru.nb.neurochat.chat.presentation.generated.resources.label_system_prompt
import ru.nb.neurochat.chat.presentation.generated.resources.label_reset_settings
import ru.nb.neurochat.chat.presentation.generated.resources.label_show_statistics
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_all
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_window
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature_default
import ru.nb.neurochat.chat.presentation.generated.resources.label_thinking_mode
import ru.nb.neurochat.chat.presentation.generated.resources.settings
import ru.nb.neurochat.chat.presentation.generated.resources.status_offline
import ru.nb.neurochat.chat.presentation.generated.resources.status_online
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.domain.model.AVAILABLE_MODELS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark)
        MaterialTheme.colorScheme.surfaceContainerLowest
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

    val contentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        color = panelColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
            )

            // Connection status
            Text(
                text = stringResource(if (state.isConnected) Res.string.status_online else Res.string.status_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Model dropdown
            Text(
                text = stringResource(Res.string.label_model),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = state.currentModel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AVAILABLE_MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = model,
                                    color = if (model == state.currentModel)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                onAction(ChatAction.OnSelectModel(model))
                                expanded = false
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Temperature
            Column {
                Text(
                    text = stringResource(
                        Res.string.label_temperature,
                        state.currentTemperature?.let { (kotlin.math.round(it * 10) / 10.0).toString() }
                            ?: stringResource(Res.string.label_temperature_default),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = (state.currentTemperature ?: 0.7).toFloat(),
                    onValueChange = { onAction(ChatAction.OnTemperatureChange(kotlin.math.round(it * 10) / 10.0)) },
                    valueRange = 0f..2f,
                    steps = 19,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Context window
            Column {
                val contextLabel = if (state.maxContextMessages == 0)
                    stringResource(Res.string.label_context_all)
                else
                    state.maxContextMessages.toString()
                Text(
                    text = stringResource(Res.string.label_context_window, contextLabel),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = state.maxContextMessages.toFloat(),
                    onValueChange = { onAction(ChatAction.OnMaxContextChange(it.toInt())) },
                    valueRange = 0f..50f,
                    steps = 24,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Thinking mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.label_thinking_mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Switch(
                    checked = state.thinkingEnabled,
                    onCheckedChange = { onAction(ChatAction.OnThinkingToggle(it)) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Show statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.label_show_statistics),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Switch(
                    checked = state.showStatistics,
                    onCheckedChange = { onAction(ChatAction.OnToggleStatistics(it)) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // System prompt
            var promptText by remember(state.systemPrompt) {
                mutableStateOf(state.systemPrompt ?: "")
            }
            Column {
                Text(
                    text = stringResource(Res.string.label_system_prompt),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
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
                    placeholder = { Text(stringResource(Res.string.hint_system_prompt)) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Reset settings
            OutlinedButton(
                onClick = { onAction(ChatAction.OnResetSettings) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(text = stringResource(Res.string.label_reset_settings))
            }
        }
    }
}
