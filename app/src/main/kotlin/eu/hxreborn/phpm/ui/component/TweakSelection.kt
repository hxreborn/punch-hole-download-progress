package eu.hxreborn.phpm.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun TweakSelection(
    title: String,
    currentValue: String,
    entries: List<String>,
    values: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    enabled: Boolean = true,
    onLongPress: ((String) -> Unit)? = null,
    longPressHint: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentIndex = values.indexOf(currentValue).coerceAtLeast(0)
    val currentEntry = entries.getOrElse(currentIndex) { entries.firstOrNull() ?: "" }

    if (showDialog) {
        SelectionDialog(
            title = title,
            entries = entries,
            values = values,
            currentValue = currentValue,
            onDismiss = { showDialog = false },
            onValueSelected = { value ->
                onValueSelected(value)
                showDialog = false
            },
            onLongPress = onLongPress,
            longPressHint = longPressHint,
        )
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showDialog = true }
                .padding(
                    horizontal = Tokens.ListItemHorizontalPadding,
                    vertical = Tokens.SettingsRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = Tokens.DISABLED_ALPHA)
                    },
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Tokens.DISABLED_ALPHA)
                        },
                    modifier = Modifier.padding(top = Tokens.SpacingXs),
                )
            }
        }
        Text(
            text = currentEntry,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = Tokens.DISABLED_ALPHA)
                },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectionDialog(
    title: String,
    entries: List<String>,
    values: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onValueSelected: (String) -> Unit,
    onLongPress: ((String) -> Unit)? = null,
    longPressHint: String? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = Tokens.DialogShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Tokens.DialogElevation,
        ) {
            Column(modifier = Modifier.padding(vertical = Tokens.DialogPadding)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Tokens.DialogPadding),
                )

                if (longPressHint != null) {
                    Text(
                        text = longPressHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Tokens.MEDIUM_EMPHASIS_ALPHA),
                        modifier =
                            Modifier.padding(
                                horizontal = Tokens.DialogPadding,
                                vertical = Tokens.SpacingXs,
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(Tokens.DialogTitleSpacing))

                entries.forEachIndexed { index, entry ->
                    val value = values.getOrElse(index) { entry }
                    val isSelected = value == currentValue
                    val rowModifier =
                        if (onLongPress != null) {
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onValueSelected(value) },
                                    onLongClick = { onLongPress(value) },
                                    onLongClickLabel = "Preview",
                                )
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .clickable { onValueSelected(value) }
                        }
                    Row(
                        modifier =
                            rowModifier
                                .semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                }.padding(
                                    horizontal = Tokens.DialogPadding,
                                    vertical = Tokens.ListItemVerticalPadding,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = Tokens.ListItemLeadingSpacing),
                        )
                    }
                }
            }
        }
    }
}
