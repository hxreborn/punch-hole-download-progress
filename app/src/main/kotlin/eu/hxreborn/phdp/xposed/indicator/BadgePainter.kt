package eu.hxreborn.phdp.xposed.indicator

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils

class BadgePainter(
    private val density: Float,
) {
    private val bgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isSubpixelText = true
        }

    private val bounds = RectF()
    private var cachedCount = -1
    private var cachedText = ""

    fun updateColors(
        @ColorInt baseColor: Int,
    ) {
        val luminance = ColorUtils.calculateLuminance(baseColor)
        bgPaint.color = darkenColor(baseColor, DARKEN_FACTOR)
        textPaint.color =
            if (luminance > LUMINANCE_THRESHOLD) {
                Color.WHITE
            } else {
                brightenColor(baseColor, BRIGHTEN_FACTOR)
            }
        textPaint.textSize = TEXT_SIZE_SP * density
    }

    fun draw(
        canvas: Canvas,
        centerX: Float,
        top: Float,
        count: Int,
        @IntRange(from = 0, to = 100) opacity: Int,
    ) {
        if (count != cachedCount) {
            cachedCount = count
            cachedText = count.toString()
        }

        val textWidth = textPaint.measureText(cachedText)
        val hPad = H_PADDING_DP * density
        val vPad = V_PADDING_DP * density
        val height = textPaint.textSize + vPad * 2
        val width = maxOf(textWidth + hPad * 2, MIN_WIDTH_DP * density)

        bounds.set(centerX - width / 2, top, centerX + width / 2, top + height)

        val alpha = opacity.coerceIn(0, 100) * 255 / 100
        bgPaint.alpha = alpha * BG_ALPHA_FACTOR / 255
        textPaint.alpha = alpha

        canvas.drawRoundRect(bounds, height / 2, height / 2, bgPaint)

        val fm = textPaint.fontMetrics
        val textY = bounds.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText(cachedText, centerX, textY, textPaint)
    }

    private fun darkenColor(
        @ColorInt color: Int,
        factor: Float,
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] *= (1f - factor)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun brightenColor(
        @ColorInt color: Int,
        factor: Float,
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] += (1f - hsl[2]) * factor
        return ColorUtils.HSLToColor(hsl)
    }

    companion object {
        private const val TEXT_SIZE_SP = 10f
        private const val H_PADDING_DP = 6f
        private const val V_PADDING_DP = 3f
        private const val MIN_WIDTH_DP = 20f
        private const val DARKEN_FACTOR = 0.75f
        private const val BRIGHTEN_FACTOR = 0.8f
        private const val LUMINANCE_THRESHOLD = 0.4
        private const val BG_ALPHA_FACTOR = 230
    }
}
