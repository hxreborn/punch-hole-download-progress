package eu.hxreborn.phpm.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.hxreborn.phpm.R
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
    val isDefault = abs(value - defaultValue) < 0.001f
    val view = LocalView.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }

    val snappedOnChange: (Float) -> Unit = { newValue ->
        val snapped =
            if (stepSize > 0) {
                (newValue / stepSize).roundToInt() * stepSize
            } else {
                newValue
            }

        if (hapticInterval > 0 && abs(snapped - lastHapticValue) >= hapticInterval) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHapticValue = snapped
        }

        onValueChange(snapped)
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
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.alpha(if (enabled) 0.85f else 0.4f),
                        )
                    }
                }
                Text(
                    text =
                        if (isDefault) {
                            "${valueLabel(value)} ${stringResource(R.string.default_suffix)}"
                        } else {
                            valueLabel(
                                value,
                            )
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Slider(
                    value = value,
                    onValueChange = snappedOnChange,
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
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}
