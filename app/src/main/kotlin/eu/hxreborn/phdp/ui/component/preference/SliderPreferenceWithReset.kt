package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
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
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlin.math.abs

@Composable
fun SliderPreferenceWithReset(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: @Composable () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (() -> Unit)? = null,
    valueText: @Composable ((Float) -> Unit)? = null,
    enabled: Boolean = true,
) {
    // Track dragging state to avoid resetting slider during drag
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(value) }

    // Only sync from external value when not dragging
    if (!isDragging) {
        sliderValue = value
    }

    val isDefault = abs(sliderValue - defaultValue) < 0.001f
    val contentAlpha = if (enabled) 1f else Tokens.DISABLED_ALPHA

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Tokens.PreferencePadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
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
            valueText?.let {
                Box(modifier = Modifier.padding(start = Tokens.PreferenceHorizontalSpacing)) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            it(sliderValue)
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Tokens.SpacingSm),
        ) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    onValueChange(sliderValue)
                    isDragging = false
                },
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onReset,
                enabled = enabled && !isDefault,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    tint =
                        if (enabled && !isDefault) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Tokens.DISABLED_ALPHA)
                        },
                )
            }
        }
    }
}
