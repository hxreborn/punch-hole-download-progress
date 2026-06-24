package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.AccelerateInterpolator

class ShutterWipeEffect : CompletionEffect {
    private var animator: ValueAnimator? = null
    private var trim = 1f
    private var fade = 1f
    private var reverse = false

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        trim = 1f
        fade = 1f
        reverse = params.reverse
        animator =
            playFinish(
                view = view,
                durationMs = params.scaled(params.exitMs),
                interpolator = AccelerateInterpolator(),
                onUpdate = { f ->
                    trim = 1f - f
                    fade = 1f - f * 0.4f
                },
                onEnd = onEnd,
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        val maxAlpha = ctx.paint.alpha
        ctx.paint.color = ctx.shinePaint.color
        ctx.paint.alpha = (maxAlpha * fade).toInt()
        ctx.renderer.drawProgress(canvas, trim, ctx.clockwise != reverse, ctx.paint)
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }
}
