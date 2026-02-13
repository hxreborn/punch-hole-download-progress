package eu.hxreborn.phdp.xposed.indicator

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class ArcRingRenderer : RingRenderer {
    private val bounds = RectF()

    override fun updateBounds(bounds: RectF) {
        this.bounds.set(bounds)
    }

    override fun drawFullRing(
        canvas: Canvas,
        paint: Paint,
    ) {
        canvas.drawArc(bounds, 0f, 360f, false, paint)
    }

    override fun drawProgress(
        canvas: Canvas,
        sweepFraction: Float,
        clockwise: Boolean,
        paint: Paint,
    ) {
        val sweepAngle = 360f * sweepFraction
        val actualSweep = if (clockwise) sweepAngle else -sweepAngle
        canvas.drawArc(bounds, -90f, actualSweep, false, paint)
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
        for (i in 0 until segments) {
            val startAngle = -90f + i * (arcDeg + gapDeg)
            val paint =
                if (i == highlight || i == highlight - 1) {
                    Paint(shinePaint).apply { this.alpha = (255 * alpha).toInt() }
                } else {
                    Paint(basePaint)
                }
            canvas.drawArc(bounds, startAngle, arcDeg, false, paint)
        }
    }
}
