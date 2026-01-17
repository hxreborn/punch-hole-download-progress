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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

    ElevatedCard(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { showDialog = true }
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(top = 2.dp)
                                .alpha(if (enabled) 0.85f else 0.4f),
                    )
                }
            }
            Text(
                text = currentEntry,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            )
        }
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                if (longPressHint != null) {
                    Text(
                        text = longPressHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .alpha(0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                }.padding(horizontal = 24.dp, vertical = 12.dp),
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
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
