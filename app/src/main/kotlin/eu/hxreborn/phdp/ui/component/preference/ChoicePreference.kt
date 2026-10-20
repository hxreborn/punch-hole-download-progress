package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.hxreborn.phdp.ui.component.ChoiceButtonGroup
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun <T> ChoicePreference(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    valueToText: (T) -> String = { it.toString() },
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Tokens.PreferencePadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { PreferenceIcon(it, enabled) }
            PreferenceTexts(title = title, summary = summary, enabled = enabled, modifier = Modifier.weight(1f))
        }
        ChoiceButtonGroup(
            options = values,
            selected = value,
            onSelect = onValueChange,
            label = { valueToText(it) },
            enabled = { enabled },
        )
    }
}
