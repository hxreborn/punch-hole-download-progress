package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun TextInputPreference(
    value: String,
    onValueChange: (String) -> Unit,
    title: @Composable () -> Unit,
    defaultValue: String,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        TextInputDialog(
            title = title,
            current = value,
            defaultValue = defaultValue,
            onConfirm = {
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
                .clickable { showDialog = true }
                .padding(Tokens.PreferencePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                title()
            }
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    Text(stringResource(R.string.character_count, value.length))
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: @Composable () -> Unit,
    current: String,
    defaultValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val textFieldState = remember { TextFieldState(current) }
    val isDefault = textFieldState.text.toString() == defaultValue

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                ) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        title()
                    }
                }

                OutlinedTextField(
                    state = textFieldState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            textFieldState.edit {
                                replace(0, length, defaultValue)
                            }
                        },
                        enabled = !isDefault,
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(onClick = { onConfirm(textFieldState.text.toString()) }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}
