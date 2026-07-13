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

internal class ProgressShaderCache {
    private data class Config(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val startColor: Int,
        val endColor: Int,
        val direction: GradientDirection,
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
        startColor: Int,
        endColor: Int,
        direction: String,
        hdrEnabled: Boolean,
        hdrHeadroom: Float,
        enabled: Boolean,
    ): Shader? {
        val normalizedDirection = GradientDirection.fromStoredValue(direction)
        if (cachedConfig?.matches(
                bounds,
                startColor,
                endColor,
                normalizedDirection,
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
                startColor = startColor,
                endColor = endColor,
                direction = normalizedDirection,
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
        startColor: Int,
        endColor: Int,
        direction: GradientDirection,
        hdrEnabled: Boolean,
        hdrHeadroom: Float,
        enabled: Boolean,
    ): Boolean =
        left == bounds.left &&
            top == bounds.top &&
            right == bounds.right &&
            bottom == bounds.bottom &&
            this.startColor == startColor &&
            this.endColor == endColor &&
            this.direction == direction &&
            this.hdrEnabled == hdrEnabled &&
            this.hdrHeadroom == hdrHeadroom &&
            this.enabled == enabled

    private fun createSdrShader(config: Config): Shader =
        when (config.direction) {
            GradientDirection.SWEEP -> {
                SweepGradient(
                    config.centerX,
                    config.centerY,
                    intArrayOf(config.startColor, config.endColor, config.startColor),
                    SWEEP_POSITIONS,
                ).alignSweepToTop(config)
            }

            else -> {
                val points = config.linearPoints()
                LinearGradient(
                    points.startX,
                    points.startY,
                    points.endX,
                    points.endY,
                    config.startColor,
                    config.endColor,
                    Shader.TileMode.CLAMP,
                )
            }
        }

    @RequiresApi(35)
    private fun createHdrShader(config: Config): Shader {
        val startColor = packExtendedSrgb(config.startColor, config.hdrHeadroom)
        val endColor = packExtendedSrgb(config.endColor, config.hdrHeadroom)
        return when (config.direction) {
            GradientDirection.SWEEP -> {
                SweepGradient(
                    config.centerX,
                    config.centerY,
                    longArrayOf(startColor, endColor, startColor),
                    SWEEP_POSITIONS,
                ).alignSweepToTop(config)
            }

            else -> {
                val points = config.linearPoints()
                LinearGradient(
                    points.startX,
                    points.startY,
                    points.endX,
                    points.endY,
                    longArrayOf(startColor, endColor),
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
                    setRotate(-90f, config.centerX, config.centerY)
                },
            )
        }

    private fun Config.linearPoints(): LinearPoints =
        when (direction) {
            GradientDirection.LEFT_TO_RIGHT -> LinearPoints(left, centerY, right, centerY)
            GradientDirection.TOP_TO_BOTTOM -> LinearPoints(centerX, top, centerX, bottom)
            GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT -> LinearPoints(left, top, right, bottom)
            GradientDirection.BOTTOM_LEFT_TO_TOP_RIGHT -> LinearPoints(left, bottom, right, top)
            GradientDirection.SWEEP -> error("Sweep gradients do not use linear points")
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

    companion object {
        private val SWEEP_POSITIONS = floatArrayOf(0f, 0.5f, 1f)
    }
}
