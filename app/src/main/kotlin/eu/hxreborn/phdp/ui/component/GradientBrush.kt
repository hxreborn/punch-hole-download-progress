package eu.hxreborn.phdp.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import eu.hxreborn.phdp.prefs.GradientDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal fun gradientBrush(
    startColor: Color,
    endColor: Color,
    direction: String,
    angleDegrees: Int,
    bounds: Rect,
): Brush =
    when (GradientDirection.fromStoredValue(direction)) {
        GradientDirection.SWEEP -> {
            val midpoint =
                Color(
                    red = (startColor.red + endColor.red) / 2f,
                    green = (startColor.green + endColor.green) / 2f,
                    blue = (startColor.blue + endColor.blue) / 2f,
                    alpha = (startColor.alpha + endColor.alpha) / 2f,
                )
            Brush.sweepGradient(
                colorStops =
                    arrayOf(
                        0f to midpoint,
                        0.25f to endColor,
                        0.75f to startColor,
                        1f to midpoint,
                    ),
                center = bounds.center,
            )
        }

        GradientDirection.LINEAR -> {
            val radians = angleDegrees * PI.toFloat() / 180f
            val dx = cos(radians)
            val dy = sin(radians)
            val halfExtent = bounds.width / 2f * abs(dx) + bounds.height / 2f * abs(dy)
            Brush.linearGradient(
                colors = listOf(startColor, endColor),
                start =
                    Offset(
                        bounds.center.x - dx * halfExtent,
                        bounds.center.y - dy * halfExtent,
                    ),
                end =
                    Offset(
                        bounds.center.x + dx * halfExtent,
                        bounds.center.y + dy * halfExtent,
                    ),
            )
        }
    }
