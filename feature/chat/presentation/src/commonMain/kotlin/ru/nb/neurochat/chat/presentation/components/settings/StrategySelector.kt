package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_context_strategy
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_branching
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_branching_hint
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_sliding_window
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_sliding_window_hint
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_sticky_facts
import ru.nb.neurochat.chat.presentation.generated.resources.strategy_sticky_facts_hint
import ru.nb.neurochat.domain.model.ContextStrategy

/** Переключатель стратегии управления контекстом (RadioGroup из 3 вариантов). */
@Composable
fun StrategySelector(
    selected: ContextStrategy,
    onSelect: (ContextStrategy) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.label_context_strategy),
            style = MaterialTheme.typography.titleMedium,
        )
        StrategyRadioRow(
            strategy = ContextStrategy.SLIDING_WINDOW,
            selected = selected,
            titleRes = Res.string.strategy_sliding_window,
            hintRes = Res.string.strategy_sliding_window_hint,
            onSelect = onSelect,
        )
        StrategyRadioRow(
            strategy = ContextStrategy.STICKY_FACTS,
            selected = selected,
            titleRes = Res.string.strategy_sticky_facts,
            hintRes = Res.string.strategy_sticky_facts_hint,
            onSelect = onSelect,
        )
        StrategyRadioRow(
            strategy = ContextStrategy.BRANCHING,
            selected = selected,
            titleRes = Res.string.strategy_branching,
            hintRes = Res.string.strategy_branching_hint,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun StrategyRadioRow(
    strategy: ContextStrategy,
    selected: ContextStrategy,
    titleRes: StringResource,
    hintRes: StringResource,
    onSelect: (ContextStrategy) -> Unit,
) {
    val isSelected = strategy == selected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelect(strategy) },
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
