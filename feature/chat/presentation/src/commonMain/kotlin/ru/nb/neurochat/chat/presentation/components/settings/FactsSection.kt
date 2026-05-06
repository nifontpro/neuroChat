package ru.nb.neurochat.chat.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.nb.neurochat.chat.presentation.generated.resources.Res
import ru.nb.neurochat.chat.presentation.generated.resources.label_facts_clear
import ru.nb.neurochat.chat.presentation.generated.resources.label_facts_empty
import ru.nb.neurochat.chat.presentation.generated.resources.label_facts_section
import ru.nb.neurochat.chat.presentation.generated.resources.label_facts_updating
import ru.nb.neurochat.domain.model.Fact

/** Секция отображения авто-извлечённых фактов (только для STICKY_FACTS). Read-only список. */
@Composable
fun FactsSection(
    facts: List<Fact>,
    isUpdating: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.label_facts_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        if (isUpdating) {
            Text(
                text = stringResource(Res.string.label_facts_updating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (facts.isEmpty()) {
            Text(
                text = stringResource(Res.string.label_facts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            facts.forEach { fact ->
                Text(
                    text = "• ${fact.key}: ${fact.value}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onClear) {
                Text(text = stringResource(Res.string.label_facts_clear))
            }
        }
    }
}
