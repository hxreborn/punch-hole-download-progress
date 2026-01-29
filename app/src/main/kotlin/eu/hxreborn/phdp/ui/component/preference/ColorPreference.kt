package eu.hxreborn.phdp.ui.component.preference

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens

private val materialColors =
    listOf(
        0xFFF44336.toInt(), // Red 500
        0xFFE91E63.toInt(), // Pink 500
        0xFF9C27B0.toInt(), // Purple 500
        0xFF673AB7.toInt(), // Deep Purple 500
        0xFF3F51B5.toInt(), // Indigo 500
        0xFF2196F3.toInt(), // Blue 500
        0xFF03A9F4.toInt(), // Light Blue 500
        0xFF00BCD4.toInt(), // Cyan 500
        0xFF009688.toInt(), // Teal 500
        0xFF4CAF50.toInt(), // Green 500
        0xFF8BC34A.toInt(), // Light Green 500
        0xFFCDDC39.toInt(), // Lime 500
        0xFFFFEB3B.toInt(), // Yellow 500
        0xFFFFC107.toInt(), // Amber 500
        0xFFFF9800.toInt(), // Orange 500
        0xFFFF5722.toInt(), // Deep Orange 500
        0xFF795548.toInt(), // Brown 500
        0xFF9E9E9E.toInt(), // Grey 500
        0xFF607D8B.toInt(), // Blue Grey 500
        0xFFFFFFFF.toInt(), // White
    )

@Composable
fun ColorPreference(
    value: Int,
    onValueChange: (Int) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA

    if (showDialog) {
        ColorPickerDialog(
            initialColor = value,
            onDismiss = { showDialog = false },
            onColorSelected = { color ->
                onValueChange(color)
                showDialog = false
            },
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
        Box(
            modifier =
                Modifier
                    .padding(start = Tokens.PreferenceHorizontalSpacing)
                    .size(Tokens.ColorPreviewSize)
                    .clip(CircleShape)
                    .background(Color(value).copy(alpha = contentAlpha))
                    .border(
                        width = Tokens.ColorBorderWidth,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = contentAlpha),
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    var selectedColor by remember {
        mutableIntStateOf(materialColors.find { it == initialColor } ?: materialColors[5])
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = Tokens.DialogShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Tokens.DialogElevation,
        ) {
            Column(
                modifier = Modifier.padding(Tokens.DialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.pick_color),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(Tokens.DialogTitleSpacing))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(Tokens.COLOR_GRID_COLUMNS),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.ColorGridSpacing),
                    verticalArrangement = Arrangement.spacedBy(Tokens.ColorGridSpacing),
                ) {
                    items(materialColors) { color ->
                        ColorSwatch(
                            color = color,
                            selected = color == selectedColor,
                            onClick = { selectedColor = color },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Tokens.DialogTitleSpacing))

                Text(
                    text = stringResource(R.string.color_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Tokens.SpacingSm))

                Box(
                    modifier =
                        Modifier
                            .size(Tokens.ColorPreviewSizeLarge)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                            .border(
                                width = Tokens.ColorBorderWidth,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                )

                Spacer(modifier = Modifier.height(Tokens.DialogActionsSpacing))

                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onColorSelected(selectedColor) }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(Tokens.ColorSwatchSize)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (selected) Tokens.ColorBorderWidthSelected else Tokens.ColorBorderWidth,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = CircleShape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint =
                    if (color == 0xFFFFFFFF.toInt() || color == 0xFFFFFF00.toInt()) {
                        Color.Black
                    } else {
                        Color.White
                    },
                modifier = Modifier.size(Tokens.ColorCheckIconSize),
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun ColorPreferencePreview() {
    AppTheme {
        ColorPreference(
            value = PrefsManager.DEFAULT_COLOR,
            onValueChange = {},
            title = { Text(stringResource(R.string.pref_progress_color_title)) },
            summary = { Text(stringResource(R.string.pref_progress_color_summary)) },
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ColorPreferenceDarkPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        ColorPreference(
            value = PrefsManager.DEFAULT_COLOR,
            onValueChange = {},
            title = { Text(stringResource(R.string.pref_progress_color_title)) },
            summary = { Text(stringResource(R.string.pref_progress_color_summary)) },
        )
    }
}
