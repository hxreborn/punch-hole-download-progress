package eu.hxreborn.phdp.ui.state

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.prefs.readClockwise
import eu.hxreborn.phdp.prefs.readColor
import eu.hxreborn.phdp.prefs.readCompletionPulseEnabled
import eu.hxreborn.phdp.prefs.readEnabled
import eu.hxreborn.phdp.prefs.readErrorColor
import eu.hxreborn.phdp.prefs.readFilenameTextEnabled
import eu.hxreborn.phdp.prefs.readFilenameTextPosition
import eu.hxreborn.phdp.prefs.readFinishExitMs
import eu.hxreborn.phdp.prefs.readFinishFlashColor
import eu.hxreborn.phdp.prefs.readFinishHoldMs
import eu.hxreborn.phdp.prefs.readFinishStyle
import eu.hxreborn.phdp.prefs.readFinishUseFlashColor
import eu.hxreborn.phdp.prefs.readHooksFeedback
import eu.hxreborn.phdp.prefs.readIdleRingEnabled
import eu.hxreborn.phdp.prefs.readIdleRingOpacity
import eu.hxreborn.phdp.prefs.readMinVisibilityEnabled
import eu.hxreborn.phdp.prefs.readMinVisibilityMs
import eu.hxreborn.phdp.prefs.readOpacity
import eu.hxreborn.phdp.prefs.readPercentTextEnabled
import eu.hxreborn.phdp.prefs.readPercentTextPosition
import eu.hxreborn.phdp.prefs.readPowerSaverMode
import eu.hxreborn.phdp.prefs.readProgressEasing
import eu.hxreborn.phdp.prefs.readRingGap
import eu.hxreborn.phdp.prefs.readShowDownloadCount
import eu.hxreborn.phdp.prefs.readStrokeWidth

data class PrefsState(
    val enabled: Boolean = PrefsManager.DEFAULT_ENABLED,
    val color: Int = PrefsManager.DEFAULT_COLOR,
    val strokeWidth: Float = PrefsManager.DEFAULT_STROKE_WIDTH,
    val ringGap: Float = PrefsManager.DEFAULT_RING_GAP,
    val opacity: Int = PrefsManager.DEFAULT_OPACITY,
    val hooksFeedback: Boolean = PrefsManager.DEFAULT_HOOKS_FEEDBACK,
    val clockwise: Boolean = true,
    val progressEasing: String = PrefsManager.DEFAULT_PROGRESS_EASING,
    val errorColor: Int = PrefsManager.DEFAULT_ERROR_COLOR,
    val powerSaverMode: String = PrefsManager.DEFAULT_POWER_SAVER_MODE,
    val idleRingEnabled: Boolean = PrefsManager.DEFAULT_IDLE_RING_ENABLED,
    val idleRingOpacity: Int = PrefsManager.DEFAULT_IDLE_RING_OPACITY,
    val showDownloadCount: Boolean = PrefsManager.DEFAULT_SHOW_DOWNLOAD_COUNT,
    val finishStyle: String = PrefsManager.DEFAULT_FINISH_STYLE,
    val finishHoldMs: Int = PrefsManager.DEFAULT_FINISH_HOLD_MS,
    val finishExitMs: Int = PrefsManager.DEFAULT_FINISH_EXIT_MS,
    val finishUseFlashColor: Boolean = PrefsManager.DEFAULT_FINISH_USE_FLASH_COLOR,
    val finishFlashColor: Int = PrefsManager.DEFAULT_FINISH_FLASH_COLOR,
    val minVisibilityEnabled: Boolean = PrefsManager.DEFAULT_MIN_VISIBILITY_ENABLED,
    val minVisibilityMs: Int = PrefsManager.DEFAULT_MIN_VISIBILITY_MS,
    val completionPulseEnabled: Boolean = PrefsManager.DEFAULT_COMPLETION_PULSE_ENABLED,
    val percentTextEnabled: Boolean = PrefsManager.DEFAULT_PERCENT_TEXT_ENABLED,
    val percentTextPosition: String = PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
    val filenameTextEnabled: Boolean = PrefsManager.DEFAULT_FILENAME_TEXT_ENABLED,
    val filenameTextPosition: String = PrefsManager.DEFAULT_FILENAME_TEXT_POSITION,
)

@Composable
fun rememberPrefsState(prefs: SharedPreferences): MutableState<PrefsState> {
    val state =
        remember {
            mutableStateOf(readPrefsState(prefs))
        }

    DisposableEffect(prefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                state.value = updatePrefsStateForKey(state.value, prefs, key)
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return state
}

@Suppress("UNUSED_PARAMETER")
private fun updatePrefsStateForKey(
    current: PrefsState,
    prefs: SharedPreferences,
    key: String?,
): PrefsState = readPrefsState(prefs)

private fun readPrefsState(prefs: SharedPreferences): PrefsState =
    PrefsState(
        enabled = prefs.readEnabled(),
        color = prefs.readColor(),
        strokeWidth = prefs.readStrokeWidth(),
        ringGap = prefs.readRingGap(),
        opacity = prefs.readOpacity(),
        hooksFeedback = prefs.readHooksFeedback(),
        clockwise = prefs.readClockwise(),
        progressEasing = prefs.readProgressEasing(),
        errorColor = prefs.readErrorColor(),
        powerSaverMode = prefs.readPowerSaverMode(),
        idleRingEnabled = prefs.readIdleRingEnabled(),
        idleRingOpacity = prefs.readIdleRingOpacity(),
        showDownloadCount = prefs.readShowDownloadCount(),
        finishStyle = prefs.readFinishStyle(),
        finishHoldMs = prefs.readFinishHoldMs(),
        finishExitMs = prefs.readFinishExitMs(),
        finishUseFlashColor = prefs.readFinishUseFlashColor(),
        finishFlashColor = prefs.readFinishFlashColor(),
        minVisibilityEnabled = prefs.readMinVisibilityEnabled(),
        minVisibilityMs = prefs.readMinVisibilityMs(),
        completionPulseEnabled = prefs.readCompletionPulseEnabled(),
        percentTextEnabled = prefs.readPercentTextEnabled(),
        percentTextPosition = prefs.readPercentTextPosition(),
        filenameTextEnabled = prefs.readFilenameTextEnabled(),
        filenameTextPosition = prefs.readFilenameTextPosition(),
    )
