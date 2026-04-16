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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.hint_system_prompt
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_all
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_window
import ru.nb.neurochat.chat.presentation.generated.resources.label_model
import ru.nb.neurochat.chat.presentation.generated.resources.label_reset_settings
import ru.nb.neurochat.chat.presentation.generated.resources.label_show_statistics
import ru.nb.neurochat.chat.presentation.generated.resources.label_system_prompt
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature_default
import ru.nb.neurochat.chat.presentation.generated.resources.label_thinking_mode
import ru.nb.neurochat.chat.presentation.generated.resources.settings
import ru.nb.neurochat.chat.presentation.generated.resources.status_offline
import ru.nb.neurochat.chat.presentation.generated.resources.status_online
import ru.nb.neurochat.domain.model.AVAILABLE_MODELS

// Панель настроек чата. Рендерится двумя способами (см. ChatScreen):
//   - широкий экран — колонка слева
//   - узкий экран — ModalBottomSheet
// Порядок блоков: статус сети, статистика, модель, температура, контекст, thinking,
// переключатель статистики, системный промпт, сброс настроек.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
	state: ChatState,
	onAction: (ChatAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	// В тёмной теме панель делаем темнее surface, в светлой — наоборот контрастнее.
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

			// Индикатор подключения (ConnectivityObserver — platform-specific).
			Text(
				text = stringResource(
					if (state.isConnected) Res.string.status_online else Res.string.status_offline,
				),
				style = MaterialTheme.typography.bodyMedium,
				color = if (state.isConnected)
					MaterialTheme.colorScheme.primary
				else
					MaterialTheme.colorScheme.error,
			)

			// Блок статистики — показывается только если пользователь включил соответствующий тумблер.
			if (state.showStatistics) {
				StatisticsSection(state = state)
				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
			}

			ModelSelector(
				currentModel = state.currentModel,
				onSelect = { onAction(ChatAction.OnSelectModel(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			TemperatureSlider(
				value = state.currentTemperature,
				onChange = { onAction(ChatAction.OnTemperatureChange(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			ContextWindowSlider(
				value = state.maxContextMessages,
				onChange = { onAction(ChatAction.OnMaxContextChange(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			// Thinking mode — расширенные рассуждения модели (budget задаётся на стороне VM).
			SwitchRow(
				label = stringResource(Res.string.label_thinking_mode),
				checked = state.thinkingEnabled,
				onCheckedChange = { onAction(ChatAction.OnThinkingToggle(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			SwitchRow(
				label = stringResource(Res.string.label_show_statistics),
				checked = state.showStatistics,
				onCheckedChange = { onAction(ChatAction.OnToggleStatistics(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			SystemPromptField(
				initial = state.systemPrompt,
				onChange = { onAction(ChatAction.OnSystemPromptChange(it)) },
				contentColor = contentColor,
			)

			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			// Сброс всех сохранённых настроек в дефолты из BuildKonfig.
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

// Выпадающий список моделей. Текущая выделяется цветом primary.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
	currentModel: String,
	onSelect: (String) -> Unit,
	contentColor: androidx.compose.ui.graphics.Color,
) {
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
			value = currentModel,
			onValueChange = {},
			readOnly = true,
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
							color = if (model == currentModel)
								MaterialTheme.colorScheme.primary
							else
								MaterialTheme.colorScheme.onSurface,
						)
					},
					onClick = {
						onSelect(model)
						expanded = false
					},
				)
			}
		}
	}
}

// Слайдер температуры 0.0–2.0 с шагом 0.1 (19 steps). null = дефолт провайдера (0.7).
@Composable
private fun TemperatureSlider(
	value: Double?,
	onChange: (Double) -> Unit,
	contentColor: androidx.compose.ui.graphics.Color,
) {
	Column {
		val displayValue = value
			?.let { (kotlin.math.round(it * 10) / 10.0).toString() }
			?: stringResource(Res.string.label_temperature_default)
		Text(
			text = stringResource(Res.string.label_temperature, displayValue),
			style = MaterialTheme.typography.titleSmall,
			color = contentColor,
		)
		Spacer(Modifier.height(4.dp))
		Slider(
			value = (value ?: 0.7).toFloat(),
			onValueChange = { onChange(kotlin.math.round(it * 10) / 10.0) },
			valueRange = 0f..2f,
			steps = 19,
		)
	}
}

// Ограничение контекста: сколько последних сообщений отправлять в запрос.
// 0 = все (без отсечения). Системный промпт добавляется независимо во VM.
@Composable
private fun ContextWindowSlider(
	value: Int,
	onChange: (Int) -> Unit,
	contentColor: androidx.compose.ui.graphics.Color,
) {
	Column {
		val label = if (value == 0)
			stringResource(Res.string.label_context_all)
		else
			value.toString()
		Text(
			text = stringResource(Res.string.label_context_window, label),
			style = MaterialTheme.typography.titleSmall,
			color = contentColor,
		)
		Spacer(Modifier.height(4.dp))
		Slider(
			value = value.toFloat(),
			onValueChange = { onChange(it.toInt()) },
			valueRange = 0f..50f,
			steps = 24,
		)
	}
}

// Обобщённая строка «подпись + Switch» для thinking/show-statistics и т.п.
@Composable
private fun SwitchRow(
	label: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	contentColor: androidx.compose.ui.graphics.Color,
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.titleSmall,
			color = contentColor,
		)
		Switch(checked = checked, onCheckedChange = onCheckedChange)
	}
}

// Поле системного промпта. Локальный state нужен чтобы UI не дёргался при каждом
// сохранении в DataStore (remember(key) — сброс при изменении state.systemPrompt извне, например reset).
@Composable
private fun SystemPromptField(
	initial: String?,
	onChange: (String?) -> Unit,
	contentColor: androidx.compose.ui.graphics.Color,
) {
	var text by remember(initial) { mutableStateOf(initial ?: "") }
	Column {
		Text(
			text = stringResource(Res.string.label_system_prompt),
			style = MaterialTheme.typography.titleSmall,
			color = contentColor,
		)
		Spacer(Modifier.height(4.dp))
		OutlinedTextField(
			value = text,
			onValueChange = {
				text = it
				onChange(it.ifBlank { null })
			},
			modifier = Modifier.fillMaxWidth(),
			minLines = 3,
			maxLines = 6,
			placeholder = { Text(stringResource(Res.string.hint_system_prompt)) },
		)
	}
}
