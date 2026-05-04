@file:Suppress("AssignedValueIsNeverRead")

package eu.hxreborn.phdp.ui.component.preference

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.drawVerticalScrollbar
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.MaterialPalette
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun ColorPreference(
    value: Int,
    onValueChange: (Int) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: List<Int> = MaterialPalette.materialColors,
) {
    var showDialog by remember { mutableStateOf(false) }
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA

    if (showDialog) {
        ColorPickerDialog(
            initialColor = value,
            colors = colors,
            onDismiss = { showDialog = false },
            onColorSelected = { color ->
                onValueChange(color)
                showDialog = false
            },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled) { showDialog = true }.padding(Tokens.PreferencePadding),
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
    colors: List<Int>,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableIntStateOf(initialColor) }
    var hexText by remember { mutableStateOf(initialColor.toHex()) }
    var showPresets by remember { mutableStateOf(false) }
    val presetsScroll = rememberScrollState()
    val chevronRotation by animateFloatAsState(
        targetValue = if (showPresets) 180f else 0f,
        label = "presetsChevron",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HsvColorPicker(
                    modifier = Modifier.size(180.dp),
                    controller = controller,
                    initialColor = Color(initialColor),
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            val argb = envelope.color.toArgb()
                            selectedColor = argb
                            hexText = argb.toHex()
                        }
                    },
                )

                Spacer(modifier = Modifier.height(Tokens.SpacingSm))

                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    controller = controller,
                )

                Spacer(modifier = Modifier.height(Tokens.SpacingLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            val cleaned = input.removePrefix("#").take(6).uppercase()
                            hexText = cleaned
                            if (cleaned.length == 6) {
                                cleaned.toIntOrNull(16)?.let {
                                    val argb = 0xFF000000.toInt() or it
                                    selectedColor = argb
                                    controller.selectByColor(Color(argb), fromUser = false)
                                }
                            }
                        },
                        label = { Text("HEX") },
                        prefix = { Text("#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                    Box(
                        modifier =
                            Modifier
                                .size(Tokens.ColorPreviewSize)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                                .border(
                                    width = Tokens.ColorBorderWidth,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                    )
                }

                Spacer(modifier = Modifier.height(Tokens.SpacingSm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    TextButton(onClick = { showPresets = !showPresets }) {
                        Text(stringResource(R.string.presets))
                        Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.rotate(chevronRotation),
                        )
                    }
                }

                if (showPresets) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = Tokens.ColorGridMaxHeight)
                                .drawVerticalScrollbar(presetsScroll)
                                .verticalScroll(presetsScroll),
                        verticalArrangement = Arrangement.spacedBy(Tokens.ColorGridSpacing),
                    ) {
                        colors.chunked(Tokens.COLOR_GRID_COLUMNS).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                rowColors.forEach { color ->
                                    ColorSwatch(
                                        color = color,
                                        selected = color == selectedColor,
                                        onClick = {
                                            selectedColor = color
                                            hexText = color.toHex()
                                            controller.selectByColor(Color(color), fromUser = false)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun Int.toHex(): String = "%06X".format(this and 0xFFFFFF)

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = spring(),
        label = "swatchBorderColor",
    )
    Box(
        modifier =
            Modifier
                .size(Tokens.ColorSwatchSize)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (selected) Tokens.ColorBorderWidthSelected else Tokens.ColorBorderWidth,
                    color = borderColor,
                    shape = CircleShape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint =
                    if (color == MaterialPalette.White || color == MaterialPalette.Yellow500) {
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
            value = Prefs.color.default,
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
            value = Prefs.color.default,
            onValueChange = {},
            title = { Text(stringResource(R.string.pref_progress_color_title)) },
            summary = { Text(stringResource(R.string.pref_progress_color_summary)) },
        )
    }
}
