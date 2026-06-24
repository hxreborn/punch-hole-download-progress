@file:Suppress("AssignedValueIsNeverRead")

package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    valueToDescription: (T) -> String? = { null },
) {
    var showDialog by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA

    if (showDialog) {
        SelectDialog(
            title = title,
            value = value,
            values = values,
            valueToText = valueToText,
            valueToDescription = valueToDescription,
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
                LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    title()
                }
            }
            summary?.let {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        it()
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
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
    valueToDescription: (T) -> String?,
    onValueChange: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Column(
                modifier =
                    Modifier
                        .selectableGroup()
                        .fadingEdges(scrollState)
                        .verticalScroll(scrollState),
            ) {
                values.forEach { option ->
                    val selected = option == value
                    val description = valueToDescription(option)
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(SelectedRowShape)
                                .then(
                                    if (selected) {
                                        Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                                    } else {
                                        Modifier
                                    },
                                ).selectable(
                                    selected = selected,
                                    onClick = { onValueChange(option) },
                                    role = Role.RadioButton,
                                ).padding(
                                    horizontal = Tokens.PreferencePadding,
                                    vertical = Tokens.SpacingSm,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                        )
                        Spacer(
                            modifier = Modifier.padding(start = Tokens.PreferenceHorizontalSpacing),
                        )
                        Column {
                            Text(
                                text = valueToText(option),
                                style = MaterialTheme.typography.bodyLarge,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                            if (description != null) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                        }
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

private val SelectedRowShape = RoundedCornerShape(Tokens.RowCornerRadius)

private fun Modifier.fadingEdges(
    scrollState: ScrollState,
    fadeLength: Dp = Tokens.SpacingLg,
): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadePx = fadeLength.toPx()
            if (scrollState.canScrollBackward) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = fadePx,
                        ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (scrollState.canScrollForward) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - fadePx,
                            endY = size.height,
                        ),
                    blendMode = BlendMode.DstIn,
                )
            }
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
