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
            Brush.sweepGradient(
                colorStops = sweepStops(startColor, endColor, angleDegrees),
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

private fun sweepStops(
    startColor: Color,
    endColor: Color,
    angleDegrees: Int,
): Array<Pair<Float, Color>> {
    val startFraction = (((angleDegrees - 90) % 360 + 360) % 360) / 360f

    fun colorAt(position: Float): Color {
        val t = (position - startFraction + 1f) % 1f
        val k = if (t < 0.5f) t * 2f else (1f - t) * 2f
        return Color(
            red = startColor.red + (endColor.red - startColor.red) * k,
            green = startColor.green + (endColor.green - startColor.green) * k,
            blue = startColor.blue + (endColor.blue - startColor.blue) * k,
            alpha = startColor.alpha + (endColor.alpha - startColor.alpha) * k,
        )
    }

    val boundary = colorAt(0f)
    val anchors =
        listOf(
            startFraction to startColor,
            (startFraction + 0.5f) % 1f to endColor,
        ).sortedBy { it.first }
    return buildList {
        add(0f to boundary)
        for (anchor in anchors) {
            if (anchor.first > 0f && anchor.first < 1f) add(anchor)
        }
        add(1f to boundary)
    }.toTypedArray()
}
