package eu.hxreborn.phdp.xposed.indicator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import eu.hxreborn.phdp.util.logDebug
import eu.hxreborn.phdp.xposed.indicator.effects.CompletionEffect
import eu.hxreborn.phdp.xposed.indicator.effects.CompletionEffects
import eu.hxreborn.phdp.xposed.indicator.effects.EffectParams

class IndicatorAnimator(
    private val view: View,
) {
    var isFinishAnimating = false
        private set

    var activeEffect: CompletionEffect? = null
        private set

    var isErrorAnimating = false
        private set
    var errorAlpha = 0f
        private set

    enum class PreviewMode { NONE, ANIMATING, GEOMETRY }

    var previewMode = PreviewMode.NONE
        private set
    var previewProgress = 0
        private set

    val isPreviewAnimating: Boolean get() = previewMode == PreviewMode.ANIMATING
    val isGeometryPreviewActive: Boolean get() = previewMode == PreviewMode.GEOMETRY

    private var errorAnimator: ValueAnimator? = null
    private var previewAnimator: ValueAnimator? = null
    private var previewDebounceRunnable: Runnable? = null
    private var geometryPreviewRunnable: Runnable? = null

    private val previewDebounceMs = 300L
    private val geometryPreviewDurationMs = 3000L

    private fun play(
        values: FloatArray,
        durationMs: Long,
        interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        onUpdate: (Float) -> Unit,
        onEnd: (() -> Unit)? = null,
    ): ValueAnimator {
        // Handle 0-duration edge case: ValueAnimator may skip onAnimationEnd
        if (durationMs <= 0) {
            onUpdate(values.last())
            view.invalidate()
            onEnd?.invoke()
            return ValueAnimator() // Return dummy animator
        }
        return ValueAnimator.ofFloat(*values).apply {
            duration = durationMs
            this.interpolator = interpolator
            addUpdateListener {
                onUpdate(it.animatedValue as Float)
                view.invalidate()
            }
            onEnd?.let { callback ->
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: Animator) = callback()
                    },
                )
            }
            start()
        }
    }

    private fun playInt(
        to: Int,
        durationMs: Long,
        interpolator: TimeInterpolator = LinearInterpolator(),
        onUpdate: (Int) -> Unit,
        onEnd: (() -> Unit)? = null,
    ): ValueAnimator {
        if (durationMs <= 0) {
            onUpdate(to)
            view.invalidate()
            onEnd?.invoke()
            return ValueAnimator()
        }
        return ValueAnimator.ofInt(0, to).apply {
            duration = durationMs
            this.interpolator = interpolator
            addUpdateListener {
                onUpdate(it.animatedValue as Int)
                view.invalidate()
            }
            onEnd?.let { callback ->
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: Animator) = callback()
                    },
                )
            }
            start()
        }
    }

    fun startFinish(
        params: EffectParams,
        onComplete: () -> Unit,
    ) {
        cancelFinish()
        isFinishAnimating = true
        logDebug {
            "Starting finish animation: style=${params.style}, " +
                "hold=${params.holdMs}ms, exit=${params.exitMs}ms"
        }
        val effect = CompletionEffects.create(params.style)
        activeEffect = effect
        effect.start(view, params) {
            activeEffect = null
            finishEnd(onComplete)
        }
    }

    private fun finishEnd(onComplete: () -> Unit) {
        isFinishAnimating = false
        onComplete()
        view.invalidate()
    }

    fun cancelFinish() {
        activeEffect?.cancel()
        activeEffect = null
        isFinishAnimating = false
    }

    fun startError(onComplete: () -> Unit) {
        if (isFinishAnimating || isErrorAnimating) return
        isErrorAnimating = true
        logDebug { "IndicatorAnimator: startError()" }

        errorAnimator?.cancel()
        errorAnimator =
            play(
                values = floatArrayOf(0f, 1f, 0f, 1f, 0f),
                durationMs = 600,
                interpolator = LinearInterpolator(),
                onUpdate = { alpha -> errorAlpha = alpha },
                onEnd = {
                    isErrorAnimating = false
                    errorAlpha = 0f
                    onComplete()
                    view.invalidate()
                },
            )
    }

    fun cancelError() {
        errorAnimator?.cancel()
        errorAnimator = null
        isErrorAnimating = false
        errorAlpha = 0f
    }

    fun startDynamicPreviewAnim(params: EffectParams) {
        logDebug { "IndicatorAnimator: startDynamicPreviewAnim() - debouncing" }

        previewDebounceRunnable?.let { view.removeCallbacks(it) }
        previewDebounceRunnable =
            Runnable {
                startDynamicPreviewAnimInternal(params)
            }
        view.postDelayed(previewDebounceRunnable, previewDebounceMs)
    }

    private fun startDynamicPreviewAnimInternal(params: EffectParams) {
        logDebug { "IndicatorAnimator: startDynamicPreviewAnimInternal()" }
        cancelDynamicPreviewAnim()
        previewMode = PreviewMode.ANIMATING
        previewProgress = 0

        previewAnimator =
            playInt(
                to = 100,
                durationMs = 800,
                interpolator = AccelerateDecelerateInterpolator(),
                onUpdate = { progress -> previewProgress = progress },
                onEnd = {
                    previewProgress = 100
                    startFinish(params) {
                        view.postDelayed(
                            {
                                previewMode = PreviewMode.NONE
                                previewProgress = 0
                                view.invalidate()
                            },
                            200,
                        )
                    }
                },
            )
    }

    fun cancelDynamicPreviewAnim() {
        previewDebounceRunnable?.let { view.removeCallbacks(it) }
        previewDebounceRunnable = null
        previewAnimator?.cancel()
        previewAnimator = null
        previewMode = PreviewMode.NONE
        previewProgress = 0
    }

    fun showStaticPreviewAnim(autoHide: Boolean = true) {
        logDebug { "IndicatorAnimator: showStaticPreviewAnim(autoHide=$autoHide)" }

        geometryPreviewRunnable?.let { view.removeCallbacks(it) }
        geometryPreviewRunnable = null

        previewMode = PreviewMode.GEOMETRY
        view.invalidate()

        if (autoHide) {
            geometryPreviewRunnable =
                Runnable {
                    previewMode = PreviewMode.NONE
                    geometryPreviewRunnable = null
                    view.invalidate()
                }
            view.postDelayed(geometryPreviewRunnable, geometryPreviewDurationMs)
        }
    }

    fun cancelStaticPreviewAnim() {
        geometryPreviewRunnable?.let { view.removeCallbacks(it) }
        geometryPreviewRunnable = null
        if (previewMode == PreviewMode.GEOMETRY) {
            previewMode = PreviewMode.NONE
            view.invalidate()
        }
    }

    fun cancelAll() {
        cancelFinish()
        cancelError()
        cancelDynamicPreviewAnim()
        cancelStaticPreviewAnim()
    }
}
