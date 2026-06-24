package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator

class PopEffect : CompletionEffect {
    private var phase1: ValueAnimator? = null
    private var phase2: ValueAnimator? = null
    private var scale = 1f
    private var alpha = 1f
    private var peak = SCALE_PEAK

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        val totalMs = params.scaled(params.holdMs + params.exitMs).coerceAtMost(MAX_MS)
        val scaleMs = (totalMs * 0.4f).toLong()
        val fadeMs = totalMs - scaleMs
        scale = 1f
        alpha = 1f
        peak = SCALE_PEAK * params.intensityScale
        phase1 =
            playFinish(
                view = view,
                durationMs = scaleMs,
                interpolator = OvershootInterpolator(2f),
                onUpdate = { f -> scale = 1f + peak * f },
                onEnd = {
                    phase2 =
                        playFinish(
                            view = view,
                            durationMs = fadeMs,
                            interpolator = AccelerateDecelerateInterpolator(),
                            onUpdate = { f ->
                                scale = 1f + peak * (1f - f * 0.5f)
                                alpha = 1f - f
                            },
                            onEnd = onEnd,
                        )
                },
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        canvas.save()
        canvas.scale(scale, scale, ctx.bounds.centerX(), ctx.bounds.centerY())
        ctx.paint.color = ctx.shinePaint.color
        ctx.paint.alpha = (ctx.paint.alpha * alpha).toInt()
        ctx.renderer.drawFullRing(canvas, ctx.paint)
        canvas.restore()
    }

    override fun cancel() {
        phase1?.cancel()
        phase1 = null
        phase2?.cancel()
        phase2 = null
    }

    companion object {
        private const val MAX_MS = 800L
        private const val SCALE_PEAK = 0.12f
    }
}
