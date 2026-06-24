package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.DecelerateInterpolator

class SpinOffEffect : CompletionEffect {
    private var animator: ValueAnimator? = null
    private var trim = 1f
    private var rotationDeg = 0f
    private var fade = 1f

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        trim = 1f
        rotationDeg = 0f
        fade = 1f
        val sweep = BASE_SPIN * params.intensityScale * if (params.reverse) -1f else 1f
        animator =
            playFinish(
                view = view,
                durationMs = params.scaled(params.exitMs),
                interpolator = DecelerateInterpolator(),
                onUpdate = { f ->
                    trim = 1f - f
                    rotationDeg = f * sweep
                    fade = 1f - f
                },
                onEnd = onEnd,
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        val maxAlpha = ctx.paint.alpha
        canvas.save()
        canvas.rotate(rotationDeg, ctx.bounds.centerX(), ctx.bounds.centerY())
        ctx.paint.color = ctx.shinePaint.color
        ctx.paint.alpha = (maxAlpha * fade).toInt()
        ctx.renderer.drawProgress(canvas, trim, ctx.clockwise, ctx.paint)
        canvas.restore()
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }

    companion object {
        private const val BASE_SPIN = 270f
    }
}
