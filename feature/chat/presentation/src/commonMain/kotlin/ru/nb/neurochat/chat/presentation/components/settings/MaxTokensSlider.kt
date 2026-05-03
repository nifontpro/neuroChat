package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_max_tokens
import ru.nb.neurochat.chat.presentation.generated.resources.label_max_tokens_unlimited

private const val MAX_TOKENS_RANGE = 8192f
private const val STEPS = 63

/** Слайдер ограничения длины ответа модели. 0 = без ограничений (null в ApiSettings).
 * Диапазон 0..8192 с шагом 128 (64 шага).
 * @param value текущее значение (null или 0 = без ограничений)
 * @param onChange callback при изменении значения
 */
@Composable
internal fun MaxTokensSlider(
    value: Int?,
    onChange: (Int?) -> Unit,
) {
    Column {
        val unlimitedLabel = stringResource(Res.string.label_max_tokens_unlimited)
        val displayValue = if (value == null || value == 0) unlimitedLabel else value.toString()
        Text(
            text = stringResource(Res.string.label_max_tokens, displayValue),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = (value ?: 0).toFloat(),
            onValueChange = { raw ->
                val rounded = (raw / 128f).toInt() * 128
                onChange(if (rounded == 0) null else rounded)
            },
            valueRange = 0f..MAX_TOKENS_RANGE,
            steps = STEPS,
        )
    }
}
