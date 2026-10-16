package eu.hxreborn.phdp.xposed.hook

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import eu.hxreborn.phdp.util.Logger
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.util.logDebug

/**
 * Tells Duo Status Bar about the download this module is tracking.
 */
object DuoDownloadPublisher {
    const val ACTION = "io.github.kvmy666.duostatusbar.action.DOWNLOAD"
    const val ACTION_QUERY = "io.github.kvmy666.duostatusbar.action.DOWNLOAD_QUERY"

    private const val EXTRA_ACTIVE = "active"
    private const val EXTRA_PROGRESS = "progress"
    private const val EXTRA_CLOCKWISE = "clockwise"
    private const val EXTRA_COLOR = "color"
    private const val EXTRA_OPACITY = "opacity"
    private const val EXTRA_SHOW_LABEL = "showLabel"
    private const val EXTRA_GRADIENT_ENABLED = "gradientEnabled"
    private const val EXTRA_GRADIENT_COLORS = "gradientColors"
    private const val EXTRA_GRADIENT_ANGLE = "gradientAngle"
    private const val DIM = 0.5f
    private const val PREVIEW_PROGRESS = 100
    private const val PREVIEW_DURATION_MS = 3000L

    private val handler = Handler(Looper.getMainLooper())
    private var context: Context? = null
    private var queryReceiver: BroadcastReceiver? = null
    private var animator: ValueAnimator? = null
    private var displayed = 0f
    private var lastProgress = 0
    private var startedAt = 0L
    private var active = false
    private var failed = false
    private var holding = false
    private var powerSave = false
    private var burnedIn = false
    private var previewing = false
    private var exitFade = 1f
    private var lastPublishTrace: String? = null
    private var lastPayload: List<Any>? = null

    private val clearRunnable = Runnable { if (holding) fadeOutThenClear() else clearNow() }
    private val previewClearRunnable = Runnable { clearPreviewNow() }
    private val burnInRunnable =
        Runnable {
            if (!active || holding || failed || lastProgress !in 1..99) return@Runnable
            burnedIn = true
            publish()
        }

    fun attach(context: Context) {
        this.context = context
        log(
            "duo publisher attach ctxPkg=${context.packageName} " +
                "mode=${IndicatorState.duoStatusBarMode} " +
                "receiver=${queryReceiver != null}",
        )
        if (queryReceiver != null) {
            publish(force = true)
            return
        }
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context?,
                    intent: Intent?,
                ) {
                    logDebug {
                        "duo query received ctxPkg=${ctx?.packageName} " +
                            "storedPkg=${context.packageName} " +
                            "mode=${IndicatorState.duoStatusBarMode} " +
                            "active=$active progress=${displayed.toInt()}"
                    }
                    publish(force = true)
                }
            }
        queryReceiver = receiver
        val filter = IntentFilter(ACTION_QUERY)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            log("duo query receiver registered ctxPkg=${context.packageName}")
        }.onFailure { log("duo download query receiver failed", it) }
        publish(force = true)
    }

    fun detach() {
        animator?.cancel()
        handler.removeCallbacksAndMessages(null)
        val ctx = context
        val receiver = queryReceiver
        if (ctx != null && receiver != null) {
            runCatching { ctx.unregisterReceiver(receiver) }
        }
        queryReceiver = null
        log(
            "duo publisher detach ctxPkg=${ctx?.packageName} " +
                "active=$active progress=${displayed.toInt()}",
        )
        if (active || failed || burnedIn || previewing) {
            active = false
            failed = false
            holding = false
            burnedIn = false
            previewing = false
            publish()
        }
        context = null
    }

    fun setPowerSave(on: Boolean) {
        handler.post {
            if (powerSave == on) return@post
            powerSave = on
            logDebug { "duo power-save -> $on mode=${IndicatorState.duoStatusBarMode}" }
            publish()
        }
    }

    fun onProgress(progress: Int) {
        handler.post { onProgressMain(progress) }
    }

    fun onComplete() {
        handler.post { onCompleteMain() }
    }

    fun onFailed() {
        handler.post { onFailedMain() }
    }

    /** A download notification arrived, even when the percentage did not move. */
    fun onActivity() {
        handler.post { armBurnIn(reveal = true) }
    }

    /** Mode flipped, or a colour / direction the wash reads changed. */
    fun refresh() {
        handler.post {
            logDebug {
                "duo refresh mode=${IndicatorState.duoStatusBarMode} " +
                    "activeDownloads=${DownloadProgressHook.hasActiveDownloads()} " +
                    "avg=${DownloadProgressHook.currentAverage()}"
            }
            if (!IndicatorState.duoStatusBarMode) {
                clearNow()
                return@post
            }
            if (DownloadProgressHook.hasActiveDownloads()) {
                onProgressMain(DownloadProgressHook.currentAverage())
            } else {
                publish()
            }
        }
    }

    fun showStaticPreview(autoHide: Boolean = true) {
        handler.post {
            logDebug {
                "duo preview show autoHide=$autoHide mode=${IndicatorState.duoStatusBarMode} " +
                    "activeDownloads=${DownloadProgressHook.hasActiveDownloads()}"
            }
            if (!IndicatorState.duoStatusBarMode) {
                clearPreviewNow()
                return@post
            }
            if (DownloadProgressHook.hasActiveDownloads()) {
                previewing = false
                handler.removeCallbacks(previewClearRunnable)
                onProgressMain(DownloadProgressHook.currentAverage())
                return@post
            }
            previewing = true
            failed = false
            burnedIn = false
            handler.removeCallbacks(previewClearRunnable)
            handler.removeCallbacks(burnInRunnable)
            publish()
            if (autoHide) {
                handler.postDelayed(previewClearRunnable, PREVIEW_DURATION_MS)
            }
        }
    }

    fun cancelStaticPreview() {
        handler.post { clearPreviewNow() }
    }

    private fun onProgressMain(progress: Int) {
        val hadPreview = previewing
        previewing = false
        handler.removeCallbacks(previewClearRunnable)
        logDebug {
            "duo progress event progress=$progress mode=${IndicatorState.duoStatusBarMode} " +
                "active=$active holding=$holding"
        }
        if (!IndicatorState.duoStatusBarMode) {
            clearNow()
            return
        }
        if (progress <= 0) {
            // Completion posts this 0 after it has already scheduled the real clear. Cancelling
            // that clear is what left a finished download painted on the ring.
            if (holding) return
            if (hadPreview && !active) publish()
            handler.removeCallbacks(clearRunnable)
            handler.postDelayed(clearRunnable, 100L)
            logDebug { "duo progress <=0 scheduled clear" }
            return
        }
        handler.removeCallbacks(clearRunnable)
        holding = false
        failed = false
        active = true
        lastProgress = progress
        if (startedAt == 0L) startedAt = SystemClock.elapsedRealtime()
        armBurnIn(reveal = true)
        animateTo(progress)
    }

    private fun onCompleteMain() {
        previewing = false
        handler.removeCallbacks(previewClearRunnable)
        logDebug {
            "duo complete event mode=${IndicatorState.duoStatusBarMode} " +
                "active=$active progress=$lastProgress"
        }
        if (!IndicatorState.duoStatusBarMode) return
        if (keepActiveDownloads()) return
        handler.removeCallbacks(clearRunnable)
        handler.removeCallbacks(burnInRunnable)
        burnedIn = false
        holding = true
        failed = false
        active = true
        lastProgress = 100
        animateTo(100)
        val elapsed = if (startedAt == 0L) 0L else SystemClock.elapsedRealtime() - startedAt
        val minWait =
            if (IndicatorState.minVisibilityEnabled) {
                (IndicatorState.minVisibilityMs.toLong() - elapsed).coerceAtLeast(0L)
            } else {
                0L
            }
        handler.postDelayed(clearRunnable, minWait + IndicatorState.finishHoldMs.toLong())
    }

    private fun onFailedMain() {
        previewing = false
        handler.removeCallbacks(previewClearRunnable)
        logDebug {
            "duo failed event mode=${IndicatorState.duoStatusBarMode} " +
                "active=$active progress=$lastProgress"
        }
        if (!IndicatorState.duoStatusBarMode) return
        if (keepActiveDownloads()) return
        handler.removeCallbacks(clearRunnable)
        handler.removeCallbacks(burnInRunnable)
        burnedIn = false
        holding = false
        failed = true
        active = lastProgress > 0
        displayed = lastProgress.toFloat()
        publish()
        handler.postDelayed(clearRunnable, IndicatorState.finishExitMs.toLong())
    }

    private fun keepActiveDownloads(): Boolean {
        if (!DownloadProgressHook.hasActiveDownloads()) return false
        onProgressMain(DownloadProgressHook.currentAverage())
        return true
    }

    private fun fadeOutThenClear() {
        animator?.cancel()
        animator =
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = IndicatorState.finishExitMs.toLong()
                addUpdateListener {
                    exitFade = it.animatedValue as Float
                    if (it.animatedFraction >= 1f) clearNow() else publish()
                }
                start()
            }
    }

    private fun clearNow() {
        animator?.cancel()
        exitFade = 1f
        handler.removeCallbacks(clearRunnable)
        handler.removeCallbacks(previewClearRunnable)
        handler.removeCallbacks(burnInRunnable)
        val was = active || failed || burnedIn || previewing
        active = false
        failed = false
        holding = false
        burnedIn = false
        previewing = false
        displayed = 0f
        lastProgress = 0
        startedAt = 0L
        if (was) {
            logDebug { "duo clear" }
            publish()
        }
    }

    private fun clearPreviewNow() {
        handler.removeCallbacks(previewClearRunnable)
        if (!previewing) return
        previewing = false
        logDebug { "duo preview clear" }
        publish()
    }

    private fun animateTo(target: Int) {
        val from = displayed
        animator?.cancel()
        exitFade = 1f
        val duration = IndicatorState.progressAnimMs.toLong()
        if (duration <= 0L || from == target.toFloat()) {
            displayed = target.toFloat()
            publish()
            return
        }
        animator =
            ValueAnimator.ofFloat(from, target.toFloat()).apply {
                this.duration = duration
                interpolator =
                    when (IndicatorState.progressEasing) {
                        "accelerate" -> AccelerateInterpolator()
                        "decelerate" -> DecelerateInterpolator()
                        "ease_in_out" -> AccelerateDecelerateInterpolator()
                        else -> LinearInterpolator()
                    }
                addUpdateListener {
                    displayed = it.animatedValue as Float
                    publish()
                }
                start()
            }
    }

    private fun publish(force: Boolean = false) {
        val ctx = context ?: return
        val mode = IndicatorState.duoStatusBarMode
        val suppressed = powerSave && IndicatorState.powerSaverMode == "disable"
        val showing = mode && (active || previewing) && !suppressed && (!burnedIn || previewing)
        val opacity =
            when {
                !showing -> 0f
                powerSave && IndicatorState.powerSaverMode == "dim" -> DIM
                else -> 1f
            } * exitFade
        val progress =
            if (previewing) {
                PREVIEW_PROGRESS
            } else {
                displayed.toInt().coerceIn(0, 100)
            }
        val color = washColor(ctx)
        val gradientEnabled = IndicatorState.gradientEnabled && !failed && !holding
        val gradientColors = IndicatorState.gradientColors
        val payload =
            listOf(
                showing,
                progress,
                IndicatorState.clockwise,
                color,
                opacity,
                IndicatorState.percentTextEnabled,
                gradientEnabled,
                gradientColors.toList(),
                IndicatorState.gradientAngle,
            )
        if (!force && payload == lastPayload) return
        val intent =
            Intent(ACTION).setPackage(ctx.packageName).apply {
                putExtra(EXTRA_ACTIVE, showing)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_CLOCKWISE, IndicatorState.clockwise)
                putExtra(EXTRA_COLOR, color)
                putExtra(EXTRA_OPACITY, opacity)
                putExtra(EXTRA_SHOW_LABEL, IndicatorState.percentTextEnabled)
                putExtra(EXTRA_GRADIENT_ENABLED, gradientEnabled)
                putExtra(EXTRA_GRADIENT_COLORS, gradientColors)
                putExtra(EXTRA_GRADIENT_ANGLE, IndicatorState.gradientAngle)
            }
        if (Logger.verboseEnabled && shouldLogPublish(progress, showing)) {
            val trace =
                "target=${ctx.packageName} mode=$mode active=$active " +
                    "showing=$showing progress=$progress " +
                    "suppressed=$suppressed burnedIn=$burnedIn previewing=$previewing " +
                    "holding=$holding failed=$failed " +
                    "opacity=$opacity"
            if (trace != lastPublishTrace) {
                lastPublishTrace = trace
                logDebug { "duo publish $trace" }
            }
        }
        runCatching { ctx.sendBroadcast(intent) }
            .onSuccess { lastPayload = payload }
            .onFailure {
                lastPayload = null
                log("duo download broadcast failed", it)
            }
    }

    private fun shouldLogPublish(
        progress: Int,
        showing: Boolean,
    ): Boolean = !showing || progress == 0 || progress == 100 || progress % 25 == 0

    private fun armBurnIn(reveal: Boolean) {
        handler.removeCallbacks(burnInRunnable)
        if (reveal && burnedIn) {
            burnedIn = false
            publish()
        }
        val delay = IndicatorState.burnInHideMs
        if (delay > 0 && active && !holding && !failed && lastProgress in 1..99) {
            handler.postDelayed(burnInRunnable, delay.toLong())
        }
    }

    private fun washColor(ctx: Context): Int {
        if (IndicatorState.gradientEnabled && !failed && !holding) {
            return IndicatorState.gradientColors.firstOrNull() ?: IndicatorState.color
        }
        val base =
            when {
                failed -> IndicatorState.errorColor
                holding && IndicatorState.finishUseFlashColor -> IndicatorState.finishFlashColor
                else -> IndicatorState.duoProgressColor
            }
        if (!IndicatorState.materialYouEnabled || Build.VERSION.SDK_INT < 31) return base
        val palette =
            when {
                failed -> IndicatorState.materialYouErrorPalette
                holding -> IndicatorState.materialYouSuccessPalette
                else -> IndicatorState.materialYouProgressPalette
            }
        val shade =
            when {
                failed -> IndicatorState.materialYouErrorShade
                holding -> IndicatorState.materialYouSuccessShade
                else -> IndicatorState.materialYouProgressShade
            }
        val resId = ctx.resources.getIdentifier("system_${palette}_$shade", "color", "android")
        return if (resId != 0) ctx.getColor(resId) else base
    }
}
