package eu.hxreborn.phdp.xposed.indicator

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
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.graphics.withSave
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log
import eu.hxreborn.phdp.xposed.hook.SystemUIHooker
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
    private val badgePainter = BadgePainter(density)

    private val animator = IndicatorAnimator(this)

    private var downloadStartTime = 0L
    private var pendingFinishRunnable: Runnable? = null
    private var lastProgressChangeTime = 0L
    private val burnInHideRunnable = Runnable { invalidate() }
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

    @Volatile
    var isPowerSaveActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                post {
                    updatePaintFromPrefs()
                    invalidate()
                }
            }
        }

    @Volatile
    var progress: Int = 0
        set(value) {
            val newValue = value.coerceIn(0, 100)
            if (field != newValue) {
                val oldValue = field
                field = newValue
                lastProgressChangeTime = System.currentTimeMillis()
                removeCallbacks(burnInHideRunnable)
                if (newValue in 1..99) {
                    postDelayed(burnInHideRunnable, BURN_IN_HIDE_DELAY_MS)
                }
                log("IndicatorView: progress = $newValue")

                if (oldValue == 0 && newValue > 0) {
                    downloadStartTime = System.currentTimeMillis()
                    pendingFinishRunnable?.let { removeCallbacks(it) }
                    pendingFinishRunnable = null
                }

                when {
                    newValue == 100 && !animator.isFinishAnimating -> {
                        val elapsed = System.currentTimeMillis() - downloadStartTime
                        val remaining = minVisibilityMs - elapsed
                        if (remaining > 0 && downloadStartTime > 0) {
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
                    }

                    newValue in 1..99 && animator.isFinishAnimating -> {
                        animator.cancelFinish()
                    }

                    newValue == 0 -> {
                        downloadStartTime = 0L
                        pendingFinishRunnable?.let { removeCallbacks(it) }
                        pendingFinishRunnable = null
                    }
                }
                post { invalidate() }
            }
        }

    @Volatile
    var appVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                log("IndicatorView: appVisible = $value")
                post { invalidate() }
            }
        }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val animatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val backgroundRingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
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
    private val effectiveOpacity: Int
        get() =
            if (isPowerSaveActive && PrefsManager.powerSaverMode == "dim") {
                (PrefsManager.opacity * POWER_SAVER_DIM_FACTOR).toInt()
            } else {
                PrefsManager.opacity
            }
    private val strokeCap: Paint.Cap
        get() =
            when (PrefsManager.strokeCapStyle) {
                "round" -> Paint.Cap.ROUND
                "square" -> Paint.Cap.SQUARE
                else -> Paint.Cap.BUTT
            }

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
        glowPaint.apply {
            color = PrefsManager.color
            alpha = effectiveOpacity * 255 / 100
            strokeWidth = PrefsManager.strokeWidth * density
            this.strokeCap = strokeCap
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
            strokeWidth = PrefsManager.strokeWidth * density * SHINE_STROKE_MULTIPLIER
            this.strokeCap = strokeCap
        }

        errorPaint.apply {
            color = PrefsManager.errorColor
            strokeWidth = PrefsManager.strokeWidth * density * ERROR_STROKE_MULTIPLIER
            this.strokeCap = strokeCap
        }

        backgroundRingPaint.apply {
            color = PrefsManager.backgroundRingColor
            alpha = PrefsManager.backgroundRingOpacity * 255 / 100
            strokeWidth = PrefsManager.strokeWidth * density
            this.strokeCap = strokeCap
        }

        percentPaint.apply {
            textSize =
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    PrefsManager.percentTextSize,
                    resources.displayMetrics,
                )
            typeface =
                Typeface.defaultFromStyle(
                    typefaceStyle(PrefsManager.percentTextBold, PrefsManager.percentTextItalic),
                )
        }
        filenamePaint.apply {
            textSize =
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    PrefsManager.filenameTextSize,
                    resources.displayMetrics,
                )
            typeface =
                Typeface.defaultFromStyle(
                    typefaceStyle(PrefsManager.filenameTextBold, PrefsManager.filenameTextItalic),
                )
        }

        badgePainter.updateColors(PrefsManager.color, PrefsManager.badgeTextSize)

        log(
            "Paint updated: color=${Integer.toHexString(PrefsManager.color)}, " +
                "opacity=$effectiveOpacity, stroke=${PrefsManager.strokeWidth}, " +
                "gap=${PrefsManager.ringGap}, scaleX=${PrefsManager.ringScaleX}, " +
                "scaleY=${PrefsManager.ringScaleY}",
        )
        invalidate()
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
        animator.cancelAll()
        pendingFinishRunnable?.let { removeCallbacks(it) }
        pendingFinishRunnable = null
        removeCallbacks(burnInHideRunnable)
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
            log(
                "Cutout path bounds: ${pathBounds.width()}x${pathBounds.height()} at (${pathBounds.centerX()}, ${pathBounds.centerY()})",
            )
            recalculateScaledPath()
        }

        invalidate()
        return super.onApplyWindowInsets(insets)
    }

    private fun startFinishAnimation() {
        animator.startFinish(
            style = PrefsManager.finishStyle,
            holdMs = PrefsManager.finishHoldMs,
            exitMs = PrefsManager.finishExitMs,
            pulseEnabled = PrefsManager.completionPulseEnabled,
        ) { progress = 0 }
    }

    fun startDynamicPreviewAnim() {
        animator.startDynamicPreviewAnim(
            finishStyle = PrefsManager.finishStyle,
            holdMs = PrefsManager.finishHoldMs,
            exitMs = PrefsManager.finishExitMs,
            pulseEnabled = PrefsManager.completionPulseEnabled,
        )
    }

    fun showStaticPreviewAnim(autoHide: Boolean = true) = animator.showStaticPreviewAnim(autoHide)

    fun cancelStaticPreviewAnim() = animator.cancelStaticPreviewAnim()

    fun showError() = animator.startError { progress = 0 }

    override fun onDraw(canvas: Canvas) {
        drawCount++
        if (cutoutPath == null) {
            if (drawCount == 1) log("First draw: NO cutoutPath, nothing to draw")
            return
        }

        if (!PrefsManager.enabled) return
        if (isPowerSaveActive && PrefsManager.powerSaverMode == "disable") return

        if (animator.isErrorAnimating) {
            computeCalibratedArcBounds()
            errorPaint.alpha = (animator.errorAlpha * 255).toInt()
            canvas.drawArc(arcBounds, 0f, 360f, false, errorPaint)
            return
        }

        val effectiveProgress =
            when {
                animator.isGeometryPreviewActive -> 100
                animator.isPreviewAnimating -> animator.previewProgress
                else -> progress
            }

        val isBurnInHidden =
            effectiveProgress in 1..99 &&
                lastProgressChangeTime > 0 &&
                System.currentTimeMillis() - lastProgressChangeTime >= BURN_IN_HIDE_DELAY_MS

        val shouldDraw =
            when {
                animator.isFinishAnimating -> true
                animator.isGeometryPreviewActive -> true
                animator.isPreviewAnimating -> true
                effectiveProgress in 1..99 -> !isBurnInHidden
                pendingFinishRunnable != null -> true
                else -> false
            }
        if (!shouldDraw) {
            if (drawCount == 1) log("IndicatorView: not drawing (disabled or no visibility)")
            return
        }

        canvas.withSave {
            if (animator.displayScale != 1f) {
                scaledPath.computeBounds(arcBounds, true)
                scale(
                    animator.displayScale,
                    animator.displayScale,
                    arcBounds.centerX(),
                    arcBounds.centerY(),
                )
            }

            animatedPaint.set(glowPaint)
            animatedPaint.alpha =
                (
                    effectiveOpacity * 255 / 100 * animator.displayAlpha *
                        animator.completionPulseAlpha
                ).toInt()
            animatedPaint.strokeCap = strokeCap
            if (animator.successColorBlend > 0f) {
                val successColor =
                    if (PrefsManager.finishUseFlashColor) {
                        PrefsManager.finishFlashColor
                    } else {
                        brightenColor(PrefsManager.color, animator.successColorBlend)
                    }
                animatedPaint.color =
                    blendColors(PrefsManager.color, successColor, animator.successColorBlend)
            }

            val isActiveProgress =
                effectiveProgress in 1..99 ||
                    animator.isGeometryPreviewActive ||
                    animator.isPreviewAnimating
            val showBackgroundRing =
                PrefsManager.backgroundRingEnabled &&
                    !animator.isFinishAnimating &&
                    !animator.isErrorAnimating &&
                    isActiveProgress

            if (showBackgroundRing) {
                scaledPath.computeBounds(arcBounds, true)
                arcBounds.applyCalibration()
                val bgOpacityBase =
                    if (isPowerSaveActive && PrefsManager.powerSaverMode == "dim") {
                        (PrefsManager.backgroundRingOpacity * POWER_SAVER_DIM_FACTOR).toInt()
                    } else {
                        PrefsManager.backgroundRingOpacity
                    }
                backgroundRingPaint.alpha =
                    (bgOpacityBase * 255 / 100 * animator.displayAlpha).toInt()
                drawArc(arcBounds, 0f, 360f, false, backgroundRingPaint)
            }

            if (animator.isFinishAnimating) {
                drawFinishAnimation(this, animatedPaint)
            } else {
                scaledPath.computeBounds(arcBounds, true)
                arcBounds.applyCalibration()
                val sweepAngle = 360f * applyEasing(effectiveProgress, PrefsManager.progressEasing)
                val actualSweep = if (PrefsManager.clockwise) sweepAngle else -sweepAngle
                drawArc(arcBounds, -90f, actualSweep, false, animatedPaint)

                val showLabels =
                    effectiveProgress in 1..99 || animator.isGeometryPreviewActive
                if (showLabels) drawLabels(this, effectiveProgress)
            }

            // Badge drawn BELOW the ring (not at center - that's behind camera hardware)
            val showBadge =
                !animator.isPreviewAnimating && PrefsManager.showDownloadCount &&
                    (activeDownloadCount > 1 || animator.isGeometryPreviewActive)
            if (showBadge) {
                scaledPath.computeBounds(arcBounds, true)
                val viewDensity = this@IndicatorView.density
                val badgeCenterX =
                    arcBounds.centerX() + PrefsManager.badgeOffsetX * viewDensity
                val badgeTop =
                    arcBounds.bottom + BADGE_TOP_PADDING_DP * viewDensity +
                        PrefsManager.badgeOffsetY * viewDensity
                val badgeCount =
                    if (animator.isGeometryPreviewActive) 3 else activeDownloadCount
                badgePainter.draw(
                    this,
                    badgeCenterX,
                    badgeTop,
                    badgeCount,
                    effectiveOpacity,
                )
            }
        }

        if (drawCount == 1) {
            log("First draw: ring rendered (appVisible=$appVisible, progress=$progress)")
        }
    }

    private fun drawFinishAnimation(
        canvas: Canvas,
        paint: Paint,
    ) {
        scaledPath.computeBounds(arcBounds, true)
        arcBounds.applyCalibration()
        if (PrefsManager.finishStyle == "segmented") {
            drawSegmented(canvas, paint)
        } else {
            canvas.drawArc(arcBounds, 0f, 360f, false, paint)
        }
    }

    private fun drawSegmented(
        canvas: Canvas,
        paint: Paint,
    ) {
        for (i in 0 until IndicatorAnimator.SEGMENT_COUNT) {
            val startAngle =
                -90f +
                    i *
                    (
                        IndicatorAnimator.SEGMENT_ARC_DEGREES +
                            IndicatorAnimator.SEGMENT_GAP_DEGREES
                    )
            val segmentPaint =
                if (i == animator.segmentHighlight || i == animator.segmentHighlight - 1) {
                    Paint(shinePaint).apply { alpha = (255 * animator.displayAlpha).toInt() }
                } else {
                    Paint(paint)
                }
            canvas.drawArc(
                arcBounds,
                startAngle,
                IndicatorAnimator.SEGMENT_ARC_DEGREES,
                false,
                segmentPaint,
            )
        }
    }

    private data class TextSpec(
        val text: String,
        val paint: Paint,
        val x: Float,
        val y: Float,
        val align: Paint.Align? = null,
    )

    private fun drawLabels(
        canvas: Canvas,
        progressVal: Int,
    ) {
        val alpha = effectiveOpacity * 255 / 100
        val padding = LABEL_PADDING_DP * density
        val specs = mutableListOf<TextSpec>()

        if (PrefsManager.percentTextEnabled) {
            val text = "$progressVal%"
            val textWidth = percentPaint.measureText(text)
            val (baseX, baseY, align) =
                computeLabelPosition(
                    PrefsManager.percentTextPosition,
                    padding,
                    percentPaint.textSize,
                    textWidth,
                )
            val x = baseX + PrefsManager.percentTextOffsetX * density
            val y = baseY + PrefsManager.percentTextOffsetY * density
            specs += TextSpec(text, percentPaint, x, y, align)
        }

        val isGeometryPreview = animator.isGeometryPreviewActive
        val filenameToShow =
            currentFilename
                ?: if (isGeometryPreview) PrefsManager.previewFilenameText else null

        if (PrefsManager.filenameTextEnabled && filenameToShow != null &&
            (activeDownloadCount <= 1 || isGeometryPreview)
        ) {
            val truncated =
                if (PrefsManager.filenameTruncateEnabled) {
                    truncateWithEllipsis(
                        filenameToShow,
                        PrefsManager.filenameMaxChars,
                        PrefsManager.filenameEllipsize,
                    )
                } else {
                    filenameToShow
                }
            val (baseX, baseY, align) =
                computeLabelPosition(
                    PrefsManager.filenameTextPosition,
                    padding,
                    filenamePaint.textSize,
                    textWidth = null,
                )
            val x = baseX + PrefsManager.filenameTextOffsetX * density
            val y = baseY + PrefsManager.filenameTextOffsetY * density
            specs += TextSpec(truncated, filenamePaint, x, y, align)
        }

        for (spec in specs) {
            spec.paint.color = PrefsManager.color
            spec.paint.alpha = alpha
            spec.align?.let { (spec.paint as? TextPaint)?.textAlign = it }
            canvas.drawText(spec.text, spec.x, spec.y, spec.paint)
        }
    }

    private fun computeLabelPosition(
        position: String,
        padding: Float,
        textSize: Float,
        textWidth: Float?,
    ): Triple<Float, Float, Paint.Align?> =
        when (position) {
            "left" -> {
                Triple(
                    textWidth?.let { arcBounds.left - it / 2 - padding }
                        ?: (arcBounds.left - padding),
                    arcBounds.centerY() + textSize / 3,
                    textWidth?.let { null } ?: Paint.Align.RIGHT,
                )
            }

            "right" -> {
                Triple(
                    textWidth?.let {
                        arcBounds.right + it / 2 + padding
                    } ?: (arcBounds.right + padding),
                    arcBounds.centerY() + textSize / 3,
                    textWidth?.let { null } ?: Paint.Align.LEFT,
                )
            }

            "top_left" -> {
                Triple(
                    arcBounds.left - padding,
                    arcBounds.top - padding,
                    Paint.Align.RIGHT,
                )
            }

            "top_right" -> {
                Triple(
                    arcBounds.right + padding,
                    arcBounds.top - padding,
                    Paint.Align.LEFT,
                )
            }

            "bottom_left" -> {
                Triple(
                    arcBounds.left - padding,
                    arcBounds.bottom + textSize + padding,
                    Paint.Align.RIGHT,
                )
            }

            "bottom_right" -> {
                Triple(
                    arcBounds.right + padding,
                    arcBounds.bottom + textSize + padding,
                    Paint.Align.LEFT,
                )
            }

            "top" -> {
                Triple(
                    arcBounds.centerX(),
                    arcBounds.top - padding,
                    Paint.Align.CENTER,
                )
            }

            "bottom" -> {
                Triple(
                    arcBounds.centerX(),
                    arcBounds.bottom + textSize + padding,
                    Paint.Align.CENTER,
                )
            }

            else -> {
                Triple(
                    textWidth?.let {
                        arcBounds.right + it / 2 + padding
                    } ?: (arcBounds.right + padding),
                    textWidth?.let { arcBounds.centerY() + textSize / 3 }
                        ?: (arcBounds.top - padding),
                    textWidth?.let { null } ?: Paint.Align.LEFT,
                )
            }
        }

    private fun brightenColor(
        color: Int,
        factor: Float,
    ): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    private fun blendColors(
        color1: Int,
        color2: Int,
        ratio: Float,
    ): Int {
        val inv = 1f - ratio
        val r = (Color.red(color1) * inv + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inv + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inv + Color.blue(color2) * ratio).toInt()
        return Color.argb(Color.alpha(color1), r, g, b)
    }

    private fun applyEasing(
        progressVal: Int,
        easingType: String,
    ): Float {
        val p = progressVal / 100f
        return when (easingType) {
            "accelerate" -> p * p
            "decelerate" -> 1f - (1f - p) * (1f - p)
            "ease_in_out" -> if (p < 0.5f) 2f * p * p else 1f - (-2f * p + 2f).pow(2) / 2f
            else -> p
        }
    }

    private fun computeCalibratedArcBounds() {
        scaledPath.computeBounds(arcBounds, true)
        arcBounds.applyCalibration()
    }

    // Apply calibration: normalize to square, offset for alignment, scale for customization
    private fun RectF.applyCalibration() {
        val offsetX = PrefsManager.ringOffsetX
        val offsetY = PrefsManager.ringOffsetY
        val scaleX = PrefsManager.ringScaleX
        val scaleY = PrefsManager.ringScaleY

        val maxDimension = maxOf(width(), height())
        val halfBaseSize = maxDimension / 2f

        val centerX = centerX() + offsetX
        val centerY = centerY() + offsetY

        val halfWidth = halfBaseSize * scaleX
        val halfHeight = halfBaseSize * scaleY

        set(
            centerX - halfWidth,
            centerY - halfHeight,
            centerX + halfWidth,
            centerY + halfHeight,
        )
    }

    // Char-based truncation where ellipsis counts toward maxChars
    private fun truncateWithEllipsis(
        text: String,
        maxChars: Int,
        mode: String,
    ): String {
        if (text.length <= maxChars) return text
        val ellipsis = "\u2026"
        val available = maxChars - 1
        if (available <= 0) return ellipsis
        return when (mode) {
            "start" -> {
                ellipsis + text.takeLast(available)
            }

            "end" -> {
                text.take(available) + ellipsis
            }

            else -> {
                val head = (available + 1) / 2
                val tail = available - head
                text.take(head) + ellipsis + text.takeLast(tail)
            }
        }
    }

    private fun typefaceStyle(
        bold: Boolean,
        italic: Boolean,
    ): Int =
        when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

    companion object {
        private const val TYPE_NAVIGATION_BAR_PANEL = 2024

        // Drawing constants
        private const val POWER_SAVER_DIM_FACTOR = 0.5f
        private const val SHINE_STROKE_MULTIPLIER = 1.2f
        private const val ERROR_STROKE_MULTIPLIER = 1.5f
        private const val BADGE_TOP_PADDING_DP = 4f
        private const val LABEL_PADDING_DP = 4f
        private const val BURN_IN_HIDE_DELAY_MS = 10_000L

        fun attach(context: Context): IndicatorView {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = IndicatorView(context)

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
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }

            wm.addView(view, params)
            return view
        }
    }
}
