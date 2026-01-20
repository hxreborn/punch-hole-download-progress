package eu.hxreborn.phdp.xposed

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
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log
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

    // Debounce preview to avoid restart spam during slider drag
    private val previewDebounceMs = 300L

    // Geometry preview state
    private var geometryPreviewRunnable: Runnable? = null
    private var isGeometryPreviewActive = false

    // Hold preview long enough to judge geometry changes
    private val geometryPreviewDurationMs = 3000L

    // Keep ring visible long enough to notice short downloads
    private var downloadStartTime = 0L
    private var pendingFinishRunnable: Runnable? = null
    private val minVisibilityMs: Long
        get() = if (PrefsManager.minVisibilityEnabled) PrefsManager.minVisibilityMs.toLong() else 0L

    @Volatile
    var activeDownloadCount: Int = 0
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    @Volatile
    var currentFilename: String? = null
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    // Power saver state set by SystemUIHooker
    @Volatile
    var isPowerSaveActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                post { invalidate() }
            }
        }

    // Segmented style 12 segments with 6 deg gaps gives 24 deg arc per segment
    private val segmentCount = 12
    private val segmentGapDegrees = 6f
    private val segmentArcDegrees = (360f - segmentCount * segmentGapDegrees) / segmentCount

    // Progress 0 hides ring 1 to 99 draws arc 100 triggers finish animation
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
                    downloadStartTime = 0L
                    pendingFinishRunnable?.let { removeCallbacks(it) }
                    pendingFinishRunnable = null
                }
                post { invalidate() }
            }
        }

    // App visible flag shows ring in preview mode
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

    // Max width for filename text from screen width
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
        SystemUIHooker.detach()
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
        val intensity = 1.5f

        log("Starting finish animation: style=$style, hold=${holdMs}ms, exit=${exitMs}ms, intensity=$intensity")

        displayAlpha = 1f
        displayScale = 1f
        shineAngle = -30f
        segmentHighlight = -1
        successColorBlend = 1f // Show success color immediately without overlap with progress color
        completionPulseAlpha = 1f

        val startStyleAnimation = {
            when (style) {
                "snap" -> {
                    progress = 0
                    isFinishAnimating = false
                }

                "pop" -> {
                    animatePop(holdMs, exitMs, intensity)
                }

                "segmented" -> {
                    animateSegmented(holdMs, exitMs, intensity)
                }

                else -> {
                    animatePop(holdMs, exitMs, intensity)
                }
            }
        }

        if (PrefsManager.completionPulseEnabled && style != "snap") {
            animateCompletionPulse { startStyleAnimation() }
        } else {
            startStyleAnimation()
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

    private fun animateSegmented(
        holdMs: Int,
        exitMs: Int,
        intensity: Float,
    ) {
        val totalMs = (holdMs + exitMs).coerceAtMost(MAX_ANIMATION_MS)
        val cascadePhaseMs = (totalMs * 0.6f).toInt()
        val fadePhaseMs = totalMs - cascadePhaseMs

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

    // Pulse alpha 100 to 70 to 100 in 400ms for subtle completion feedback
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

    private fun applyEasing(progress: Int): Float {
        val p = progress / 100f
        return when (PrefsManager.progressEasing) {
            "accelerate" -> p * p
            "decelerate" -> 1f - (1f - p) * (1f - p)
            "ease_in_out" -> if (p < 0.5f) 2f * p * p else 1f - (-2f * p + 2f).pow(2) / 2f
            else -> p // linear
        }
    }

    // Start preview animation debounced from 0 to 100 then finish animation
    fun startPreview() {
        log("IndicatorView: startPreview() - debouncing")

        previewDebounceRunnable?.let { removeCallbacks(it) }

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
                            previewProgress = 100
                            startFinishAnimation()
                            // Clear preview state after finish animation about 800ms plus buffer
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

    // Show geometry preview at 100 percent for 3 seconds debounced
    fun showGeometryPreview() {
        log("IndicatorView: showGeometryPreview() - debouncing")

        geometryPreviewRunnable?.let { removeCallbacks(it) }

        isGeometryPreviewActive = true
        invalidate()

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

    fun showError() {
        if (isFinishAnimating || isErrorAnimating) return
        isErrorAnimating = true
        log("IndicatorView: showError()")

        // Two blinks in 600ms with 5 keyframes at 120ms each
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

        if (isPowerSaveActive && PrefsManager.powerSaverMode == "disable") {
            return
        }

        if (PrefsManager.idleRingEnabled && progress == 0 && !isFinishAnimating && !isErrorAnimating && !appVisible) {
            canvas.drawPath(scaledPath, idlePaint)
            return
        }

        if (isErrorAnimating) {
            scaledPath.computeBounds(arcBounds, true)
            errorPaint.alpha = (errorAlpha * 255).toInt()
            canvas.drawPath(scaledPath, errorPaint)
            return
        }

        // Determine effective progress with preview override
        val effectiveProgress =
            when {
                isGeometryPreviewActive -> 100
                isPreviewAnimating -> previewProgress
                else -> progress
            }

        // Visibility rules keep ring during animation preview active progress or pending finish
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

        canvas.save()

        if (displayScale != 1f) {
            scaledPath.computeBounds(arcBounds, true)
            canvas.scale(displayScale, displayScale, arcBounds.centerX(), arcBounds.centerY())
        }

        val animatedPaint =
            Paint(glowPaint).apply {
                alpha = (glowPaint.alpha * displayAlpha * completionPulseAlpha).toInt()
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
            drawFinishAnimation(canvas, animatedPaint)
        } else {
            scaledPath.computeBounds(arcBounds, true)
            val easedProgress = applyEasing(effectiveProgress)
            val sweepAngle = 360f * easedProgress
            val actualSweep = if (PrefsManager.clockwise) sweepAngle else -sweepAngle
            canvas.drawArc(arcBounds, -90f, actualSweep, false, animatedPaint)

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

            if (PrefsManager.percentTextEnabled && effectiveProgress in 1..99) {
                drawPercentText(canvas, effectiveProgress)
            }

            if (PrefsManager.filenameTextEnabled && effectiveProgress in 1..99 && (activeDownloadCount > 1 || currentFilename != null)) {
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

            else -> {
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

    private fun drawPercentText(
        canvas: Canvas,
        progress: Int,
    ) {
        percentPaint.color = PrefsManager.color
        percentPaint.alpha = (PrefsManager.opacity * 255 / 100)

        val text = "$progress%"
        val textWidth = percentPaint.measureText(text)
        val padding = 8f * density

        val offsetX =
            when (PrefsManager.percentTextPosition) {
                "left" -> arcBounds.left - textWidth / 2 - padding
                else -> arcBounds.right + textWidth / 2 + padding
            }
        val offsetY = arcBounds.centerY() + percentPaint.textSize / 3

        canvas.drawText(text, offsetX, offsetY, percentPaint)
    }

    private fun drawFilenameText(canvas: Canvas) {
        val text =
            if (activeDownloadCount > 1) {
                "Downloading $activeDownloadCount files"
            } else {
                currentFilename ?: return
            }

        filenamePaint.color = PrefsManager.color
        filenamePaint.alpha = (PrefsManager.opacity * 255 / 100)

        val truncated =
            TextUtils
                .ellipsize(
                    text,
                    filenamePaint,
                    maxFilenameWidth,
                    TextUtils.TruncateAt.MIDDLE,
                ).toString()

        val padding = 8f * density

        val (offsetX, offsetY) =
            when (PrefsManager.filenameTextPosition) {
                "left" -> {
                    filenamePaint.textAlign = Paint.Align.RIGHT
                    Pair(arcBounds.left - padding, arcBounds.centerY() + filenamePaint.textSize / 3)
                }

                "right" -> {
                    filenamePaint.textAlign = Paint.Align.LEFT
                    Pair(arcBounds.right + padding, arcBounds.centerY() + filenamePaint.textSize / 3)
                }

                "top_left" -> {
                    filenamePaint.textAlign = Paint.Align.RIGHT
                    Pair(arcBounds.left - padding, arcBounds.top - padding)
                }

                else -> {
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
        // Non SDK window type renders above nav bar without stealing focus
        // See cs.android.com/android/platform/superproject/+/main:frameworks/base/core/java/android/view/WindowManager.java
        private const val TYPE_NAVIGATION_BAR_PANEL = 2024

        // Cap finish animations to prevent sluggish feel
        private const val MAX_ANIMATION_MS = 800

        fun attach(context: Context): IndicatorView {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = IndicatorView(context)

            // NOT_TOUCHABLE passes input through NO_LIMITS extends into cutout area CUTOUT_MODE_ALWAYS enables cutout drawing
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
