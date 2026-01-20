package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences

// Boolean readers
fun SharedPreferences.readEnabled(): Boolean =
    getBoolean(PrefsManager.KEY_ENABLED, PrefsManager.DEFAULT_ENABLED)

fun SharedPreferences.readHooksFeedback(): Boolean =
    getBoolean(PrefsManager.KEY_HOOKS_FEEDBACK, PrefsManager.DEFAULT_HOOKS_FEEDBACK)

fun SharedPreferences.readAppVisible(): Boolean = getBoolean(PrefsManager.KEY_APP_VISIBLE, false)

fun SharedPreferences.readClockwise(): Boolean = getBoolean(PrefsManager.KEY_CLOCKWISE, true)

fun SharedPreferences.readIdleRingEnabled(): Boolean =
    getBoolean(PrefsManager.KEY_IDLE_RING_ENABLED, PrefsManager.DEFAULT_IDLE_RING_ENABLED)

fun SharedPreferences.readShowDownloadCount(): Boolean =
    getBoolean(PrefsManager.KEY_SHOW_DOWNLOAD_COUNT, PrefsManager.DEFAULT_SHOW_DOWNLOAD_COUNT)

fun SharedPreferences.readFinishUseFlashColor(): Boolean =
    getBoolean(PrefsManager.KEY_FINISH_USE_FLASH_COLOR, PrefsManager.DEFAULT_FINISH_USE_FLASH_COLOR)

fun SharedPreferences.readMinVisibilityEnabled(): Boolean =
    getBoolean(PrefsManager.KEY_MIN_VISIBILITY_ENABLED, PrefsManager.DEFAULT_MIN_VISIBILITY_ENABLED)

fun SharedPreferences.readCompletionPulseEnabled(): Boolean =
    getBoolean(
        PrefsManager.KEY_COMPLETION_PULSE_ENABLED,
        PrefsManager.DEFAULT_COMPLETION_PULSE_ENABLED,
    )

fun SharedPreferences.readPercentTextEnabled(): Boolean =
    getBoolean(PrefsManager.KEY_PERCENT_TEXT_ENABLED, PrefsManager.DEFAULT_PERCENT_TEXT_ENABLED)

fun SharedPreferences.readFilenameTextEnabled(): Boolean =
    getBoolean(PrefsManager.KEY_FILENAME_TEXT_ENABLED, PrefsManager.DEFAULT_FILENAME_TEXT_ENABLED)

// Int readers no coercion
fun SharedPreferences.readColor(): Int = getInt(PrefsManager.KEY_COLOR, PrefsManager.DEFAULT_COLOR)

fun SharedPreferences.readErrorColor(): Int =
    getInt(PrefsManager.KEY_ERROR_COLOR, PrefsManager.DEFAULT_ERROR_COLOR)

fun SharedPreferences.readFinishFlashColor(): Int =
    getInt(PrefsManager.KEY_FINISH_FLASH_COLOR, PrefsManager.DEFAULT_FINISH_FLASH_COLOR)

// Int readers with coercion
fun SharedPreferences.readOpacity(): Int =
    getInt(
        PrefsManager.KEY_OPACITY,
        PrefsManager.DEFAULT_OPACITY,
    ).coerceIn(PrefsManager.MIN_OPACITY, PrefsManager.MAX_OPACITY)

fun SharedPreferences.readIdleRingOpacity(): Int =
    getInt(PrefsManager.KEY_IDLE_RING_OPACITY, PrefsManager.DEFAULT_IDLE_RING_OPACITY).coerceIn(
        0,
        100,
    )

fun SharedPreferences.readFinishHoldMs(): Int =
    getInt(PrefsManager.KEY_FINISH_HOLD_MS, PrefsManager.DEFAULT_FINISH_HOLD_MS).coerceIn(
        PrefsManager.MIN_FINISH_HOLD_MS,
        PrefsManager.MAX_FINISH_HOLD_MS,
    )

fun SharedPreferences.readFinishExitMs(): Int =
    getInt(PrefsManager.KEY_FINISH_EXIT_MS, PrefsManager.DEFAULT_FINISH_EXIT_MS).coerceIn(
        PrefsManager.MIN_FINISH_EXIT_MS,
        PrefsManager.MAX_FINISH_EXIT_MS,
    )

fun SharedPreferences.readMinVisibilityMs(): Int =
    getInt(PrefsManager.KEY_MIN_VISIBILITY_MS, PrefsManager.DEFAULT_MIN_VISIBILITY_MS).coerceIn(
        PrefsManager.MIN_MIN_VISIBILITY_MS,
        PrefsManager.MAX_MIN_VISIBILITY_MS,
    )

// Float readers with coercion
fun SharedPreferences.readStrokeWidth(): Float =
    getFloat(
        PrefsManager.KEY_STROKE_WIDTH,
        PrefsManager.DEFAULT_STROKE_WIDTH,
    ).coerceIn(PrefsManager.MIN_STROKE_WIDTH, PrefsManager.MAX_STROKE_WIDTH)

fun SharedPreferences.readRingGap(): Float =
    getFloat(
        PrefsManager.KEY_RING_GAP,
        PrefsManager.DEFAULT_RING_GAP,
    ).coerceIn(PrefsManager.MIN_RING_GAP, PrefsManager.MAX_RING_GAP)

// String readers
fun SharedPreferences.readProgressEasing(): String =
    getString(PrefsManager.KEY_PROGRESS_EASING, PrefsManager.DEFAULT_PROGRESS_EASING)
        ?: PrefsManager.DEFAULT_PROGRESS_EASING

fun SharedPreferences.readPowerSaverMode(): String =
    getString(PrefsManager.KEY_POWER_SAVER_MODE, PrefsManager.DEFAULT_POWER_SAVER_MODE)
        ?: PrefsManager.DEFAULT_POWER_SAVER_MODE

fun SharedPreferences.readFinishStyle(): String =
    getString(PrefsManager.KEY_FINISH_STYLE, PrefsManager.DEFAULT_FINISH_STYLE)
        ?: PrefsManager.DEFAULT_FINISH_STYLE

fun SharedPreferences.readPercentTextPosition(): String =
    getString(PrefsManager.KEY_PERCENT_TEXT_POSITION, PrefsManager.DEFAULT_PERCENT_TEXT_POSITION)
        ?: PrefsManager.DEFAULT_PERCENT_TEXT_POSITION

fun SharedPreferences.readFilenameTextPosition(): String =
    getString(PrefsManager.KEY_FILENAME_TEXT_POSITION, PrefsManager.DEFAULT_FILENAME_TEXT_POSITION)
        ?: PrefsManager.DEFAULT_FILENAME_TEXT_POSITION

// Writer helper
fun SharedPreferences.Editor.putAny(
    key: String,
    value: Any,
): SharedPreferences.Editor =
    apply {
        when (value) {
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            is String -> putString(key, value)
        }
    }
