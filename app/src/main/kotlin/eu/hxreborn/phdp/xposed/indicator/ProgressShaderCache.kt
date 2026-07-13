package eu.hxreborn.phdp.xposed.indicator

import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import androidx.annotation.RequiresApi
import eu.hxreborn.phdp.prefs.GradientDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal class ProgressShaderCache {
    private class Config(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val colors: IntArray,
        val direction: GradientDirection,
        val angle: Int,
        val hdrEnabled: Boolean,
        val hdrHeadroom: Float,
        val enabled: Boolean,
    )

    private data class LinearPoints(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
    )

    private var cachedConfig: Config? = null
    private var cachedShader: Shader? = null

    fun shaderFor(
        bounds: RectF,
        colors: IntArray,
        direction: String,
        angle: Int,
        hdrEnabled: Boolean,
        hdrHeadroom: Float,
        enabled: Boolean,
    ): Shader? {
        val normalizedDirection = GradientDirection.fromStoredValue(direction)
        if (cachedConfig?.matches(
                bounds,
                colors,
                normalizedDirection,
                angle,
                hdrEnabled,
                hdrHeadroom,
                enabled,
            ) == true
        ) {
            return cachedShader
        }

        val config =
            Config(
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
                colors = colors,
                direction = normalizedDirection,
                angle = angle,
                hdrEnabled = hdrEnabled,
                hdrHeadroom = hdrHeadroom,
                enabled = enabled,
            )
        cachedConfig = config
        cachedShader =
            when {
                !enabled || bounds.isEmpty -> null
                Build.VERSION.SDK_INT >= 35 && hdrEnabled -> createHdrShader(config)
                else -> createSdrShader(config)
            }
        return cachedShader
    }

    private fun Config.matches(
        bounds: RectF,
        colors: IntArray,
        direction: GradientDirection,
        angle: Int,
        hdrEnabled: Boolean,
        hdrHeadroom: Float,
        enabled: Boolean,
    ): Boolean =
        left == bounds.left &&
            top == bounds.top &&
            right == bounds.right &&
            bottom == bounds.bottom &&
            this.colors.contentEquals(colors) &&
            this.direction == direction &&
            this.angle == angle &&
            this.hdrEnabled == hdrEnabled &&
            this.hdrHeadroom == hdrHeadroom &&
            this.enabled == enabled

    private fun createSdrShader(config: Config): Shader =
        when (config.direction) {
            GradientDirection.SWEEP -> {
                SweepGradient(
                    config.centerX,
                    config.centerY,
                    config.colors + config.colors[0],
                    null,
                ).alignSweepToTop(config)
            }

            GradientDirection.LINEAR -> {
                val points = config.linearPoints()
                LinearGradient(
                    points.startX,
                    points.startY,
                    points.endX,
                    points.endY,
                    config.colors,
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
        }

    @RequiresApi(35)
    private fun createHdrShader(config: Config): Shader {
        val packed =
            LongArray(config.colors.size) {
                packExtendedSrgb(config.colors[it], config.hdrHeadroom)
            }
        return when (config.direction) {
            GradientDirection.SWEEP -> {
                SweepGradient(
                    config.centerX,
                    config.centerY,
                    packed + packed[0],
                    null,
                ).alignSweepToTop(config)
            }

            GradientDirection.LINEAR -> {
                val points = config.linearPoints()
                LinearGradient(
                    points.startX,
                    points.startY,
                    points.endX,
                    points.endY,
                    packed,
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
        }
    }

    private fun Shader.alignSweepToTop(config: Config): Shader =
        apply {
            setLocalMatrix(
                Matrix().apply {
                    setRotate(-90f + config.angle, config.centerX, config.centerY)
                },
            )
        }

    private fun Config.linearPoints(): LinearPoints {
        val radians = angle * PI.toFloat() / 180f
        val dx = cos(radians)
        val dy = sin(radians)
        val halfExtent = (right - left) / 2f * abs(dx) + (bottom - top) / 2f * abs(dy)
        return LinearPoints(
            startX = centerX - dx * halfExtent,
            startY = centerY - dy * halfExtent,
            endX = centerX + dx * halfExtent,
            endY = centerY + dy * halfExtent,
        )
    }

    @RequiresApi(35)
    private fun packExtendedSrgb(
        color: Int,
        headroom: Float,
    ): Long =
        Color.pack(
            Color.red(color) / 255f * headroom,
            Color.green(color) / 255f * headroom,
            Color.blue(color) / 255f * headroom,
            Color.alpha(color) / 255f,
            ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB),
        )

    private val Config.centerX: Float
        get() = (left + right) / 2f

    private val Config.centerY: Float
        get() = (top + bottom) / 2f
}
