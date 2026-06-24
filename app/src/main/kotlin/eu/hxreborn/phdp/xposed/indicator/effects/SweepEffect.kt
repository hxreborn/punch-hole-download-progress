package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class SweepEffect : CompletionEffect {
    private var animator: ValueAnimator? = null
    private var trim = 0f
    private var fade = 1f
    private var reverse = false

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        trim = 0f
        fade = 1f
        reverse = params.reverse
        animator =
            playFinish(
                view = view,
                durationMs = params.scaled(params.holdMs + params.exitMs),
                interpolator = AccelerateDecelerateInterpolator(),
                onUpdate = { f ->
                    if (f < DRAW_ON) {
                        trim = f / DRAW_ON
                        fade = 1f
                    } else {
                        trim = 1f
                        fade = 1f - (f - DRAW_ON) / (1f - DRAW_ON)
                    }
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

    companion object {
        private const val DRAW_ON = 0.4f
    }
}
