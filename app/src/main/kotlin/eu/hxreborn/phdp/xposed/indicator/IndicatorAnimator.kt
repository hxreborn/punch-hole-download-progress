package eu.hxreborn.phdp.xposed.indicator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log

class IndicatorAnimator(
    private val view: View,
) {
    var isFinishAnimating = false
        private set
    var displayAlpha = 1f
        private set
    var displayScale = 1f
        private set
    var segmentHighlight = -1
        private set
    var successColorBlend = 0f
        private set
    var completionPulseAlpha = 1f
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

    private var finishAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var errorAnimator: ValueAnimator? = null
    private var previewAnimator: ValueAnimator? = null
    private var previewDebounceRunnable: Runnable? = null
    private var geometryPreviewRunnable: Runnable? = null

    private val previewDebounceMs = 300L
    private val geometryPreviewDurationMs = 3000L

    companion object {
        private const val MAX_ANIMATION_MS = 800
        private const val SEGMENT_COUNT = 12
        private const val SEGMENT_GAP_DEGREES = 6f
        const val SEGMENT_ARC_DEGREES = (360f - SEGMENT_COUNT * SEGMENT_GAP_DEGREES) / SEGMENT_COUNT
    }

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
        from: Int,
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
        return ValueAnimator.ofInt(from, to).apply {
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
        style: String,
        holdMs: Int,
        exitMs: Int,
        pulseEnabled: Boolean,
        onComplete: () -> Unit,
    ) {
        cancelFinish()
        isFinishAnimating = true

        val intensity = 1.5f
        log("Starting finish animation: style=$style, hold=${holdMs}ms, exit=${exitMs}ms")

        displayAlpha = 1f
        displayScale = 1f
        segmentHighlight = -1
        successColorBlend = 1f
        completionPulseAlpha = 1f

        val startStyleAnimation = {
            when (style) {
                "snap" -> {
                    isFinishAnimating = false
                    onComplete()
                }

                "pop" -> {
                    animatePop(holdMs, exitMs, intensity, onComplete)
                }

                "segmented" -> {
                    animateSegmented(holdMs, exitMs, intensity, onComplete)
                }

                else -> {
                    animatePop(holdMs, exitMs, intensity, onComplete)
                }
            }
        }

        if (pulseEnabled && style != "snap") {
            animateCompletionPulse { startStyleAnimation() }
        } else {
            startStyleAnimation()
        }
    }

    private fun animatePop(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
        onComplete: () -> Unit,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val scalePhaseMs = (totalMs * 0.4f).toLong()
        val fadePhaseMs = (totalMs - scalePhaseMs).toLong()

        finishAnimator =
            play(
                values = floatArrayOf(0f, 1f),
                durationMs = scalePhaseMs,
                interpolator = OvershootInterpolator(2f * intensity),
                onUpdate = { fraction -> displayScale = 1f + (0.08f * intensity * fraction) },
                onEnd = {
                    finishAnimator =
                        play(
                            values = floatArrayOf(0f, 1f),
                            durationMs = fadePhaseMs,
                            onUpdate = { fraction ->
                                displayScale = 1f + (0.08f * intensity * (1f - fraction * 0.5f))
                                displayAlpha = 1f - fraction
                            },
                            onEnd = { finishEnd(onComplete) },
                        )
                },
            )
    }

    private fun animateSegmented(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
        onComplete: () -> Unit,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val cascadePhaseMs = (totalMs * 0.6f).toLong()
        val fadePhaseMs = (totalMs - cascadePhaseMs).toLong()

        finishAnimator =
            playInt(
                from = 0,
                to = SEGMENT_COUNT + 2,
                durationMs = cascadePhaseMs,
                onUpdate = { segment ->
                    segmentHighlight = segment
                    successColorBlend = intensity * 0.4f
                },
                onEnd = {
                    segmentHighlight = -1
                    finishAnimator =
                        play(
                            values = floatArrayOf(1f, 0f),
                            durationMs = fadePhaseMs,
                            onUpdate = { alpha -> displayAlpha = alpha },
                            onEnd = { finishEnd(onComplete) },
                        )
                },
            )
    }

    private fun finishEnd(onComplete: () -> Unit) {
        isFinishAnimating = false
        resetFinishState()
        onComplete()
        view.invalidate()
    }

    private fun animateCompletionPulse(onComplete: () -> Unit) {
        pulseAnimator?.cancel()
        pulseAnimator =
            play(
                values = floatArrayOf(1f, 0.7f, 1f),
                durationMs = 400,
                onUpdate = { alpha -> completionPulseAlpha = alpha },
                onEnd = {
                    completionPulseAlpha = 1f
                    onComplete()
                },
            )
    }

    fun cancelFinish() {
        finishAnimator?.cancel()
        finishAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
        isFinishAnimating = false
        resetFinishState()
    }

    private fun resetFinishState() {
        displayAlpha = 1f
        displayScale = 1f
        segmentHighlight = -1
        successColorBlend = 0f
        completionPulseAlpha = 1f
    }

    fun startError(onComplete: () -> Unit) {
        if (isFinishAnimating || isErrorAnimating) return
        isErrorAnimating = true
        log("IndicatorAnimator: startError()")

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

    fun startDynamicPreviewAnim(
        finishStyle: String,
        holdMs: Int,
        exitMs: Int,
        pulseEnabled: Boolean,
    ) {
        log("IndicatorAnimator: startDynamicPreviewAnim() - debouncing")

        previewDebounceRunnable?.let { view.removeCallbacks(it) }
        previewDebounceRunnable =
            Runnable {
                startDynamicPreviewAnimInternal(finishStyle, holdMs, exitMs, pulseEnabled)
            }
        view.postDelayed(previewDebounceRunnable, previewDebounceMs)
    }

    private fun startDynamicPreviewAnimInternal(
        finishStyle: String,
        holdMs: Int,
        exitMs: Int,
        pulseEnabled: Boolean,
    ) {
        log("IndicatorAnimator: startDynamicPreviewAnimInternal()")
        cancelDynamicPreviewAnim()
        previewMode = PreviewMode.ANIMATING
        previewProgress = 0

        previewAnimator =
            playInt(
                from = 0,
                to = 100,
                durationMs = 800,
                interpolator = AccelerateDecelerateInterpolator(),
                onUpdate = { progress -> previewProgress = progress },
                onEnd = {
                    previewProgress = 100
                    startFinish(finishStyle, holdMs, exitMs, pulseEnabled) {
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

    fun showStaticPreviewAnim() {
        log("IndicatorAnimator: showStaticPreviewAnim() - debouncing")

        geometryPreviewRunnable?.let { view.removeCallbacks(it) }

        previewMode = PreviewMode.GEOMETRY
        view.invalidate()

        geometryPreviewRunnable =
            Runnable {
                previewMode = PreviewMode.NONE
                geometryPreviewRunnable = null
                view.invalidate()
            }
        view.postDelayed(geometryPreviewRunnable, geometryPreviewDurationMs)
    }

    fun cancelStaticPreviewAnim() {
        geometryPreviewRunnable?.let { view.removeCallbacks(it) }
        geometryPreviewRunnable = null
        if (previewMode == PreviewMode.GEOMETRY) previewMode = PreviewMode.NONE
    }

    fun cancelAll() {
        cancelFinish()
        cancelError()
        cancelDynamicPreviewAnim()
        cancelStaticPreviewAnim()
    }
}
