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
    colors: List<Color>,
    direction: String,
    angleDegrees: Int,
    bounds: Rect,
): Brush =
    when (GradientDirection.fromStoredValue(direction)) {
        GradientDirection.SWEEP -> {
            Brush.sweepGradient(
                colorStops = sweepStops(colors, angleDegrees),
                center = bounds.center,
            )
        }

        GradientDirection.LINEAR -> {
            val radians = angleDegrees * PI.toFloat() / 180f
            val dx = cos(radians)
            val dy = sin(radians)
            val halfExtent = bounds.width / 2f * abs(dx) + bounds.height / 2f * abs(dy)
            Brush.linearGradient(
                colors = colors,
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
    colors: List<Color>,
    angleDegrees: Int,
): Array<Pair<Float, Color>> {
    val n = colors.size
    val startFraction = (((angleDegrees - 90) % 360 + 360) % 360) / 360f

    fun wheel(t: Float): Color {
        val scaled = (((t % 1f) + 1f) % 1f) * n
        val i = scaled.toInt() % n
        val j = (i + 1) % n
        val f = scaled - scaled.toInt()
        val a = colors[i]
        val b = colors[j]
        return Color(
            red = a.red + (b.red - a.red) * f,
            green = a.green + (b.green - a.green) * f,
            blue = a.blue + (b.blue - a.blue) * f,
            alpha = a.alpha + (b.alpha - a.alpha) * f,
        )
    }

    val boundary = wheel(-startFraction)
    val anchors =
        colors
            .mapIndexed { index, color ->
                ((index.toFloat() / n + startFraction + 1f) % 1f) to color
            }.sortedBy { it.first }
    return buildList {
        add(0f to boundary)
        for (anchor in anchors) {
            if (anchor.first > 0f && anchor.first < 1f) add(anchor)
        }
        add(1f to boundary)
    }.toTypedArray()
}
