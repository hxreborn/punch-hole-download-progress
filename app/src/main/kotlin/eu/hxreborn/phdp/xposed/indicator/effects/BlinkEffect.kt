package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.cos

class BlinkEffect : CompletionEffect {
    private var animator: ValueAnimator? = null
    private var blend = 0f
    private var fade = 1f

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        blend = 0f
        fade = 1f
        val pulses = params.repeat.coerceAtLeast(1)
        val amplitude = (params.intensityScale / MAX_INTENSITY).coerceIn(0f, 1f)
        animator =
            playFinish(
                view = view,
                durationMs = params.scaled(params.exitMs),
                interpolator = LinearInterpolator(),
                onUpdate = { f ->
                    blend = abs(cos((f * pulses * Math.PI).toFloat())) * amplitude
                    fade = 1f - f * 0.25f
                },
                onEnd = onEnd,
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        val maxAlpha = ctx.paint.alpha
        ctx.paint.color = lerpColor(ctx.baseColor, ctx.shinePaint.color, blend)
        ctx.paint.alpha = (maxAlpha * fade).toInt()
        ctx.renderer.drawFullRing(canvas, ctx.paint)
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }

    companion object {
        private const val MAX_INTENSITY = 1.5f
    }
}
