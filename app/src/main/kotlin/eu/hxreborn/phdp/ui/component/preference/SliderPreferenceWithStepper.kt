@file:Suppress("AssignedValueIsNeverRead")

package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlin.math.abs
import kotlin.math.roundToInt

private fun Float.roundToStep(
    stepSize: Float,
    range: ClosedFloatingPointRange<Float>,
): Float {
    if (stepSize <= 0f) return this
    val stepped = (((this - range.start) / stepSize).roundToInt() * stepSize + range.start)
    return stepped.coerceIn(range)
}

private fun formatValue(
    value: Float,
    decimalPlaces: Int,
    suffix: String,
): String = "%.${decimalPlaces}f$suffix".format(value)

@Composable
fun SliderPreferenceWithStepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: @Composable () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stepSize: Float = 1f,
    decimalPlaces: Int = 0,
    suffix: String = "",
    summary: @Composable (() -> Unit)? = null,
) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(value) }

    // Sync from external only when not dragging to avoid excessive IO writes
    if (!isDragging) {
        sliderValue = value
    }

    val isDefault = abs(sliderValue - defaultValue) < 0.001f
    val isAtMin = sliderValue <= valueRange.start
    val isAtMax = sliderValue >= valueRange.endInclusive
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Tokens.PreferencePadding),
    ) {
        // Row 1: Title + Stepper cluster
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Title (takes remaining space, pushes stepper to end)
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                        title()
                    }
                }
            }

            // Stepper cluster: [-] value [+]
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decrement button
                IconButton(
                    onClick = {
                        val newValue = (sliderValue - stepSize).roundToStep(stepSize, valueRange)
                        sliderValue = newValue
                        onValueChange(newValue)
                    },
                    enabled = enabled && !isAtMin,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease),
                        tint = iconTint.copy(alpha = if (enabled && !isAtMin) 1f else Tokens.DISABLED_ALPHA),
                    )
                }

                // Value text (fixed width to prevent layout jump)
                Text(
                    text = formatValue(sliderValue, decimalPlaces, suffix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 56.dp),
                )

                // Increment button
                IconButton(
                    onClick = {
                        val newValue = (sliderValue + stepSize).roundToStep(stepSize, valueRange)
                        sliderValue = newValue
                        onValueChange(newValue)
                    },
                    enabled = enabled && !isAtMax,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.increase),
                        tint = iconTint.copy(alpha = if (enabled && !isAtMax) 1f else Tokens.DISABLED_ALPHA),
                    )
                }
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

        // Row 2: Slider + Reset button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Tokens.SpacingSm),
        ) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDragging = true
                    sliderValue = it.roundToStep(stepSize, valueRange)
                },
                onValueChangeFinished = {
                    onValueChange(sliderValue)
                    isDragging = false
                },
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )

            // Reset button
            IconButton(
                onClick = onReset,
                enabled = enabled && !isDefault,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    tint = iconTint.copy(alpha = if (enabled && !isDefault) 1f else Tokens.DISABLED_ALPHA),
                )
            }
        }
    }
}
