package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.View

class SegmentedEffect : CompletionEffect {
    private var phase1: ValueAnimator? = null
    private var phase2: ValueAnimator? = null
    private var cascade = -1
    private var fade = 1f
    private var count = 4
    private var gap = 2f
    private var arc = 88f
    private var reverse = false

    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) {
        count = params.segmentCount
        gap = params.segmentGapDegrees
        arc = (360f - count * gap) / count
        reverse = params.reverse
        val totalMs = params.scaled(params.holdMs + params.exitMs).coerceAtMost(MAX_MS)
        val cascadeMs = (totalMs * 0.6f).toLong()
        val fadeMs = totalMs - cascadeMs
        cascade = -1
        fade = 1f
        phase1 =
            playFinishInt(
                view = view,
                to = count + 2,
                durationMs = cascadeMs,
                onUpdate = { step -> cascade = step },
                onEnd = {
                    cascade = -1
                    phase2 =
                        playFinish(
                            view = view,
                            durationMs = fadeMs,
                            onUpdate = { f -> fade = 1f - f },
                            onEnd = onEnd,
                        )
                },
            )
    }

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
        val highlight = if (reverse && cascade >= 0) count - 1 - cascade else cascade
        ctx.paint.color = lerpColor(ctx.baseColor, ctx.shinePaint.color, BLEND)
        ctx.renderer.drawSegmented(
            canvas,
            count,
            gap,
            arc,
            highlight,
            ctx.paint,
            ctx.shinePaint,
            fade,
        )
    }

    override fun cancel() {
        phase1?.cancel()
        phase1 = null
        phase2?.cancel()
        phase2 = null
    }

    companion object {
        private const val MAX_MS = 800L
        private const val BLEND = 0.6f
    }
}
