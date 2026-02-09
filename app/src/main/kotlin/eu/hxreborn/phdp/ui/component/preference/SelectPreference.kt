package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun <T> SelectPreference(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    valueToText: (T) -> String = { it.toString() },
) {
    var showDialog by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA

    if (showDialog) {
        SelectDialog(
            title = title,
            value = value,
            values = values,
            valueToText = valueToText,
            onValueChange = {
                onValueChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showDialog = true }
                .padding(Tokens.PreferencePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalContentColor provides
                    MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    title()
                }
            }
            summary?.let {
                CompositionLocalProvider(
                    LocalContentColor provides
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        it()
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
        ) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Text(valueToText(value))
            }
        }
    }
}

@Composable
private fun <T> SelectDialog(
    title: @Composable () -> Unit,
    value: T,
    values: List<T>,
    valueToText: (T) -> String,
    onValueChange: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                values.forEach { option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(Tokens.PreferenceIconContainerMinWidth)
                                .selectable(
                                    selected = option == value,
                                    onClick = { onValueChange(option) },
                                    role = Role.RadioButton,
                                ).padding(horizontal = Tokens.PreferencePadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == value,
                            onClick = null,
                        )
                        Spacer(
                            modifier = Modifier.padding(start = Tokens.PreferenceHorizontalSpacing),
                        )
                        Text(
                            text = valueToText(option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun SelectPreferencePreview() {
    AppTheme {
        SelectPreference(
            value = Prefs.percentTextPosition.default,
            onValueChange = {},
            values = listOf("left", "right"),
            title = { Text(stringResource(R.string.pref_calibrate_percent_title)) },
            summary = { Text(stringResource(R.string.pref_calibrate_percent_summary)) },
            valueToText = { it.replaceFirstChar { c -> c.uppercase() } },
        )
    }
}
