package eu.hxreborn.phdp.xposed.indicator

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Draws and updates the ring geometry used by the indicator overlay.
 *
 * Implementations expose the same rendering operations while using different geometric models:
 * - [ArcRingRenderer]: [drawArc][android.graphics.Canvas.drawArc]-based angle math for circular cutouts.
 * - [PathRingRenderer]: [PathMeasure][android.graphics.PathMeasure]-based distance math for pill-like cutouts.
 */
interface RingRenderer {
    /**
     * Updates the drawable ring bounds after layout, inset, or cutout geometry changes.
     */
    fun updateBounds(bounds: RectF)

    /**
     * Draws the complete ring path using the provided paint.
     */
    fun drawFullRing(
        canvas: Canvas,
        paint: Paint,
    )

    /**
     * Draws the active progress arc on top of the track.
     *
     * @param sweepFraction progress in the [0f, 1f] range.
     * @param clockwise `true` to advance clockwise, `false` for counterclockwise.
     */
    fun drawProgress(
        canvas: Canvas,
        sweepFraction: Float,
        clockwise: Boolean,
        paint: Paint,
    )

    /**
     * Draws segmented progress markers and highlights one segment as active.
     *
     * @param segments total marker count around the ring.
     * @param gapDeg angular gap between neighboring segments in degrees.
     * @param arcDeg segment angular size in degrees.
     * @param highlight segment index to emphasize.
     * @param alpha global alpha multiplier for segmented rendering.
     */
    fun drawSegmented(
        canvas: Canvas,
        segments: Int,
        gapDeg: Float,
        arcDeg: Float,
        highlight: Int,
        basePaint: Paint,
        shinePaint: Paint,
        alpha: Float,
    )
}
