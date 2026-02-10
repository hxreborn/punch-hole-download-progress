package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun TextInputPreference(
    value: String,
    onValueChange: (String) -> Unit,
    title: @Composable () -> Unit,
    defaultValue: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    var editableValue by remember(value) { mutableStateOf(value) }
    val focusManager = LocalFocusManager.current
    val isDefault = editableValue == defaultValue
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Tokens.PreferencePadding),
    ) {
        ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
            title()
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = Tokens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = editableValue,
                onValueChange = {
                    editableValue = it
                    onValueChange(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = placeholder,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                supportingText = {
                    Text(
                        "${editableValue.length}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            IconButton(
                onClick = {
                    editableValue = defaultValue
                    onValueChange(defaultValue)
                },
                enabled = !isDefault,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    tint = if (!isDefault) iconTint else iconTint.copy(alpha = Tokens.DISABLED_ALPHA),
                )
            }
        }
    }
}
