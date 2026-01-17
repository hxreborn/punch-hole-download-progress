package eu.hxreborn.phpm.ui.component

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import eu.hxreborn.phpm.R
import eu.hxreborn.phpm.ui.theme.Tokens
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TweakSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    defaultValue: Float,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    stepSize: Float = 0f,
    hapticInterval: Float = 0f,
    enabled: Boolean = true,
    description: String = "",
) {
    var localValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    if (!isDragging && abs(localValue - value) > 0.001f) {
        localValue = value
    }

    val displayValue = if (isDragging) localValue else value
    val isDefault = abs(displayValue - defaultValue) < 0.001f
    val view = LocalView.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }
    var lastHapticTime by remember { mutableStateOf(0L) }

    val snappedOnChange: (Float) -> Unit = { newValue ->
        val snapped =
            if (stepSize > 0) {
                (newValue / stepSize).roundToInt() * stepSize
            } else {
                newValue
            }

        if (hapticInterval > 0 && abs(snapped - lastHapticValue) >= hapticInterval) {
            val now = SystemClock.uptimeMillis()
            if (now - lastHapticTime >= Tokens.HAPTIC_THROTTLE_MS) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                lastHapticTime = now
            }
            lastHapticValue = snapped
        }

        localValue = snapped
    }

    val onDragFinished: () -> Unit = {
        isDragging = false
        onValueChange(localValue)
    }

    Column(
        modifier =
            modifier.padding(
                horizontal = Tokens.SettingsRowHorizontalPadding,
                vertical = Tokens.SettingsRowVerticalPadding,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
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
                    )
                }
            }
            Text(
                text =
                    if (isDefault) {
                        "${valueLabel(displayValue)} ${stringResource(R.string.default_suffix)}"
                    } else {
                        valueLabel(displayValue)
                    },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Tokens.SpacingXs),
        ) {
            Slider(
                value = displayValue,
                onValueChange = {
                    isDragging = true
                    snappedOnChange(it)
                },
                onValueChangeFinished = onDragFinished,
                valueRange = valueRange,
                steps = steps,
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
                        if (isDefault) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Tokens.DISABLED_ALPHA)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
