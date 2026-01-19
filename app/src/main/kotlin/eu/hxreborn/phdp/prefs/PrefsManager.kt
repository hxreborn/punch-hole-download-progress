package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log

object PrefsManager {
    const val PREFS_GROUP = "phdp_settings"

    // Keys
    const val KEY_ENABLED = "enabled"
    const val KEY_COLOR = "color"
    const val KEY_STROKE_WIDTH = "stroke_width"
    const val KEY_RING_GAP = "ring_gap"
    const val KEY_OPACITY = "opacity"
    const val KEY_HOOKS_FEEDBACK = "hooks_feedback"
    const val KEY_APP_VISIBLE = "app_visible"
    const val KEY_CLOCKWISE = "clockwise"
    const val KEY_TEST_PROGRESS = "test_progress"
    const val KEY_TEST_ERROR = "test_error"
    const val KEY_PROGRESS_EASING = "progress_easing"
    const val KEY_ERROR_COLOR = "error_color"
    const val KEY_POWER_SAVER_MODE = "power_saver_mode"
    const val KEY_IDLE_RING_ENABLED = "idle_ring_enabled"
    const val KEY_IDLE_RING_OPACITY = "idle_ring_opacity"
    const val KEY_SHOW_DOWNLOAD_COUNT = "show_download_count"
    const val KEY_MIN_VISIBILITY_ENABLED = "min_visibility_enabled"
    const val KEY_MIN_VISIBILITY_MS = "min_visibility_ms"
    const val KEY_COMPLETION_PULSE_ENABLED = "completion_pulse_enabled"
    const val KEY_PERCENT_TEXT_ENABLED = "percent_text_enabled"
    const val KEY_PERCENT_TEXT_POSITION = "percent_text_position"
    const val KEY_FILENAME_TEXT_ENABLED = "filename_text_enabled"
    const val KEY_FILENAME_TEXT_POSITION = "filename_text_position"
    const val KEY_FINISH_STYLE = "finish_style"
    const val KEY_FINISH_HOLD_MS = "finish_hold_ms"
    const val KEY_FINISH_EXIT_MS = "finish_exit_ms"
    const val KEY_FINISH_USE_FLASH_COLOR = "finish_use_flash_color"
    const val KEY_FINISH_FLASH_COLOR = "finish_flash_color"
    const val KEY_PREVIEW_TRIGGER = "preview_trigger"

    // Keys that trigger preview when changed
    private val VISUAL_KEYS =
        setOf(KEY_COLOR, KEY_STROKE_WIDTH, KEY_RING_GAP, KEY_OPACITY, KEY_CLOCKWISE)

    // Defaults
    const val DEFAULT_ENABLED = true
    const val DEFAULT_COLOR = 0xFF00FFFF.toInt()
    const val DEFAULT_STROKE_WIDTH = 2f
    const val DEFAULT_RING_GAP = 1.155f
    const val DEFAULT_OPACITY = 90
    const val DEFAULT_HOOKS_FEEDBACK = false
    const val DEFAULT_PROGRESS_EASING = "linear"
    const val DEFAULT_ERROR_COLOR = 0xFFFF5252.toInt()
    const val DEFAULT_POWER_SAVER_MODE = "normal"
    const val DEFAULT_IDLE_RING_ENABLED = false
    const val DEFAULT_IDLE_RING_OPACITY = 20
    const val DEFAULT_SHOW_DOWNLOAD_COUNT = false
    const val DEFAULT_MIN_VISIBILITY_ENABLED = true
    const val DEFAULT_MIN_VISIBILITY_MS = 500
    const val DEFAULT_COMPLETION_PULSE_ENABLED = true
    const val DEFAULT_PERCENT_TEXT_ENABLED = false
    const val DEFAULT_PERCENT_TEXT_POSITION = "right"
    const val DEFAULT_FILENAME_TEXT_ENABLED = false
    const val DEFAULT_FILENAME_TEXT_POSITION = "top_right"
    const val DEFAULT_FINISH_STYLE = "pop"
    const val DEFAULT_FINISH_HOLD_MS = 500
    const val DEFAULT_FINISH_EXIT_MS = 500
    const val DEFAULT_FINISH_USE_FLASH_COLOR = true
    const val DEFAULT_FINISH_FLASH_COLOR = 0xFFFFFFFF.toInt()

    // Ranges
    const val MIN_STROKE_WIDTH = 0.5f
    const val MAX_STROKE_WIDTH = 5.0f
    const val MIN_RING_GAP = 1.0f
    const val MAX_RING_GAP = 1.6f
    const val MIN_OPACITY = 1
    const val MAX_OPACITY = 100
    const val MIN_FINISH_HOLD_MS = 0
    const val MAX_FINISH_HOLD_MS = 5000
    const val MIN_FINISH_EXIT_MS = 50
    const val MAX_FINISH_EXIT_MS = 3000
    const val MIN_MIN_VISIBILITY_MS = 0
    const val MAX_MIN_VISIBILITY_MS = 2000

    // Reset defaults (excludes KEY_ENABLED)
    val DEFAULTS: Map<String, Any> =
        mapOf(
            KEY_COLOR to DEFAULT_COLOR,
            KEY_STROKE_WIDTH to DEFAULT_STROKE_WIDTH,
            KEY_RING_GAP to DEFAULT_RING_GAP,
            KEY_OPACITY to DEFAULT_OPACITY,
            KEY_HOOKS_FEEDBACK to DEFAULT_HOOKS_FEEDBACK,
            KEY_CLOCKWISE to true,
            KEY_PROGRESS_EASING to DEFAULT_PROGRESS_EASING,
            KEY_ERROR_COLOR to DEFAULT_ERROR_COLOR,
            KEY_POWER_SAVER_MODE to DEFAULT_POWER_SAVER_MODE,
            KEY_IDLE_RING_ENABLED to DEFAULT_IDLE_RING_ENABLED,
            KEY_IDLE_RING_OPACITY to DEFAULT_IDLE_RING_OPACITY,
            KEY_SHOW_DOWNLOAD_COUNT to DEFAULT_SHOW_DOWNLOAD_COUNT,
            KEY_MIN_VISIBILITY_ENABLED to DEFAULT_MIN_VISIBILITY_ENABLED,
            KEY_MIN_VISIBILITY_MS to DEFAULT_MIN_VISIBILITY_MS,
            KEY_COMPLETION_PULSE_ENABLED to DEFAULT_COMPLETION_PULSE_ENABLED,
            KEY_PERCENT_TEXT_ENABLED to DEFAULT_PERCENT_TEXT_ENABLED,
            KEY_PERCENT_TEXT_POSITION to DEFAULT_PERCENT_TEXT_POSITION,
            KEY_FILENAME_TEXT_ENABLED to DEFAULT_FILENAME_TEXT_ENABLED,
            KEY_FILENAME_TEXT_POSITION to DEFAULT_FILENAME_TEXT_POSITION,
            KEY_FINISH_STYLE to DEFAULT_FINISH_STYLE,
            KEY_FINISH_HOLD_MS to DEFAULT_FINISH_HOLD_MS,
            KEY_FINISH_EXIT_MS to DEFAULT_FINISH_EXIT_MS,
            KEY_FINISH_USE_FLASH_COLOR to DEFAULT_FINISH_USE_FLASH_COLOR,
            KEY_FINISH_FLASH_COLOR to DEFAULT_FINISH_FLASH_COLOR,
        )

    // Cached values
    @Volatile
    private var remotePrefs: SharedPreferences? = null

    @Volatile
    var enabled = DEFAULT_ENABLED
        private set

    @Volatile
    var color = DEFAULT_COLOR
        private set

    @Volatile
    var strokeWidth = DEFAULT_STROKE_WIDTH
        private set

    @Volatile
    var ringGap = DEFAULT_RING_GAP
        private set

    @Volatile
    var opacity = DEFAULT_OPACITY
        private set

    @Volatile
    var hooksFeedback = DEFAULT_HOOKS_FEEDBACK
        private set

    @Volatile
    var appVisible = false
        private set

    @Volatile
    var clockwise = true
        private set

    @Volatile
    var progressEasing = DEFAULT_PROGRESS_EASING
        private set

    @Volatile
    var errorColor = DEFAULT_ERROR_COLOR
        private set

    @Volatile
    var powerSaverMode = DEFAULT_POWER_SAVER_MODE
        private set

    @Volatile
    var idleRingEnabled = DEFAULT_IDLE_RING_ENABLED
        private set

    @Volatile
    var idleRingOpacity = DEFAULT_IDLE_RING_OPACITY
        private set

    @Volatile
    var showDownloadCount = DEFAULT_SHOW_DOWNLOAD_COUNT
        private set

    @Volatile
    var finishStyle = DEFAULT_FINISH_STYLE
        private set

    @Volatile
    var finishHoldMs = DEFAULT_FINISH_HOLD_MS
        private set

    @Volatile
    var finishExitMs = DEFAULT_FINISH_EXIT_MS
        private set

    @Volatile
    var finishUseFlashColor = DEFAULT_FINISH_USE_FLASH_COLOR
        private set

    @Volatile
    var finishFlashColor = DEFAULT_FINISH_FLASH_COLOR
        private set

    @Volatile
    var minVisibilityEnabled = DEFAULT_MIN_VISIBILITY_ENABLED
        private set

    @Volatile
    var minVisibilityMs = DEFAULT_MIN_VISIBILITY_MS
        private set

    @Volatile
    var completionPulseEnabled = DEFAULT_COMPLETION_PULSE_ENABLED
        private set

    @Volatile
    var percentTextEnabled = DEFAULT_PERCENT_TEXT_ENABLED
        private set

    @Volatile
    var percentTextPosition = DEFAULT_PERCENT_TEXT_POSITION
        private set

    @Volatile
    var filenameTextEnabled = DEFAULT_FILENAME_TEXT_ENABLED
        private set

    @Volatile
    var filenameTextPosition = DEFAULT_FILENAME_TEXT_POSITION
        private set

    // Callbacks
    var onPrefsChanged: (() -> Unit)? = null
    var onAppVisibilityChanged: ((Boolean) -> Unit)? = null
    var onTestProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onTestErrorChanged: ((Boolean) -> Unit)? = null
    var onPreviewTriggered: (() -> Unit)? = null
    var onGeometryPreviewTriggered: (() -> Unit)? = null

    fun init(xposed: io.github.libxposed.api.XposedInterface) {
        runCatching {
            remotePrefs = xposed.getRemotePreferences(PREFS_GROUP)
            refreshCache()

            // Listener lives for SystemUI process lifetime - cleanup via SystemUIHook.detach()
            remotePrefs?.registerOnSharedPreferenceChangeListener { prefs, key ->
                runCatching {
                    refreshCache()
                    when (key) {
                        KEY_APP_VISIBLE -> {
                            onAppVisibilityChanged?.invoke(appVisible)
                        }

                        KEY_TEST_PROGRESS -> {
                            val progress = prefs.getInt(KEY_TEST_PROGRESS, -1)
                            if (progress >= 0) {
                                onTestProgressChanged?.invoke(progress)
                                if (progress == 100) onDownloadComplete?.invoke()
                            }
                        }

                        KEY_TEST_ERROR -> {
                            onTestErrorChanged?.invoke(prefs.getBoolean(KEY_TEST_ERROR, false))
                        }

                        KEY_PREVIEW_TRIGGER -> {
                            onPreviewTriggered?.invoke()
                        }

                        in VISUAL_KEYS -> {
                            onPrefsChanged?.invoke()
                            onGeometryPreviewTriggered?.invoke()
                        }

                        else -> {
                            onPrefsChanged?.invoke()
                        }
                    }
                }.onFailure { log("Preference change handler failed", it) }
            }
            log("PrefsManager initialized")
        }.onFailure { log("PrefsManager.init() failed", it) }
    }

    // Cache refresh
    private fun refreshCache() {
        runCatching {
            remotePrefs?.let { prefs ->
                enabled = prefs.readEnabled()
                color = prefs.readColor()
                strokeWidth = prefs.readStrokeWidth()
                ringGap = prefs.readRingGap()
                opacity = prefs.readOpacity()
                hooksFeedback = prefs.readHooksFeedback()
                appVisible = prefs.readAppVisible()
                clockwise = prefs.readClockwise()
                progressEasing = prefs.readProgressEasing()
                errorColor = prefs.readErrorColor()
                powerSaverMode = prefs.readPowerSaverMode()
                idleRingEnabled = prefs.readIdleRingEnabled()
                idleRingOpacity = prefs.readIdleRingOpacity()
                showDownloadCount = prefs.readShowDownloadCount()
                finishStyle = prefs.readFinishStyle()
                finishHoldMs = prefs.readFinishHoldMs()
                finishExitMs = prefs.readFinishExitMs()
                finishUseFlashColor = prefs.readFinishUseFlashColor()
                finishFlashColor = prefs.readFinishFlashColor()
                minVisibilityEnabled = prefs.readMinVisibilityEnabled()
                minVisibilityMs = prefs.readMinVisibilityMs()
                completionPulseEnabled = prefs.readCompletionPulseEnabled()
                percentTextEnabled = prefs.readPercentTextEnabled()
                percentTextPosition = prefs.readPercentTextPosition()
                filenameTextEnabled = prefs.readFilenameTextEnabled()
                filenameTextPosition = prefs.readFilenameTextPosition()
            }
        }.onFailure { log("refreshCache() failed", it) }
    }
}
