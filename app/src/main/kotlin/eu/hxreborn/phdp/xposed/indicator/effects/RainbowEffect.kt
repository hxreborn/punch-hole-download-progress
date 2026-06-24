package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.animation.LinearInterpolator

class RainbowEffect : CompletionEffect {
    private var animator: ValueAnimator? = null
    private var hue = 0f
    private var fade = 1f
    private val hsv = FloatArray(3)

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        hue = 0f
        fade = 1f
        val cycles = params.repeat.coerceAtLeast(1)
        val direction = if (params.reverse) -1f else 1f
        animator =
            playFinish(
                view = view,
                durationMs = params.scaled(params.holdMs + params.exitMs),
                interpolator = LinearInterpolator(),
                onUpdate = { f ->
                    hue = (f * 360f * cycles * direction) % 360f
                    fade = if (f < FADE_START) 1f else 1f - (f - FADE_START) / (1f - FADE_START)
                },
                onEnd = onEnd,
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        hsv[0] = if (hue < 0f) hue + 360f else hue
        hsv[1] = 1f
        hsv[2] = 1f
        val maxAlpha = ctx.paint.alpha
        ctx.paint.color = Color.HSVToColor(hsv)
        ctx.paint.alpha = (maxAlpha * fade).toInt()
        ctx.renderer.drawFullRing(canvas, ctx.paint)
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }

    companion object {
        private const val FADE_START = 0.6f
    }
}
