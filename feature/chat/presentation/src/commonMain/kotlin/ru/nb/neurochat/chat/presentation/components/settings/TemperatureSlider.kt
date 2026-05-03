package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature
import ru.nb.neurochat.chat.presentation.generated.resources.label_temperature_default
import kotlin.math.round

private const val DEFAULT_TEMPERATURE = 0.7

/** Слайдер температуры 0.0–2.0 с шагом 0.1 (19 шагов).
 * @param value текущее значение (null = дефолт провайдера)
 * @param onChange callback при изменении значения
 */
@Composable
internal fun TemperatureSlider(
    value: Double?,
    onChange: (Double) -> Unit,
) {
    Column {
        val defaultLabel = stringResource(Res.string.label_temperature_default)
        val displayValue by remember(value, defaultLabel) {
            derivedStateOf {
                value?.let { (round(it * 10) / 10.0).toString() } ?: defaultLabel
            }
        }
        Text(
            text = stringResource(Res.string.label_temperature, displayValue),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = (value ?: DEFAULT_TEMPERATURE).toFloat(),
            onValueChange = { onChange(round(it * 10) / 10.0) },
            valueRange = 0f..2f,
            steps = 19,
        )
    }
}
