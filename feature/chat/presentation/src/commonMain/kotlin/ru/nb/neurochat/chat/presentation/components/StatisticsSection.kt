package ru.nb.neurochat.chat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.ChatState
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_chars
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_completion_tokens
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_duration
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_empty
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_prompt_tokens
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_section
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_session_total
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_speed
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_tokens
import ru.nb.neurochat.chat.presentation.generated.resources.label_stats_total_tokens
import ru.nb.neurochat.domain.model.ChatRole

// Блок статистики последнего ответа модели + суммарные токены за сессию.
// Локальная статистика (time/tokens/speed/chars) берётся из последнего Assistant-сообщения,
// данные по usage (prompt/completion/total) — из state.lastUsage (поле usage в SSE от OpenAI).
@Composable
fun StatisticsSection(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val valueColor = MaterialTheme.colorScheme.onSurfaceVariant

    val lastResponseStats = state.messages
        .lastOrNull { it.role == ChatRole.Assistant }
        ?.statistics

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.label_stats_section),
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
        )

        // До первого ответа показываем понятный placeholder, а не пустой блок.
        if (lastResponseStats == null && state.lastUsage == null) {
            Text(
                text = stringResource(Res.string.label_stats_empty),
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
            )
            return@Column
        }

        // Локальные метрики: считаются на клиенте пока идёт стриминг.
        lastResponseStats?.let { stats ->
            val durationSec = kotlin.math.round(stats.durationMs / 100.0) / 10.0
            val speed = kotlin.math.round(stats.tokensPerSecond * 10.0) / 10.0
            StatLine(labelArg = durationSec.toString(), resId = Res.string.label_stats_duration, color = valueColor)
            StatLine(labelArg = stats.tokenCount.toString(), resId = Res.string.label_stats_tokens, color = valueColor)
            StatLine(labelArg = speed.toString(), resId = Res.string.label_stats_speed, color = valueColor)
            StatLine(labelArg = stats.charCount.toString(), resId = Res.string.label_stats_chars, color = valueColor)
        }

        // Токены от API (точные значения от провайдера).
        state.lastUsage?.let { usage ->
            StatLine(labelArg = usage.promptTokens.toString(), resId = Res.string.label_stats_prompt_tokens, color = valueColor)
            StatLine(labelArg = usage.completionTokens.toString(), resId = Res.string.label_stats_completion_tokens, color = valueColor)
            StatLine(labelArg = usage.totalTokens.toString(), resId = Res.string.label_stats_total_tokens, color = valueColor)
        }

        StatLine(
            labelArg = state.sessionTotalTokens.toString(),
            resId = Res.string.label_stats_session_total,
            color = valueColor,
        )
    }
}

@Composable
private fun StatLine(
    labelArg: String,
    resId: org.jetbrains.compose.resources.StringResource,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = stringResource(resId, labelArg),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}
