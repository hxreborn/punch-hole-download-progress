package eu.hxreborn.phdp.xposed.indicator

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF

class PathRingRenderer : RingRenderer {
    private val ringPath = Path()
    private val ringMeasure = PathMeasure()
    private val segment = Path()
    private var ringLength = 0f

    override fun updateBounds(bounds: RectF) {
        buildCapsulePath(bounds)
        ringMeasure.setPath(ringPath, false)
        ringLength = ringMeasure.length
    }

    // Build capsule path starting at top center so progress begins at distance 0
    private fun buildCapsulePath(bounds: RectF) {
        ringPath.reset()
        val r = minOf(bounds.width(), bounds.height()) / 2f
        val cx = bounds.centerX()

        ringPath.moveTo(cx, bounds.top)

        if (bounds.width() >= bounds.height()) {
            // Horizontal capsule
            ringPath.lineTo(bounds.right - r, bounds.top)
            ringPath.arcTo(
                bounds.right - 2 * r,
                bounds.top,
                bounds.right,
                bounds.bottom,
                -90f,
                180f,
                false,
            )
            ringPath.lineTo(bounds.left + r, bounds.bottom)
            ringPath.arcTo(
                bounds.left,
                bounds.top,
                bounds.left + 2 * r,
                bounds.bottom,
                90f,
                180f,
                false,
            )
        } else {
            // Vertical capsule
            ringPath.arcTo(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.top + 2 * r,
                -90f,
                90f,
                false,
            )
            ringPath.lineTo(bounds.right, bounds.bottom - r)
            ringPath.arcTo(
                bounds.left,
                bounds.bottom - 2 * r,
                bounds.right,
                bounds.bottom,
                0f,
                180f,
                false,
            )
            ringPath.lineTo(bounds.left, bounds.top + r)
            ringPath.arcTo(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.top + 2 * r,
                180f,
                90f,
                false,
            )
        }

        ringPath.close()
    }

    override fun drawFullRing(
        canvas: Canvas,
        paint: Paint,
    ) {
        if (ringLength == 0f) return
        canvas.drawPath(ringPath, paint)
    }

    override fun drawProgress(
        canvas: Canvas,
        sweepFraction: Float,
        clockwise: Boolean,
        paint: Paint,
    ) {
        if (ringLength == 0f) return
        if (sweepFraction >= 1f) {
            drawFullRing(canvas, paint)
            return
        }
        val sweep = sweepFraction.coerceIn(0f, 1f) * ringLength
        segment.reset()
        if (clockwise) {
            ringMeasure.getSegment(0f, sweep, segment, true)
        } else {
            ringMeasure.getSegment(ringLength - sweep, ringLength, segment, true)
        }
        canvas.drawPath(segment, paint)
    }

    override fun drawSegmented(
        canvas: Canvas,
        segments: Int,
        gapDeg: Float,
        arcDeg: Float,
        highlight: Int,
        basePaint: Paint,
        shinePaint: Paint,
        alpha: Float,
    ) {
        if (ringLength == 0f) return
        val totalDeg = segments * (arcDeg + gapDeg)
        val gapLen = ringLength * (gapDeg / totalDeg)
        val segLen = ringLength * (arcDeg / totalDeg)

        for (i in 0 until segments) {
            val paint =
                if (i == highlight || i == highlight - 1) {
                    Paint(shinePaint).apply { this.alpha = (255 * alpha).toInt() }
                } else {
                    Paint(basePaint)
                }
            val segStart = i * (segLen + gapLen)
            segment.reset()
            ringMeasure.getSegment(segStart, segStart + segLen, segment, true)
            canvas.drawPath(segment, paint)
        }
    }
}
