package eu.hxreborn.phdp.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import eu.hxreborn.phdp.prefs.GradientDirection

internal fun gradientBrush(
    startColor: Color,
    endColor: Color,
    direction: String,
    bounds: Rect,
): Brush =
    when (GradientDirection.fromStoredValue(direction)) {
        GradientDirection.SWEEP -> {
            val midpoint = lerp(startColor, endColor, 0.5f)
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

        GradientDirection.LEFT_TO_RIGHT -> {
            Brush.linearGradient(
                colors = listOf(startColor, endColor),
                start = Offset(bounds.left, bounds.center.y),
                end = Offset(bounds.right, bounds.center.y),
            )
        }

        GradientDirection.TOP_TO_BOTTOM -> {
            Brush.linearGradient(
                colors = listOf(startColor, endColor),
                start = Offset(bounds.center.x, bounds.top),
                end = Offset(bounds.center.x, bounds.bottom),
            )
        }

        GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT -> {
            Brush.linearGradient(
                colors = listOf(startColor, endColor),
                start = bounds.topLeft,
                end = bounds.bottomRight,
            )
        }

        GradientDirection.BOTTOM_LEFT_TO_TOP_RIGHT -> {
            Brush.linearGradient(
                colors = listOf(startColor, endColor),
                start = bounds.bottomLeft,
                end = bounds.topRight,
            )
        }
    }
