package eu.hxreborn.phdp.xposed.indicator

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.view.DisplayCutout
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.graphics.withSave
import eu.hxreborn.phdp.prefs.RotationSlot
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.util.logDebug
import eu.hxreborn.phdp.xposed.hook.IndicatorState
import eu.hxreborn.phdp.xposed.hook.SystemUIHook
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
    private val iconPainter = IconPainter(context, density)

    // ArcRingRenderer (default) for circles, PathRingRenderer for pills. Toggled via "Path mode" pref.
    private var renderer: RingRenderer = ArcRingRenderer()

    private val animator = IndicatorAnimator(this)

    private var downloadStartTime = 0L
    private var pendingFinishRunnable: Runnable? = null
    private var lastActivityTime = 0L
    internal var windowParams: WindowManager.LayoutParams? = null
    private val burnInHideRunnable = Runnable { invalidate() }
    private var smoothProgress: Float = 0f
    private var progressAnim: ValueAnimator? = null

    fun touchActivity() {
        lastActivityTime = System.currentTimeMillis()
        removeCallbacks(burnInHideRunnable)
        val hideDelay = IndicatorState.burnInHideMs.toLong()
        if (progress in 1..99 && hideDelay > 0L) {
            postDelayed(burnInHideRunnable, hideDelay)
        }
    }

    // Easing-pref drives previews directly; their internal animations already smooth the sweep
    private fun smoothProgressFor(effectiveProgress: Int): Float =
        if (animator.isGeometryPreviewActive || animator.isPreviewAnimating) {
            applyEasing(effectiveProgress, IndicatorState.progressEasing)
        } else {
            smoothProgress
        }

    private fun animateProgressTo(target: Int) {
        val targetFraction = applyEasing(target, IndicatorState.progressEasing)
        progressAnim?.cancel()
        val durationMs = IndicatorState.progressAnimMs.toLong()
        if (durationMs <= 0L) {
            smoothProgress = targetFraction
            invalidate()
            return
        }
        progressAnim =
            ValueAnimator.ofFloat(smoothProgress, targetFraction).apply {
                duration = durationMs
                addUpdateListener {
                    smoothProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
    }

    private val minVisibilityMs: Long
        get() =
            if (IndicatorState.minVisibilityEnabled) IndicatorState.minVisibilityMs.toLong() else 0L

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
    var currentPackageName: String? = null
        set(value) {
            if (field != value) {
                field = value
                iconPainter.invalidateCache()
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
                touchActivity()
                animateProgressTo(newValue)
                logDebug { "IndicatorView: progress = $newValue" }

                if (oldValue == 0 && newValue > 0) {
                    downloadStartTime = System.currentTimeMillis()
                    pendingFinishRunnable?.let { removeCallbacks(it) }
                    pendingFinishRunnable = null
                }

                when (newValue) {
                    100 -> {
                        if (!animator.isFinishAnimating) {
                            val elapsed = System.currentTimeMillis() - downloadStartTime
                            val remaining = minVisibilityMs - elapsed
                            if (remaining > 0 && downloadStartTime > 0) {
                                logDebug {
                                    "IndicatorView: fast download, delaying finish by ${remaining}ms"
                                }
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
                    }

                    in 1..99 -> {
                        if (animator.isFinishAnimating) {
                            animator.cancelFinish()
                        }
                    }

                    0 -> {
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
                logDebug { "IndicatorView: appVisible = $value" }
                post { invalidate() }
            }
        }

    private var resolvedRingColor = IndicatorState.color

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val animatedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val backgroundRingPaint =
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
    private val percentStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
        }
    private val filenameStrokePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            textAlign = Paint.Align.LEFT
        }

    private class HaloPaint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var lastBlurRadius: Float = -1f
    }

    private val percentHalo = HaloPaint()
    private val filenameHalo = HaloPaint()
    private val textBoundsScratch = Rect()
    private val effectiveOpacity: Int
        get() =
            if (isPowerSaveActive && IndicatorState.powerSaverMode == "dim") {
                (IndicatorState.opacity * POWER_SAVER_DIM_FACTOR).toInt()
            } else {
                IndicatorState.opacity
            }
    private val strokeCap: Paint.Cap
        get() =
            when (IndicatorState.strokeCapStyle) {
                "round" -> Paint.Cap.ROUND
                "square" -> Paint.Cap.SQUARE
                else -> Paint.Cap.BUTT
            }

    init {
        log("IndicatorView: constructor called")
        updatePaintFromPrefs()
        updateRenderer()

        IndicatorState.onPrefsChanged = {
            post {
                logDebug { "IndicatorView: prefs changed, updating..." }
                updatePaintFromPrefs()
                updateRenderer()
                recalculateScaledPath()
                smoothProgress = applyEasing(progress, IndicatorState.progressEasing)
                invalidate()
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveSystemAccent(
        palette: String,
        shade: Int,
    ): Int? {
        val resId = context.resources.getIdentifier("system_${palette}_$shade", "color", "android")
        return if (resId != 0) context.getColor(resId) else null
    }

    @RequiresApi(35)
    private fun applyHdrColor(
        paint: Paint,
        baseColor: Int,
        headroom: Float,
    ) {
        val a = Color.alpha(baseColor) / 255f
        val r = Color.red(baseColor) / 255f * headroom
        val g = Color.green(baseColor) / 255f * headroom
        val b = Color.blue(baseColor) / 255f * headroom
        paint.setColor(Color.pack(r, g, b, a, ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)))
    }

    private fun applyRingColor(
        paint: Paint,
        baseColor: Int,
    ) {
        if (Build.VERSION.SDK_INT >= 35 && IndicatorState.hdrEnabled) {
            applyHdrColor(paint, baseColor, IndicatorState.hdrHeadroom)
        } else {
            paint.color = baseColor
        }
    }

    private fun composeShadowColor(
        baseColor: Int,
        opacityPercent: Int,
    ): Int {
        val rgb = baseColor and 0x00FFFFFF
        val alphaByte = (opacityPercent * 255 / 100).coerceIn(0, 255)
        return rgb or (alphaByte shl 24)
    }

    private fun updatePaintFromPrefs() {
        configureRingFamily()
        applyTextShadow(
            fillPaint = percentPaint,
            strokePaint = percentStrokePaint,
            halo = percentHalo,
            textSizeSp = IndicatorState.percentTextSize,
            bold = IndicatorState.percentTextBold,
            italic = IndicatorState.percentTextItalic,
            shadowMode = IndicatorState.percentTextShadowMode,
            shadowColor = IndicatorState.percentTextShadowColor,
            shadowOpacity = IndicatorState.percentTextShadowOpacity,
            shadowRadius = IndicatorState.percentTextShadowRadius,
            shadowDy = IndicatorState.percentTextShadowDy,
            strokeWidthDp = IndicatorState.percentTextStrokeWidth,
            strokeColor = IndicatorState.percentTextStrokeColor,
        )
        applyTextShadow(
            fillPaint = filenamePaint,
            strokePaint = filenameStrokePaint,
            halo = filenameHalo,
            textSizeSp = IndicatorState.filenameTextSize,
            bold = IndicatorState.filenameTextBold,
            italic = IndicatorState.filenameTextItalic,
            shadowMode = IndicatorState.filenameTextShadowMode,
            shadowColor = IndicatorState.filenameTextShadowColor,
            shadowOpacity = IndicatorState.filenameTextShadowOpacity,
            shadowRadius = IndicatorState.filenameTextShadowRadius,
            shadowDy = IndicatorState.filenameTextShadowDy,
            strokeWidthDp = IndicatorState.filenameTextStrokeWidth,
            strokeColor = IndicatorState.filenameTextStrokeColor,
        )

        badgePainter.updateColors(resolvedRingColor, IndicatorState.badgeTextSize)
        iconPainter.updateColors(resolvedRingColor, effectiveOpacity)

        logDebug {
            "Paint updated: color=${Integer.toHexString(resolvedRingColor)}, " +
                "opacity=$effectiveOpacity, stroke=${IndicatorState.strokeWidth}, " +
                "gap=${IndicatorState.ringGap}, scaleX=${IndicatorState.ringScaleX}, " +
                "scaleY=${IndicatorState.ringScaleY}"
        }
        invalidate()
    }

    private fun configureRingFamily() {
        resolvedRingColor = IndicatorState.color
        val ringStrokeWidth = IndicatorState.strokeWidth * density
        val glowRadiusPx =
            if (IndicatorState.glowEnabled) IndicatorState.glowRadius * density else 0f

        glowPaint.apply {
            applyRingColor(this, resolvedRingColor)
            alpha = effectiveOpacity * 255 / 100
            strokeWidth = ringStrokeWidth
            this.strokeCap = strokeCap
            setShadowLayer(glowRadiusPx, 0f, 0f, resolvedRingColor)
        }

        shinePaint.apply {
            color =
                if (IndicatorState.finishUseFlashColor) {
                    IndicatorState.finishFlashColor
                } else {
                    brightenColor(IndicatorState.color, 0.5f)
                }
            alpha = 255
            strokeWidth = ringStrokeWidth * SHINE_STROKE_MULTIPLIER
            this.strokeCap = strokeCap
        }

        errorPaint.apply {
            color = IndicatorState.errorColor
            strokeWidth = ringStrokeWidth * ERROR_STROKE_MULTIPLIER
            this.strokeCap = strokeCap
        }

        if (IndicatorState.materialYouEnabled && Build.VERSION.SDK_INT >= 31) {
            resolveSystemAccent(
                IndicatorState.materialYouProgressPalette,
                IndicatorState.materialYouProgressShade,
            )?.let { c ->
                resolvedRingColor = c
                applyRingColor(glowPaint, resolvedRingColor)
                glowPaint.alpha = effectiveOpacity * 255 / 100
                glowPaint.setShadowLayer(glowRadiusPx, 0f, 0f, resolvedRingColor)
            }
            resolveSystemAccent(
                IndicatorState.materialYouSuccessPalette,
                IndicatorState.materialYouSuccessShade,
            )?.let { c -> shinePaint.color = c }
            resolveSystemAccent(
                IndicatorState.materialYouErrorPalette,
                IndicatorState.materialYouErrorShade,
            )?.let { c -> errorPaint.color = c }
        }

        backgroundRingPaint.apply {
            applyRingColor(this, IndicatorState.backgroundRingColor)
            alpha = IndicatorState.backgroundRingOpacity * 255 / 100
            strokeWidth = ringStrokeWidth
            this.strokeCap = strokeCap
        }
    }

    private fun applyTextShadow(
        fillPaint: Paint,
        strokePaint: Paint,
        halo: HaloPaint,
        textSizeSp: Float,
        bold: Boolean,
        italic: Boolean,
        shadowMode: String,
        shadowColor: Int,
        shadowOpacity: Int,
        shadowRadius: Float,
        shadowDy: Float,
        strokeWidthDp: Float,
        strokeColor: Int,
    ) {
        val composedColor = composeShadowColor(shadowColor, shadowOpacity)
        val radiusPx = shadowRadius * density
        val dyPx = shadowDy * density
        val isOval = shadowMode == "oval"

        fillPaint.apply {
            textSize =
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    textSizeSp,
                    resources.displayMetrics,
                )
            typeface = Typeface.defaultFromStyle(typefaceStyle(bold, italic))
            if (isOval) {
                clearShadowLayer()
            } else {
                setShadowLayer(radiusPx, 0f, dyPx, composedColor)
            }
        }

        strokePaint.apply {
            textSize = fillPaint.textSize
            typeface = fillPaint.typeface
            strokeWidth = strokeWidthDp * density
            color = strokeColor
        }

        if (isOval) {
            halo.paint.color = composedColor
            if (halo.lastBlurRadius != radiusPx) {
                halo.paint.maskFilter =
                    if (radiusPx >
                        0f
                    ) {
                        BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
                    } else {
                        null
                    }
                halo.lastBlurRadius = radiusPx
            }
        }
    }

    private fun updateRenderer() {
        renderer = if (IndicatorState.pathMode) PathRingRenderer() else ArcRingRenderer()
    }

    private fun recalculateScaledPath() {
        cutoutPath?.let { path ->
            path.computeBounds(pathBounds, true)
            scaleMatrix.setScale(
                IndicatorState.ringGap,
                IndicatorState.ringGap,
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
        IndicatorState.onHdrConfigChanged = { applyHdrToOverlayWindow() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        log("IndicatorView: onDetachedFromWindow()")
        animator.cancelAll()
        progressAnim?.cancel()
        progressAnim = null
        pendingFinishRunnable?.let { removeCallbacks(it) }
        pendingFinishRunnable = null
        removeCallbacks(burnInHideRunnable)
        iconPainter.recycle()
        IndicatorState.onPrefsChanged = null
        IndicatorState.onTestProgressChanged = null
        IndicatorState.onTestErrorChanged = null
        IndicatorState.onPreviewTriggered = null
        IndicatorState.onGeometryPreviewTriggered = null
        IndicatorState.onHdrConfigChanged = null
        SystemUIHook.detach()
    }

    private fun applyHdrToOverlayWindow() {
        if (Build.VERSION.SDK_INT < 35) return
        post {
            val params = windowParams ?: return@post
            if (IndicatorState.hdrEnabled) {
                params.colorMode = ActivityInfo.COLOR_MODE_HDR
                params.desiredHdrHeadroom = IndicatorState.hdrHeadroom
            } else {
                params.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                params.desiredHdrHeadroom = 0f
            }
            runCatching {
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .updateViewLayout(this, params)
            }.onFailure { log("hdr refresh failed", it) }
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val displayCutout = insets.displayCutout
        cutoutPath =
            // cutoutPath is API 31+, falls through to buildFallbackPath on older
            @Suppress("NewApi")
            displayCutout?.cutoutPath?.also { log("Cutout source: native") }
                ?: buildFallbackPath(displayCutout) ?: buildMockCutoutPath()

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

    // Fallback for API < 31. Manually build path since API doesn't provide it
    private fun buildFallbackPath(displayCutout: DisplayCutout?): Path? {
        val rect = displayCutout?.boundingRects?.firstOrNull() ?: return null
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val radius = minOf(rect.width(), rect.height()) / 2f
        log("Cutout source: boundingRects fallback, center=($cx, $cy), radius=$radius")
        return Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
    }

    // Mock cutout for emulators/flat displays
    private fun buildMockCutoutPath(): Path {
        val dm = resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val radius = 15f * dm.density
        val cy = radius * 2f
        log("Cutout source: mock circle, center=($cx, $cy), radius=$radius")
        return Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
    }

    private fun startFinishAnimation() {
        animator.startFinish(
            style = IndicatorState.finishStyle,
            holdMs = IndicatorState.finishHoldMs,
            exitMs = IndicatorState.finishExitMs,
            pulseEnabled = IndicatorState.completionPulseEnabled,
        ) { progress = 0 }
    }

    fun startDynamicPreviewAnim() {
        animator.startDynamicPreviewAnim(
            finishStyle = IndicatorState.finishStyle,
            holdMs = IndicatorState.finishHoldMs,
            exitMs = IndicatorState.finishExitMs,
            pulseEnabled = IndicatorState.completionPulseEnabled,
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

        if (!IndicatorState.enabled) return
        if (isPowerSaveActive && IndicatorState.powerSaverMode == "disable") return

        if (animator.isErrorAnimating) {
            computeCalibratedArcBounds()
            renderer.updateBounds(arcBounds)
            errorPaint.alpha = (animator.errorAlpha * 255).toInt()
            renderer.drawFullRing(canvas, errorPaint)
            return
        }

        val effectiveProgress =
            when {
                animator.isGeometryPreviewActive -> 100
                animator.isPreviewAnimating -> animator.previewProgress
                else -> progress
            }

        val hideDelay = IndicatorState.burnInHideMs
        val isBurnInHidden =
            hideDelay > 0 &&
                effectiveProgress in 1..99 && lastActivityTime > 0 &&
                System.currentTimeMillis() - lastActivityTime >= hideDelay

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
                // Use the calibrated bounds so the pivot reflects ring offsets/scales
                arcBounds.applyCalibration()
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
                val baseColor = resolvedRingColor
                val successColor =
                    if (IndicatorState.finishUseFlashColor) {
                        shinePaint.color
                    } else {
                        brightenColor(baseColor, animator.successColorBlend)
                    }
                animatedPaint.color =
                    blendColors(baseColor, successColor, animator.successColorBlend)
            }

            val isActiveProgress =
                effectiveProgress in 1..99 || animator.isGeometryPreviewActive ||
                    animator.isPreviewAnimating
            val showBackgroundRing =
                IndicatorState.backgroundRingEnabled && !animator.isFinishAnimating &&
                    !animator.isErrorAnimating &&
                    isActiveProgress

            if (showBackgroundRing) {
                scaledPath.computeBounds(arcBounds, true)
                arcBounds.applyCalibration()
                renderer.updateBounds(arcBounds)
                val bgOpacityBase =
                    if (isPowerSaveActive && IndicatorState.powerSaverMode == "dim") {
                        (IndicatorState.backgroundRingOpacity * POWER_SAVER_DIM_FACTOR).toInt()
                    } else {
                        IndicatorState.backgroundRingOpacity
                    }
                backgroundRingPaint.alpha =
                    (bgOpacityBase * 255 / 100 * animator.displayAlpha).toInt()
                renderer.drawFullRing(this, backgroundRingPaint)
            }

            if (animator.isFinishAnimating) {
                drawFinishAnimation(this, animatedPaint)
            } else {
                scaledPath.computeBounds(arcBounds, true)
                arcBounds.applyCalibration()
                renderer.updateBounds(arcBounds)
                val fraction = smoothProgressFor(effectiveProgress)
                renderer.drawProgress(this, fraction, IndicatorState.clockwise, animatedPaint)

                val showLabels = effectiveProgress in 1..99 || animator.isGeometryPreviewActive
                if (showLabels) {
                    drawLabels(this, effectiveProgress)
                    drawAppIcon(this)
                }
            }

            // Badge drawn BELOW the ring (not at center - that's behind camera hardware)
            val showBadge =
                !animator.isPreviewAnimating && IndicatorState.showDownloadCount &&
                    (activeDownloadCount > 1 || animator.isGeometryPreviewActive)
            if (showBadge) {
                scaledPath.computeBounds(arcBounds, true)
                val viewDensity = this@IndicatorView.density
                val badgeRotation = display?.rotation ?: Surface.ROTATION_0
                val badgeLocked = IndicatorState.badgeLockRotation
                val badgeEffRotation = if (badgeLocked) Surface.ROTATION_0 else badgeRotation
                val badgeSlot = RotationSlot.fromSurfaceRotation(badgeEffRotation)
                val badgeOffset = IndicatorState.badgeOffsets[badgeSlot]
                val badgeCenterX = arcBounds.centerX() + badgeOffset.x * viewDensity
                val badgeTop =
                    arcBounds.bottom + BADGE_TOP_PADDING_DP * viewDensity +
                        badgeOffset.y * viewDensity
                val badgeCount = if (animator.isGeometryPreviewActive) 3 else activeDownloadCount
                if (badgeLocked && badgeRotation != Surface.ROTATION_0) {
                    withSave {
                        rotate(
                            -90f * (badgeRotation / Surface.ROTATION_90).toFloat(),
                            arcBounds.centerX(),
                            arcBounds.centerY(),
                        )
                        badgePainter.draw(
                            this,
                            badgeCenterX,
                            badgeTop,
                            badgeCount,
                            effectiveOpacity,
                        )
                    }
                } else {
                    badgePainter.draw(this, badgeCenterX, badgeTop, badgeCount, effectiveOpacity)
                }
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
        renderer.updateBounds(arcBounds)
        if (IndicatorState.finishStyle == "segmented") {
            val count = IndicatorState.segmentCount
            val gap = IndicatorState.segmentGapDegrees
            val arc = (360f - count * gap) / count
            renderer.drawSegmented(
                canvas,
                count,
                gap,
                arc,
                animator.segmentHighlight,
                paint,
                shinePaint,
                animator.displayAlpha,
            )
        } else {
            renderer.drawFullRing(canvas, paint)
        }
    }

    private data class TextSpec(
        val text: String,
        val paint: Paint,
        val strokePaint: Paint?,
        val haloPaint: Paint?,
        val x: Float,
        val y: Float,
        val align: Paint.Align? = null,
        val locked: Boolean = false,
    )

    private fun drawLabels(
        canvas: Canvas,
        progressVal: Int,
    ) {
        val alpha = effectiveOpacity * 255 / 100
        val padding = LABEL_PADDING_DP * density
        val specs = mutableListOf<TextSpec>()
        val rotation = display?.rotation ?: Surface.ROTATION_0
        val percentStrokeWidthPx = IndicatorState.percentTextStrokeWidth * density
        val filenameStrokeWidthPx = IndicatorState.filenameTextStrokeWidth * density
        val percentIsOval = IndicatorState.percentTextShadowMode == "oval"
        val filenameIsOval = IndicatorState.filenameTextShadowMode == "oval"

        if (IndicatorState.percentTextEnabled) {
            val locked = IndicatorState.percentTextLockRotation
            val effRotation = if (locked) Surface.ROTATION_0 else rotation
            val slot = RotationSlot.fromSurfaceRotation(effRotation)
            val text = "$progressVal%"
            val textWidth = percentPaint.measureText(text)
            val (baseX, baseY, align) =
                computeLabelPosition(
                    if (locked) {
                        IndicatorState.percentTextPosition
                    } else {
                        rotatePosition(IndicatorState.percentTextPosition, rotation)
                    },
                    padding,
                    percentPaint.textSize,
                    textWidth,
                )
            val pctOffset = IndicatorState.percentTextOffsets[slot]
            val x = baseX + pctOffset.x * density
            val y = baseY + pctOffset.y * density
            val stroke = if (percentStrokeWidthPx > 0f) percentStrokePaint else null
            val halo = if (percentIsOval) percentHalo.paint else null
            specs += TextSpec(text, percentPaint, stroke, halo, x, y, align, locked)
        }

        val isGeometryPreview = animator.isGeometryPreviewActive
        val filenameToShow =
            currentFilename ?: if (isGeometryPreview) IndicatorState.previewFilenameText else null

        if (IndicatorState.filenameTextEnabled && filenameToShow != null &&
            (activeDownloadCount <= 1 || isGeometryPreview)
        ) {
            val locked = IndicatorState.filenameTextLockRotation
            val effRotation = if (locked) Surface.ROTATION_0 else rotation
            val slot = RotationSlot.fromSurfaceRotation(effRotation)
            val truncated =
                if (IndicatorState.filenameTruncateEnabled) {
                    truncateWithEllipsis(
                        filenameToShow,
                        IndicatorState.filenameMaxChars,
                        IndicatorState.filenameEllipsize,
                    )
                } else {
                    filenameToShow
                }
            val isLandscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
            val isVertical = IndicatorState.filenameVerticalText && isLandscape && !locked
            if (isVertical && truncated.isNotEmpty()) {
                filenamePaint.color = resolvedRingColor
                filenamePaint.alpha = alpha
                val fnOffset = IndicatorState.filenameTextOffsets[slot]
                val fm = filenamePaint.fontMetrics
                val charHeight = fm.descent - fm.ascent
                val totalHeight = truncated.length * charHeight
                val startY = arcBounds.centerY() - totalHeight / 2 + fnOffset.y * density
                // Place on whichever side of the ring faces the screen center
                val screenCenterX = width / 2f
                val x =
                    (
                        if (arcBounds.centerX() < screenCenterX) {
                            arcBounds.right + padding
                        } else {
                            arcBounds.left - padding
                        }
                    ) + fnOffset.x * density
                for (i in truncated.indices) {
                    canvas.drawText(
                        truncated,
                        i,
                        i + 1,
                        x,
                        startY + i * charHeight - fm.ascent,
                        filenamePaint,
                    )
                }
            } else {
                val (baseX, baseY, align) =
                    computeLabelPosition(
                        if (locked) {
                            IndicatorState.filenameTextPosition
                        } else {
                            rotatePosition(IndicatorState.filenameTextPosition, rotation)
                        },
                        padding,
                        filenamePaint.textSize,
                        textWidth = null,
                    )
                val fnOffset = IndicatorState.filenameTextOffsets[slot]
                val x = baseX + fnOffset.x * density
                val y = baseY + fnOffset.y * density
                val stroke = if (filenameStrokeWidthPx > 0f) filenameStrokePaint else null
                val halo = if (filenameIsOval) filenameHalo.paint else null
                specs += TextSpec(truncated, filenamePaint, stroke, halo, x, y, align, locked)
            }
        }

        for (spec in specs) {
            spec.paint.color = resolvedRingColor
            spec.paint.alpha = alpha
            spec.align?.let { (spec.paint as? TextPaint)?.textAlign = it }
            spec.strokePaint?.apply {
                this.alpha = alpha
                spec.align?.let { (this as? TextPaint)?.textAlign = it }
            }
            val drawBlock = {
                spec.haloPaint?.let { drawTextOvalHalo(canvas, spec, it) }
                spec.strokePaint?.let {
                    canvas.drawText(spec.text, spec.x, spec.y, it)
                }
                canvas.drawText(spec.text, spec.x, spec.y, spec.paint)
            }
            if (spec.locked && rotation != Surface.ROTATION_0) {
                canvas.withSave {
                    rotate(
                        -90f * (rotation / Surface.ROTATION_90).toFloat(),
                        arcBounds.centerX(),
                        arcBounds.centerY(),
                    )
                    drawBlock()
                }
            } else {
                drawBlock()
            }
        }
    }

    private fun drawTextOvalHalo(
        canvas: Canvas,
        spec: TextSpec,
        halo: Paint,
    ) {
        if (halo.maskFilter == null) return
        val bounds = textBoundsScratch
        spec.paint.getTextBounds(spec.text, 0, spec.text.length, bounds)
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return
        val padX = height * 0.5f
        val padY = height * 0.35f
        val advance = spec.paint.measureText(spec.text)
        val originX =
            when (spec.paint.textAlign) {
                Paint.Align.CENTER -> spec.x - advance / 2f
                Paint.Align.RIGHT -> spec.x - advance
                else -> spec.x
            }
        val left = originX + bounds.left - padX
        val top = spec.y + bounds.top - padY
        val right = originX + bounds.right + padX
        val bottom = spec.y + bounds.bottom + padY
        val radius = (bottom - top) / 2f
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, halo)
    }

    private fun drawAppIcon(canvas: Canvas) {
        if (!IndicatorState.appIconEnabled) return

        val isGeometryPreview = animator.isGeometryPreviewActive
        val packageName =
            currentPackageName ?: if (isGeometryPreview) context.packageName else return

        if (activeDownloadCount > 1 && !isGeometryPreview) return

        val sizePx = IndicatorState.appIconSize * density
        val padding = LABEL_PADDING_DP * density
        val rotation = display?.rotation ?: Surface.ROTATION_0
        val locked = IndicatorState.appIconLockRotation
        val effRotation = if (locked) Surface.ROTATION_0 else rotation
        val slot = RotationSlot.fromSurfaceRotation(effRotation)

        val (baseX, baseY, _) =
            computeLabelPosition(
                if (locked) {
                    IndicatorState.appIconPosition
                } else {
                    rotatePosition(IndicatorState.appIconPosition, rotation)
                },
                padding,
                sizePx,
                sizePx,
            )

        val iconOffset = IndicatorState.appIconOffsets[slot]
        val x = baseX + iconOffset.x * density - sizePx / 2
        val y = baseY + iconOffset.y * density - sizePx

        if (locked && rotation != Surface.ROTATION_0) {
            canvas.withSave {
                rotate(
                    -90f * (rotation / Surface.ROTATION_90).toFloat(),
                    arcBounds.centerX(),
                    arcBounds.centerY(),
                )
                iconPainter.draw(this, x, y, packageName)
            }
        } else {
            iconPainter.draw(canvas, x, y, packageName)
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
                val x =
                    if (textWidth != null) {
                        arcBounds.left - textWidth / 2 - padding
                    } else {
                        arcBounds.left - padding
                    }
                Triple(
                    x,
                    arcBounds.centerY() + textSize / 3,
                    if (textWidth != null) null else Paint.Align.RIGHT,
                )
            }

            "right" -> {
                val x =
                    if (textWidth != null) {
                        arcBounds.right + textWidth / 2 + padding
                    } else {
                        arcBounds.right + padding
                    }
                Triple(
                    x,
                    arcBounds.centerY() + textSize / 3,
                    if (textWidth != null) null else Paint.Align.LEFT,
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
                val x =
                    if (textWidth != null) {
                        arcBounds.right + textWidth / 2 + padding
                    } else {
                        arcBounds.right + padding
                    }
                val y =
                    if (textWidth != null) {
                        arcBounds.centerY() + textSize / 3
                    } else {
                        arcBounds.top - padding
                    }
                Triple(
                    x,
                    y,
                    if (textWidth != null) null else Paint.Align.LEFT,
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

    // Rotate portrait-calibrated offset vector to match current display rotation
    private fun rotateOffset(
        dx: Float,
        dy: Float,
    ): Pair<Float, Float> =
        when (display?.rotation) {
            Surface.ROTATION_90 -> dy to -dx
            Surface.ROTATION_180 -> -dx to -dy
            Surface.ROTATION_270 -> -dy to dx
            else -> dx to dy
        }

    // Transform portrait-calibrated position string to match current display rotation
    private fun rotatePosition(
        position: String,
        rotation: Int,
    ): String =
        when (rotation) {
            Surface.ROTATION_90 -> {
                when (position) {
                    "right" -> "bottom"
                    "left" -> "top"
                    "top" -> "right"
                    "bottom" -> "left"
                    "top_right" -> "bottom_right"
                    "top_left" -> "top_right"
                    "bottom_left" -> "top_left"
                    "bottom_right" -> "bottom_left"
                    else -> position
                }
            }

            Surface.ROTATION_270 -> {
                when (position) {
                    "right" -> "top"
                    "left" -> "bottom"
                    "top" -> "left"
                    "bottom" -> "right"
                    "top_right" -> "top_left"
                    "top_left" -> "bottom_left"
                    "bottom_left" -> "bottom_right"
                    "bottom_right" -> "top_right"
                    else -> position
                }
            }

            Surface.ROTATION_180 -> {
                when (position) {
                    "right" -> "left"
                    "left" -> "right"
                    "top" -> "bottom"
                    "bottom" -> "top"
                    "top_right" -> "bottom_left"
                    "top_left" -> "bottom_right"
                    "bottom_left" -> "top_right"
                    "bottom_right" -> "top_left"
                    else -> position
                }
            }

            else -> {
                position
            }
        }

    // Apply calibration: normalize base, offset, then scale
    private fun RectF.applyCalibration() {
        val (offsetX, offsetY) =
            rotateOffset(
                IndicatorState.ringOffsetX,
                IndicatorState.ringOffsetY,
            )
        val scaleX = IndicatorState.ringScaleX
        val scaleY = IndicatorState.ringScaleY

        if (width() == 0f && height() == 0f) return

        // Arc mode: normalize to square so drawArc produces a circle as base
        // Path mode: keep original aspect ratio for pill-shaped cutouts
        val halfBase = if (!IndicatorState.pathMode) maxOf(width(), height()) / 2f else null

        val centerX = centerX() + offsetX
        val centerY = centerY() + offsetY

        val halfWidth = (halfBase ?: (width() / 2f)) * scaleX
        val halfHeight = (halfBase ?: (height() / 2f)) * scaleY

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
        private const val PRIVATE_FLAG_EXCLUDE_FROM_SCREEN_MAGNIFICATION = 1 shl 21

        // Drawing constants
        private const val POWER_SAVER_DIM_FACTOR = 0.5f
        private const val SHINE_STROKE_MULTIPLIER = 1.2f
        private const val ERROR_STROKE_MULTIPLIER = 1.5f
        private const val BADGE_TOP_PADDING_DP = 4f
        private const val LABEL_PADDING_DP = 4f

        @SuppressLint("InlinedApi")
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
            if (Build.VERSION.SDK_INT >= 35 && IndicatorState.hdrEnabled) {
                params.colorMode = ActivityInfo.COLOR_MODE_HDR
                params.desiredHdrHeadroom = IndicatorState.hdrHeadroom
            }

            runCatching {
                val privateFlagsField = params.javaClass.accessibleField("privateFlags")
                val currentFlags = privateFlagsField.getInt(params)
                val newFlags = currentFlags or PRIVATE_FLAG_EXCLUDE_FROM_SCREEN_MAGNIFICATION
                privateFlagsField.setInt(params, newFlags)
            }.onFailure { log("magnification exclusion failed", it) }

            wm.addView(view, params)
            view.windowParams = params
            return view
        }
    }
}
