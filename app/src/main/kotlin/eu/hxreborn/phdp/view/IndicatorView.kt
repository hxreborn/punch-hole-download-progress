package eu.hxreborn.phdp.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import eu.hxreborn.phdp.PunchHoleDownloadProgressModule.Companion.log
import eu.hxreborn.phdp.hook.SystemUIHook
import eu.hxreborn.phdp.prefs.PrefsManager
import kotlin.math.pow

class IndicatorView(
    context: Context,
) : View(context) {
    private var cutoutPath: Path? = null
    private val scaledPath = Path()
    private val scaleMatrix = Matrix()
    private val pathBounds = RectF()
    private val arcBounds = RectF()
    private var drawCount = 0
    private val density = resources.displayMetrics.density

    // Finish animation state
    private var finishAnimator: ValueAnimator? = null
    private var isFinishAnimating = false
    private var displayAlpha = 1f
    private var displayScale = 1f
    private var shineAngle = -30f
    private var segmentHighlight = -1
    private var successColorBlend = 0f

    // Completion pulse animation state
    private var pulseAnimator: ValueAnimator? = null
    private var completionPulseAlpha = 1f

    // Error animation state
    private var errorAnimator: ValueAnimator? = null
    private var isErrorAnimating = false
    private var errorAlpha = 0f

    // Preview animation state
    private var previewAnimator: ValueAnimator? = null
    private var isPreviewAnimating = false
    private var previewProgress = 0
    private var previewDebounceRunnable: Runnable? = null
    private val previewDebounceMs = 300L // prevents animation spam when dragging sliders

    // Geometry preview state
    private var geometryPreviewRunnable: Runnable? = null
    private var isGeometryPreviewActive = false
    private val geometryPreviewDurationMs = 3000L // enough time to see geometry changes before ring hides

    // Minimum visibility window for fast downloads
    private var downloadStartTime = 0L
    private var pendingFinishRunnable: Runnable? = null
    private val minVisibilityMs: Long
        get() = if (PrefsManager.minVisibilityEnabled) PrefsManager.minVisibilityMs.toLong() else 0L

    // Multi-download count
    @Volatile
    var activeDownloadCount: Int = 0
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    // Current filename (from leading download)
    @Volatile
    var currentFilename: String? = null
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    // Power saver state (set from SystemUIHook)
    @Volatile
    var isPowerSaveActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    // Segmented style: 12 segments with 6° gaps = 24° arc per segment
    private val segmentCount = 12
    private val segmentGapDegrees = 6f
    private val segmentArcDegrees = (360f - segmentCount * segmentGapDegrees) / segmentCount

    // Progress: 0 = hidden, 1-99 = arc, 100 = finish animation
    @Volatile
    var progress: Int = 0
        set(value) {
            val newValue = value.coerceIn(0, 100)
            if (field != newValue) {
                val oldValue = field
                field = newValue
                log("IndicatorView: progress = $newValue")

                // Track download start for minimum visibility
                if (oldValue == 0 && newValue > 0) {
                    downloadStartTime = System.currentTimeMillis()
                    pendingFinishRunnable?.let { removeCallbacks(it) }
                    pendingFinishRunnable = null
                }

                if (newValue == 100 && !isFinishAnimating) {
                    val elapsed = System.currentTimeMillis() - downloadStartTime
                    val remaining = minVisibilityMs - elapsed
                    if (remaining > 0 && downloadStartTime > 0) {
                        // Delay finish animation to meet minimum visibility
                        log("IndicatorView: fast download, delaying finish by ${remaining}ms")
                        pendingFinishRunnable =
                            Runnable {
                                pendingFinishRunnable = null
                                startFinishAnimation()
                            }
                        postDelayed(pendingFinishRunnable, remaining)
                    } else {
                        startFinishAnimation()
                    }
                } else if (newValue in 1..99 && isFinishAnimating) {
                    cancelFinishAnimation()
                } else if (newValue == 0) {
                    // Reset start time when progress clears
                    downloadStartTime = 0L
                    pendingFinishRunnable?.let { removeCallbacks(it) }
                    pendingFinishRunnable = null
                }
                post { invalidate() }
            }
        }

    // App visible flag - show ring in preview mode
    @Volatile
    var appVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                log("IndicatorView: appVisible = $value")
                post { invalidate() }
            }
        }

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

    private val shinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

    private val errorPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

    private val countPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

    private val idlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

    private val percentPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize =
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    8f,
                    resources.displayMetrics,
                )
        }

    private val filenamePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT
            textSize =
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    7f,
                    resources.displayMetrics,
                )
        }

    // Max width for filename text (calculated from screen width)
    private val maxFilenameWidth: Float
        get() = resources.displayMetrics.widthPixels * 0.25f

    init {
        log("IndicatorView: constructor called")
        updatePaintFromPrefs()

        PrefsManager.onPrefsChanged = {
            post {
                log("IndicatorView: prefs changed, updating...")
                updatePaintFromPrefs()
                recalculateScaledPath()
                invalidate()
            }
        }

        PrefsManager.onTestProgressChanged = { testProgress ->
            post {
                log("IndicatorView: test progress = $testProgress")
                progress = testProgress
            }
        }
    }

    private fun updatePaintFromPrefs() {
        // Apply power saver dim if active
        val effectiveOpacity =
            if (isPowerSaveActive && PrefsManager.powerSaverMode == "dim") {
                (PrefsManager.opacity * 0.5f).toInt()
            } else {
                PrefsManager.opacity
            }

        glowPaint.apply {
            color = PrefsManager.color
            alpha = (effectiveOpacity * 255 / 100)
            strokeWidth = PrefsManager.strokeWidth * density
            maskFilter = null
        }

        shinePaint.apply {
            color =
                if (PrefsManager.finishUseFlashColor) {
                    PrefsManager.finishFlashColor
                } else {
                    brightenColor(PrefsManager.color, 0.5f)
                }
            alpha = 255
            strokeWidth = PrefsManager.strokeWidth * density * 1.2f
        }

        errorPaint.apply {
            color = PrefsManager.errorColor
            strokeWidth = PrefsManager.strokeWidth * density * 1.5f
        }

        countPaint.apply {
            color = PrefsManager.color
            textSize = 10f * density
        }

        idlePaint.apply {
            color = PrefsManager.color
            alpha = (PrefsManager.idleRingOpacity * 255 / 100)
            strokeWidth = PrefsManager.strokeWidth * density
        }

        log(
            "Paint updated: color=${Integer.toHexString(PrefsManager.color)}, " +
                "opacity=$effectiveOpacity, stroke=${PrefsManager.strokeWidth}, " +
                "gap=${PrefsManager.ringGap}",
        )
    }

    private fun brightenColor(
        color: Int,
        factor: Float,
    ): Int {
        val r = ((Color.red(color) + (255 - Color.red(color)) * factor)).toInt().coerceIn(0, 255)
        val g = ((Color.green(color) + (255 - Color.green(color)) * factor)).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color) + (255 - Color.blue(color)) * factor)).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    private fun recalculateScaledPath() {
        cutoutPath?.let { path ->
            path.computeBounds(pathBounds, true)
            scaleMatrix.setScale(
                PrefsManager.ringGap,
                PrefsManager.ringGap,
                pathBounds.centerX(),
                pathBounds.centerY(),
            )
            scaledPath.reset()
            path.transform(scaleMatrix, scaledPath)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log("IndicatorView: onAttachedToWindow()")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        log("IndicatorView: onDetachedFromWindow()")
        cancelFinishAnimation()
        cancelErrorAnimation()
        cancelPreviewAnimation()
        cancelGeometryPreview()
        pendingFinishRunnable?.let { removeCallbacks(it) }
        pendingFinishRunnable = null
        PrefsManager.onPrefsChanged = null
        PrefsManager.onTestProgressChanged = null
        PrefsManager.onTestErrorChanged = null
        PrefsManager.onPreviewTriggered = null
        PrefsManager.onGeometryPreviewTriggered = null
        SystemUIHook.detach()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val displayCutout = insets.displayCutout

        if (displayCutout == null) {
            log("WARNING: No display cutout available!")
            cutoutPath = null
            return super.onApplyWindowInsets(insets)
        }

        cutoutPath = displayCutout.cutoutPath

        if (cutoutPath == null) {
            log("WARNING: cutoutPath is null (device may not support cutoutPath API)")
            return super.onApplyWindowInsets(insets)
        }

        cutoutPath?.let { path ->
            path.computeBounds(pathBounds, true)
            log("Cutout path bounds: ${pathBounds.width()}x${pathBounds.height()} at (${pathBounds.centerX()}, ${pathBounds.centerY()})")
            recalculateScaledPath()
        }

        invalidate()
        return super.onApplyWindowInsets(insets)
    }

    private fun startFinishAnimation() {
        cancelFinishAnimation()
        isFinishAnimating = true

        val style = PrefsManager.finishStyle
        val holdMs = PrefsManager.finishHoldMs
        val exitMs = PrefsManager.finishExitMs
        val intensity =
            when (PrefsManager.finishIntensity) {
                "low" -> 0.7f
                "high" -> 1.5f
                else -> 1.0f
            }

        log("Starting finish animation: style=$style, hold=${holdMs}ms, exit=${exitMs}ms, intensity=$intensity")

        // Reset animation state
        displayAlpha = 1f
        displayScale = 1f
        shineAngle = -30f
        segmentHighlight = -1
        successColorBlend = 1f // Immediately show success color (no overlap with progress color)
        completionPulseAlpha = 1f

        // Helper to start the selected style animation
        val startStyleAnimation = {
            when (style) {
                "snap" -> {
                    // Instant hide
                    progress = 0
                    isFinishAnimating = false
                }

                "hold_fade" -> {
                    animateHoldFade(holdMs, exitMs)
                }

                "pop" -> {
                    animatePop(holdMs, exitMs, intensity)
                }

                "pulse" -> {
                    animatePulse(holdMs, exitMs, intensity)
                }

                "shine_sweep" -> {
                    animateShineSweep(holdMs, exitMs, intensity)
                }

                "segmented" -> {
                    animateSegmented(holdMs, exitMs, intensity)
                }

                else -> {
                    animateHoldFade(holdMs, exitMs)
                }
            }
        }

        // Run completion pulse before style animation if enabled
        if (PrefsManager.completionPulseEnabled && style != "snap") {
            animateCompletionPulse { startStyleAnimation() }
        } else {
            startStyleAnimation()
        }
    }

    private fun animateHoldFade(
        holdMs: Int,
        exitMs: Int,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val holdFraction = holdMs.toFloat() / totalMs

        finishAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = totalMs.toLong()
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    displayAlpha =
                        if (fraction < holdFraction) {
                            1f
                        } else {
                            val exitFraction = (fraction - holdFraction) / (1f - holdFraction)
                            1f - exitFraction
                        }
                    invalidate()
                }
                addListener(finishAnimationEndListener())
                start()
            }
    }

    private fun animatePop(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val scalePhaseMs = (totalMs * 0.4f).toInt()
        val fadePhaseMs = totalMs - scalePhaseMs

        // Phase 1: Scale pop with overshoot
        finishAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = scalePhaseMs.toLong()
                interpolator = OvershootInterpolator(2f * intensity)
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    displayScale = 1f + (0.08f * intensity * fraction)
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Phase 2: Settle back and fade
                            finishAnimator =
                                ValueAnimator.ofFloat(0f, 1f).apply {
                                    duration = fadePhaseMs.toLong()
                                    interpolator = AccelerateDecelerateInterpolator()
                                    addUpdateListener { animator ->
                                        val fraction = animator.animatedValue as Float
                                        displayScale = 1f + (0.08f * intensity * (1f - fraction * 0.5f))
                                        displayAlpha = 1f - fraction
                                        invalidate()
                                    }
                                    addListener(finishAnimationEndListener())
                                    start()
                                }
                        }
                    },
                )
                start()
            }
    }

    private fun animatePulse(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val holdFraction = holdMs.toFloat() / totalMs

        finishAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = totalMs.toLong()
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    if (fraction < holdFraction) {
                        displayAlpha = 1f
                    } else {
                        val pulseFraction = (fraction - holdFraction) / (1f - holdFraction)
                        displayAlpha = 1f - pulseFraction
                    }
                    invalidate()
                }
                addListener(finishAnimationEndListener())
                start()
            }
    }

    private fun animateShineSweep(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val sweepPhaseMs = (totalMs * 0.6f).toInt()
        val fadePhaseMs = totalMs - sweepPhaseMs

        // Phase 1: Shine sweeps around the ring (-30° start to 390° gives full circle + overshoot)
        finishAnimator =
            ValueAnimator.ofFloat(-30f, 390f).apply {
                duration = sweepPhaseMs.toLong()
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    shineAngle = animator.animatedValue as Float
                    successColorBlend = intensity * 0.3f
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Phase 2: Fade out
                            shineAngle = -30f
                            finishAnimator =
                                ValueAnimator.ofFloat(1f, 0f).apply {
                                    duration = fadePhaseMs.toLong()
                                    interpolator = AccelerateDecelerateInterpolator()
                                    addUpdateListener { animator ->
                                        displayAlpha = animator.animatedValue as Float
                                        successColorBlend = intensity * 0.3f * displayAlpha
                                        invalidate()
                                    }
                                    addListener(finishAnimationEndListener())
                                    start()
                                }
                        }
                    },
                )
                start()
            }
    }

    private fun animateSegmented(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val cascadePhaseMs = (totalMs * 0.6f).toInt()
        val fadePhaseMs = totalMs - cascadePhaseMs

        // Phase 1: Cascade highlight around segments
        finishAnimator =
            ValueAnimator.ofInt(0, segmentCount + 2).apply {
                duration = cascadePhaseMs.toLong()
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    segmentHighlight = animator.animatedValue as Int
                    successColorBlend = intensity * 0.4f
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Phase 2: Fade out
                            segmentHighlight = -1
                            finishAnimator =
                                ValueAnimator.ofFloat(1f, 0f).apply {
                                    duration = fadePhaseMs.toLong()
                                    interpolator = AccelerateDecelerateInterpolator()
                                    addUpdateListener { animator ->
                                        displayAlpha = animator.animatedValue as Float
                                        invalidate()
                                    }
                                    addListener(finishAnimationEndListener())
                                    start()
                                }
                        }
                    },
                )
                start()
            }
    }

    private fun finishAnimationEndListener() =
        object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isFinishAnimating = false
                progress = 0
                resetAnimationState()
                invalidate()
            }
        }

    private fun cancelFinishAnimation() {
        finishAnimator?.cancel()
        finishAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
        isFinishAnimating = false
        resetAnimationState()
    }

    private fun resetAnimationState() {
        displayAlpha = 1f
        displayScale = 1f
        shineAngle = -30f
        segmentHighlight = -1
        successColorBlend = 0f
        completionPulseAlpha = 1f
    }

    // Pulse alpha 100% → 70% → 100% in 400ms for subtle completion feedback
    private fun animateCompletionPulse(onComplete: () -> Unit) {
        pulseAnimator?.cancel()
        pulseAnimator =
            ValueAnimator.ofFloat(1f, 0.7f, 1f).apply {
                duration = 400
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    completionPulseAlpha = animator.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            completionPulseAlpha = 1f
                            onComplete()
                        }
                    },
                )
                start()
            }
    }

    // Apply easing to progress value
    private fun applyEasing(progress: Int): Float {
        val p = progress / 100f
        return when (PrefsManager.progressEasing) {
            "accelerate" -> p * p
            "decelerate" -> 1f - (1f - p) * (1f - p)
            "ease_in_out" -> if (p < 0.5f) 2f * p * p else 1f - (-2f * p + 2f).pow(2) / 2f
            else -> p // linear
        }
    }

    // Start preview animation (debounced, 0 -> 100 -> finish animation)
    fun startPreview() {
        log("IndicatorView: startPreview() - debouncing")

        // Cancel any pending debounce
        previewDebounceRunnable?.let { removeCallbacks(it) }

        // Schedule the actual preview after debounce period
        previewDebounceRunnable =
            Runnable {
                startPreviewInternal()
            }
        postDelayed(previewDebounceRunnable, previewDebounceMs)
    }

    private fun startPreviewInternal() {
        log("IndicatorView: startPreviewInternal()")
        cancelPreviewAnimation()
        isPreviewAnimating = true
        previewProgress = 0

        // Animate progress from 0 to 100
        previewAnimator =
            ValueAnimator.ofInt(0, 100).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    previewProgress = animator.animatedValue as Int
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Trigger finish animation at 100%
                            previewProgress = 100
                            startFinishAnimation()
                            // Clear preview state after finish animation (~800ms) + buffer
                            postDelayed({
                                isPreviewAnimating = false
                                previewProgress = 0
                                invalidate()
                            }, 1000)
                        }
                    },
                )
                start()
            }
    }

    private fun cancelPreviewAnimation() {
        previewDebounceRunnable?.let { removeCallbacks(it) }
        previewDebounceRunnable = null
        previewAnimator?.cancel()
        previewAnimator = null
        isPreviewAnimating = false
        previewProgress = 0
    }

    // Show geometry preview (ring at 100% for 3 seconds, debounced)
    fun showGeometryPreview() {
        log("IndicatorView: showGeometryPreview() - debouncing")

        // Cancel any pending geometry preview hide
        geometryPreviewRunnable?.let { removeCallbacks(it) }

        // Show ring immediately at 100%
        isGeometryPreviewActive = true
        invalidate()

        // Schedule hide after duration
        geometryPreviewRunnable =
            Runnable {
                isGeometryPreviewActive = false
                geometryPreviewRunnable = null
                invalidate()
            }
        postDelayed(geometryPreviewRunnable, geometryPreviewDurationMs)
    }

    private fun cancelGeometryPreview() {
        geometryPreviewRunnable?.let { removeCallbacks(it) }
        geometryPreviewRunnable = null
        isGeometryPreviewActive = false
    }

    // Show error flash animation
    fun showError() {
        if (isFinishAnimating || isErrorAnimating) return
        isErrorAnimating = true
        log("IndicatorView: showError()")

        // Two blinks in 600ms (off→on→off→on→off, 5 keyframes at 120ms each)
        errorAnimator?.cancel()
        errorAnimator =
            ValueAnimator.ofFloat(0f, 1f, 0f, 1f, 0f).apply {
                duration = 600
                interpolator = LinearInterpolator()
                addUpdateListener {
                    errorAlpha = it.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            isErrorAnimating = false
                            errorAlpha = 0f
                            progress = 0
                            invalidate()
                        }
                    },
                )
                start()
            }
    }

    private fun cancelErrorAnimation() {
        errorAnimator?.cancel()
        errorAnimator = null
        isErrorAnimating = false
        errorAlpha = 0f
    }

    override fun onDraw(canvas: Canvas) {
        drawCount++

        if (!PrefsManager.enabled) {
            if (drawCount == 1) log("IndicatorView: disabled, skipping draw")
            return
        }

        if (cutoutPath == null) {
            if (drawCount == 1) log("First draw: NO cutoutPath, nothing to draw")
            return
        }

        // Skip animations in power saver disable mode
        if (isPowerSaveActive && PrefsManager.powerSaverMode == "disable") {
            return
        }

        // Idle ring mode: draw faint ring when no activity
        if (PrefsManager.idleRingEnabled && progress == 0 && !isFinishAnimating && !isErrorAnimating && !appVisible) {
            canvas.drawPath(scaledPath, idlePaint)
            return
        }

        // Error animation mode
        if (isErrorAnimating) {
            scaledPath.computeBounds(arcBounds, true)
            errorPaint.alpha = (errorAlpha * 255).toInt()
            canvas.drawPath(scaledPath, errorPaint)
            return
        }

        // Determine effective progress (preview overrides real progress)
        val effectiveProgress =
            when {
                isGeometryPreviewActive -> 100
                isPreviewAnimating -> previewProgress
                else -> progress
            }

        // Visibility logic:
        // - Animation in progress: always show
        // - Geometry preview: show at 100%
        // - Preview animating: always show
        // - Active download/test (1-99): always show
        // - Waiting for minimum visibility (100% with pending finish): show
        // - Otherwise: hidden
        val shouldDraw =
            when {
                isFinishAnimating -> true
                isGeometryPreviewActive -> true
                isPreviewAnimating -> true
                effectiveProgress in 1..99 -> true
                pendingFinishRunnable != null -> true
                else -> false
            }

        if (!shouldDraw) {
            return
        }

        // Apply animation transforms
        canvas.save()

        if (displayScale != 1f) {
            scaledPath.computeBounds(arcBounds, true)
            canvas.scale(displayScale, displayScale, arcBounds.centerX(), arcBounds.centerY())
        }

        // Apply alpha and success color
        val animatedPaint =
            Paint(glowPaint).apply {
                alpha = (glowPaint.alpha * displayAlpha * completionPulseAlpha).toInt()
                // Apply success color blend
                if (successColorBlend > 0f) {
                    val successColor =
                        if (PrefsManager.finishUseFlashColor) {
                            PrefsManager.finishFlashColor
                        } else {
                            brightenColor(PrefsManager.color, successColorBlend)
                        }
                    color = blendColors(PrefsManager.color, successColor, successColorBlend)
                }
            }

        if (isFinishAnimating) {
            // Finish animation: draw based on style
            drawFinishAnimation(canvas, animatedPaint)
        } else {
            // Progress mode: draw arc based on effective progress with easing
            scaledPath.computeBounds(arcBounds, true)
            val easedProgress = applyEasing(effectiveProgress)
            val sweepAngle = 360f * easedProgress
            val actualSweep = if (PrefsManager.clockwise) sweepAngle else -sweepAngle
            canvas.drawArc(arcBounds, -90f, actualSweep, false, animatedPaint)

            // Draw download count badge if enabled and multiple downloads (not during preview)
            if (!isPreviewAnimating && PrefsManager.showDownloadCount && activeDownloadCount > 1 && effectiveProgress > 0) {
                val centerX = arcBounds.centerX()
                val centerY = arcBounds.centerY()
                canvas.drawText(
                    activeDownloadCount.toString(),
                    centerX,
                    centerY + countPaint.textSize / 3,
                    countPaint,
                )
            }

            // Draw percentage text if enabled and during active progress (1-99%)
            if (PrefsManager.percentTextEnabled && effectiveProgress in 1..99) {
                drawPercentText(canvas, effectiveProgress)
            }

            // Draw filename text if enabled and during active progress (1-99%)
            if (PrefsManager.filenameTextEnabled && effectiveProgress in 1..99 && currentFilename != null) {
                drawFilenameText(canvas)
            }
        }

        canvas.restore()

        if (drawCount == 1) {
            log("First draw: ring rendered (appVisible=$appVisible, progress=$progress)")
        }
    }

    private fun drawFinishAnimation(
        canvas: Canvas,
        paint: Paint,
    ) {
        scaledPath.computeBounds(arcBounds, true)

        val style = PrefsManager.finishStyle

        when (style) {
            "segmented" -> {
                drawSegmented(canvas, paint)
            }

            "shine_sweep" -> {
                drawShineSweep(canvas, paint)
            }

            else -> {
                // Default: draw full ring with current animation state
                canvas.drawPath(scaledPath, paint)
            }
        }
    }

    private fun drawSegmented(
        canvas: Canvas,
        paint: Paint,
    ) {
        for (i in 0 until segmentCount) {
            val startAngle = -90f + i * (segmentArcDegrees + segmentGapDegrees)

            val segmentPaint =
                if (i == segmentHighlight || i == segmentHighlight - 1) {
                    Paint(shinePaint).apply {
                        alpha = (255 * displayAlpha).toInt()
                    }
                } else {
                    Paint(paint)
                }

            canvas.drawArc(arcBounds, startAngle, segmentArcDegrees, false, segmentPaint)
        }
    }

    private fun drawShineSweep(
        canvas: Canvas,
        paint: Paint,
    ) {
        // Draw base ring
        canvas.drawPath(scaledPath, paint)

        // Draw shine highlight arc if in sweep phase
        if (shineAngle > -30f && shineAngle < 360f) {
            val shineArcPaint =
                Paint(shinePaint).apply {
                    alpha = (200 * displayAlpha).toInt()
                    strokeWidth = paint.strokeWidth * 1.3f
                }
            canvas.drawArc(arcBounds, shineAngle - 90f, 30f, false, shineArcPaint)
        }
    }

    private fun drawPercentText(
        canvas: Canvas,
        progress: Int,
    ) {
        percentPaint.color = PrefsManager.color
        percentPaint.alpha = (PrefsManager.opacity * 255 / 100)

        val text = "$progress%"
        val textWidth = percentPaint.measureText(text)
        val padding = 8f * density

        // Position text just left or right of ring bounds
        val offsetX =
            when (PrefsManager.percentTextPosition) {
                "left" -> arcBounds.left - textWidth / 2 - padding
                else -> arcBounds.right + textWidth / 2 + padding // "right"
            }
        val offsetY = arcBounds.centerY() + percentPaint.textSize / 3

        canvas.drawText(text, offsetX, offsetY, percentPaint)
    }

    private fun drawFilenameText(canvas: Canvas) {
        val filename = currentFilename ?: return

        filenamePaint.color = PrefsManager.color
        filenamePaint.alpha = (PrefsManager.opacity * 255 / 100)

        // Truncate filename with ellipsis if too long
        val truncated =
            TextUtils
                .ellipsize(
                    filename,
                    filenamePaint,
                    maxFilenameWidth,
                    TextUtils.TruncateAt.MIDDLE,
                ).toString()

        val padding = 8f * density

        // Calculate position based on preference
        val (offsetX, offsetY) =
            when (PrefsManager.filenameTextPosition) {
                "left" -> {
                    // Left of ring, vertically centered
                    filenamePaint.textAlign = Paint.Align.RIGHT
                    Pair(arcBounds.left - padding, arcBounds.centerY() + filenamePaint.textSize / 3)
                }

                "right" -> {
                    // Right of ring, vertically centered
                    filenamePaint.textAlign = Paint.Align.LEFT
                    Pair(arcBounds.right + padding, arcBounds.centerY() + filenamePaint.textSize / 3)
                }

                "top_left" -> {
                    // Above and left of ring
                    filenamePaint.textAlign = Paint.Align.RIGHT
                    Pair(arcBounds.left - padding, arcBounds.top - padding)
                }

                else -> {
                    // "top_right" - Above and right of ring
                    filenamePaint.textAlign = Paint.Align.LEFT
                    Pair(arcBounds.right + padding, arcBounds.top - padding)
                }
            }

        canvas.drawText(truncated, offsetX, offsetY, filenamePaint)
    }

    private fun blendColors(
        color1: Int,
        color2: Int,
        ratio: Float,
    ): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(Color.alpha(color1), r, g, b)
    }

    companion object {
        // Non-SDK window type that renders above nav bar without stealing focus
        // See: cs.android.com/android/platform/superproject/+/main:frameworks/base/core/java/android/view/WindowManager.java
        private const val TYPE_NAVIGATION_BAR_PANEL = 2024

        // Cap finish animations to prevent sluggish feel
        private const val MAX_ANIMATION_MS = 800

        fun attach(context: Context): IndicatorView {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = IndicatorView(context)

            // NOT_TOUCHABLE passes input through, NO_LIMITS extends into cutout area,
            // CUTOUT_MODE_ALWAYS ensures we can draw in the cutout region
            val params =
                WindowManager
                    .LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        TYPE_NAVIGATION_BAR_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT,
                    ).apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }

            wm.addView(view, params)
            return view
        }
    }
}
