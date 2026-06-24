package eu.hxreborn.phdp.xposed.indicator.effects

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import eu.hxreborn.phdp.xposed.indicator.RingRenderer
import kotlin.math.roundToInt

interface CompletionEffect {
    fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    )

    fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    )

    fun cancel()
}

class FinishDrawContext(
    var renderer: RingRenderer,
    var bounds: RectF,
    var paint: Paint,
    var shinePaint: Paint,
    var baseColor: Int,
    var clockwise: Boolean,
)

object CompletionEffects {
    fun create(style: String): CompletionEffect =
        when (style) {
            "snap" -> SnapEffect()
            "pop" -> PopEffect()
            "segmented" -> SegmentedEffect()
            "wipe" -> ShutterWipeEffect()
            "spinoff" -> SpinOffEffect()
            "blink" -> BlinkEffect()
            "sweep" -> SweepEffect()
            "rainbow" -> RainbowEffect()
            else -> PopEffect()
        }
}

fun playFinish(
    view: View,
    durationMs: Long,
    interpolator: TimeInterpolator = LinearInterpolator(),
    onUpdate: (Float) -> Unit,
    onEnd: () -> Unit,
): ValueAnimator? {
    if (durationMs <= 0L) {
        onUpdate(1f)
        view.invalidate()
        onEnd()
        return null
    }
    return ValueAnimator.ofFloat(0f, 1f).apply {
        duration = durationMs
        this.interpolator = interpolator
        addUpdateListener {
            onUpdate(it.animatedValue as Float)
            view.invalidate()
        }
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) = onEnd()
            },
        )
        start()
    }
}

fun playFinishInt(
    view: View,
    to: Int,
    durationMs: Long,
    interpolator: TimeInterpolator = LinearInterpolator(),
    onUpdate: (Int) -> Unit,
    onEnd: () -> Unit,
): ValueAnimator? {
    if (durationMs <= 0L) {
        onUpdate(to)
        view.invalidate()
        onEnd()
        return null
    }
    return ValueAnimator.ofFloat(0f, to.toFloat()).apply {
        duration = durationMs
        this.interpolator = interpolator
        addUpdateListener {
            onUpdate((it.animatedValue as Float).roundToInt())
            view.invalidate()
        }
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) = onEnd()
            },
        )
        start()
    }
}

fun colorWithAlpha(
    color: Int,
    alphaFraction: Float,
): Int {
    val a = (Color.alpha(color) * alphaFraction).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (a shl 24)
}

fun lerpColor(
    from: Int,
    to: Int,
    t: Float,
): Int {
    val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
    val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
    val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
    return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}
