package eu.hxreborn.phpm.ui.state

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import eu.hxreborn.phpm.prefs.PrefsManager

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
    val finishIntensity: String = PrefsManager.DEFAULT_FINISH_INTENSITY,
    val finishUseFlashColor: Boolean = PrefsManager.DEFAULT_FINISH_USE_FLASH_COLOR,
    val finishFlashColor: Int = PrefsManager.DEFAULT_FINISH_FLASH_COLOR,
    val minVisibilityEnabled: Boolean = PrefsManager.DEFAULT_MIN_VISIBILITY_ENABLED,
    val minVisibilityMs: Int = PrefsManager.DEFAULT_MIN_VISIBILITY_MS,
    val completionPulseEnabled: Boolean = PrefsManager.DEFAULT_COMPLETION_PULSE_ENABLED,
    val percentTextEnabled: Boolean = PrefsManager.DEFAULT_PERCENT_TEXT_ENABLED,
    val percentTextPosition: String = PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
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

private fun updatePrefsStateForKey(
    current: PrefsState,
    prefs: SharedPreferences,
    key: String?,
): PrefsState {
    if (key == null) return readPrefsState(prefs)
    return when (key) {
        PrefsManager.KEY_ENABLED -> {
            current.copy(enabled = prefs.getBoolean(PrefsManager.KEY_ENABLED, PrefsManager.DEFAULT_ENABLED))
        }

        PrefsManager.KEY_COLOR -> {
            current.copy(color = prefs.getInt(PrefsManager.KEY_COLOR, PrefsManager.DEFAULT_COLOR))
        }

        PrefsManager.KEY_STROKE_WIDTH -> {
            current.copy(
                strokeWidth =
                    prefs
                        .getFloat(PrefsManager.KEY_STROKE_WIDTH, PrefsManager.DEFAULT_STROKE_WIDTH)
                        .coerceIn(PrefsManager.MIN_STROKE_WIDTH, PrefsManager.MAX_STROKE_WIDTH),
            )
        }

        PrefsManager.KEY_RING_GAP -> {
            current.copy(
                ringGap =
                    prefs
                        .getFloat(PrefsManager.KEY_RING_GAP, PrefsManager.DEFAULT_RING_GAP)
                        .coerceIn(PrefsManager.MIN_RING_GAP, PrefsManager.MAX_RING_GAP),
            )
        }

        PrefsManager.KEY_OPACITY -> {
            current.copy(
                opacity =
                    prefs
                        .getInt(PrefsManager.KEY_OPACITY, PrefsManager.DEFAULT_OPACITY)
                        .coerceIn(PrefsManager.MIN_OPACITY, PrefsManager.MAX_OPACITY),
            )
        }

        PrefsManager.KEY_HOOKS_FEEDBACK -> {
            current.copy(
                hooksFeedback =
                    prefs.getBoolean(
                        PrefsManager.KEY_HOOKS_FEEDBACK,
                        PrefsManager.DEFAULT_HOOKS_FEEDBACK,
                    ),
            )
        }

        PrefsManager.KEY_CLOCKWISE -> {
            current.copy(clockwise = prefs.getBoolean(PrefsManager.KEY_CLOCKWISE, true))
        }

        PrefsManager.KEY_PROGRESS_EASING -> {
            current.copy(
                progressEasing =
                    prefs.getString(
                        PrefsManager.KEY_PROGRESS_EASING,
                        PrefsManager.DEFAULT_PROGRESS_EASING,
                    ) ?: PrefsManager.DEFAULT_PROGRESS_EASING,
            )
        }

        PrefsManager.KEY_ERROR_COLOR -> {
            current.copy(errorColor = prefs.getInt(PrefsManager.KEY_ERROR_COLOR, PrefsManager.DEFAULT_ERROR_COLOR))
        }

        PrefsManager.KEY_POWER_SAVER_MODE -> {
            current.copy(
                powerSaverMode =
                    prefs.getString(
                        PrefsManager.KEY_POWER_SAVER_MODE,
                        PrefsManager.DEFAULT_POWER_SAVER_MODE,
                    ) ?: PrefsManager.DEFAULT_POWER_SAVER_MODE,
            )
        }

        PrefsManager.KEY_IDLE_RING_ENABLED -> {
            current.copy(
                idleRingEnabled =
                    prefs.getBoolean(
                        PrefsManager.KEY_IDLE_RING_ENABLED,
                        PrefsManager.DEFAULT_IDLE_RING_ENABLED,
                    ),
            )
        }

        PrefsManager.KEY_IDLE_RING_OPACITY -> {
            current.copy(
                idleRingOpacity =
                    prefs
                        .getInt(PrefsManager.KEY_IDLE_RING_OPACITY, PrefsManager.DEFAULT_IDLE_RING_OPACITY)
                        .coerceIn(0, 100),
            )
        }

        PrefsManager.KEY_SHOW_DOWNLOAD_COUNT -> {
            current.copy(
                showDownloadCount =
                    prefs.getBoolean(
                        PrefsManager.KEY_SHOW_DOWNLOAD_COUNT,
                        PrefsManager.DEFAULT_SHOW_DOWNLOAD_COUNT,
                    ),
            )
        }

        PrefsManager.KEY_FINISH_STYLE -> {
            current.copy(
                finishStyle =
                    prefs.getString(
                        PrefsManager.KEY_FINISH_STYLE,
                        PrefsManager.DEFAULT_FINISH_STYLE,
                    ) ?: PrefsManager.DEFAULT_FINISH_STYLE,
            )
        }

        PrefsManager.KEY_FINISH_HOLD_MS -> {
            current.copy(
                finishHoldMs =
                    prefs
                        .getInt(PrefsManager.KEY_FINISH_HOLD_MS, PrefsManager.DEFAULT_FINISH_HOLD_MS)
                        .coerceIn(PrefsManager.MIN_FINISH_HOLD_MS, PrefsManager.MAX_FINISH_HOLD_MS),
            )
        }

        PrefsManager.KEY_FINISH_EXIT_MS -> {
            current.copy(
                finishExitMs =
                    prefs
                        .getInt(PrefsManager.KEY_FINISH_EXIT_MS, PrefsManager.DEFAULT_FINISH_EXIT_MS)
                        .coerceIn(PrefsManager.MIN_FINISH_EXIT_MS, PrefsManager.MAX_FINISH_EXIT_MS),
            )
        }

        PrefsManager.KEY_FINISH_INTENSITY -> {
            current.copy(
                finishIntensity =
                    prefs.getString(
                        PrefsManager.KEY_FINISH_INTENSITY,
                        PrefsManager.DEFAULT_FINISH_INTENSITY,
                    ) ?: PrefsManager.DEFAULT_FINISH_INTENSITY,
            )
        }

        PrefsManager.KEY_FINISH_USE_FLASH_COLOR -> {
            current.copy(
                finishUseFlashColor =
                    prefs.getBoolean(
                        PrefsManager.KEY_FINISH_USE_FLASH_COLOR,
                        PrefsManager.DEFAULT_FINISH_USE_FLASH_COLOR,
                    ),
            )
        }

        PrefsManager.KEY_FINISH_FLASH_COLOR -> {
            current.copy(
                finishFlashColor =
                    prefs.getInt(
                        PrefsManager.KEY_FINISH_FLASH_COLOR,
                        PrefsManager.DEFAULT_FINISH_FLASH_COLOR,
                    ),
            )
        }

        PrefsManager.KEY_MIN_VISIBILITY_ENABLED -> {
            current.copy(
                minVisibilityEnabled =
                    prefs.getBoolean(
                        PrefsManager.KEY_MIN_VISIBILITY_ENABLED,
                        PrefsManager.DEFAULT_MIN_VISIBILITY_ENABLED,
                    ),
            )
        }

        PrefsManager.KEY_MIN_VISIBILITY_MS -> {
            current.copy(
                minVisibilityMs =
                    prefs
                        .getInt(PrefsManager.KEY_MIN_VISIBILITY_MS, PrefsManager.DEFAULT_MIN_VISIBILITY_MS)
                        .coerceIn(PrefsManager.MIN_MIN_VISIBILITY_MS, PrefsManager.MAX_MIN_VISIBILITY_MS),
            )
        }

        PrefsManager.KEY_COMPLETION_PULSE_ENABLED -> {
            current.copy(
                completionPulseEnabled =
                    prefs.getBoolean(
                        PrefsManager.KEY_COMPLETION_PULSE_ENABLED,
                        PrefsManager.DEFAULT_COMPLETION_PULSE_ENABLED,
                    ),
            )
        }

        PrefsManager.KEY_PERCENT_TEXT_ENABLED -> {
            current.copy(
                percentTextEnabled =
                    prefs.getBoolean(
                        PrefsManager.KEY_PERCENT_TEXT_ENABLED,
                        PrefsManager.DEFAULT_PERCENT_TEXT_ENABLED,
                    ),
            )
        }

        PrefsManager.KEY_PERCENT_TEXT_POSITION -> {
            current.copy(
                percentTextPosition =
                    prefs.getString(
                        PrefsManager.KEY_PERCENT_TEXT_POSITION,
                        PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
                    ) ?: PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
            )
        }

        else -> {
            readPrefsState(prefs)
        }
    }
}

private fun readPrefsState(prefs: SharedPreferences): PrefsState =
    PrefsState(
        enabled = prefs.getBoolean(PrefsManager.KEY_ENABLED, PrefsManager.DEFAULT_ENABLED),
        color = prefs.getInt(PrefsManager.KEY_COLOR, PrefsManager.DEFAULT_COLOR),
        strokeWidth =
            prefs
                .getFloat(PrefsManager.KEY_STROKE_WIDTH, PrefsManager.DEFAULT_STROKE_WIDTH)
                .coerceIn(PrefsManager.MIN_STROKE_WIDTH, PrefsManager.MAX_STROKE_WIDTH),
        ringGap =
            prefs
                .getFloat(PrefsManager.KEY_RING_GAP, PrefsManager.DEFAULT_RING_GAP)
                .coerceIn(PrefsManager.MIN_RING_GAP, PrefsManager.MAX_RING_GAP),
        opacity =
            prefs
                .getInt(PrefsManager.KEY_OPACITY, PrefsManager.DEFAULT_OPACITY)
                .coerceIn(PrefsManager.MIN_OPACITY, PrefsManager.MAX_OPACITY),
        hooksFeedback =
            prefs.getBoolean(
                PrefsManager.KEY_HOOKS_FEEDBACK,
                PrefsManager.DEFAULT_HOOKS_FEEDBACK,
            ),
        clockwise = prefs.getBoolean(PrefsManager.KEY_CLOCKWISE, true),
        progressEasing =
            prefs.getString(PrefsManager.KEY_PROGRESS_EASING, PrefsManager.DEFAULT_PROGRESS_EASING)
                ?: PrefsManager.DEFAULT_PROGRESS_EASING,
        errorColor = prefs.getInt(PrefsManager.KEY_ERROR_COLOR, PrefsManager.DEFAULT_ERROR_COLOR),
        powerSaverMode =
            prefs.getString(
                PrefsManager.KEY_POWER_SAVER_MODE,
                PrefsManager.DEFAULT_POWER_SAVER_MODE,
            )
                ?: PrefsManager.DEFAULT_POWER_SAVER_MODE,
        idleRingEnabled =
            prefs.getBoolean(
                PrefsManager.KEY_IDLE_RING_ENABLED,
                PrefsManager.DEFAULT_IDLE_RING_ENABLED,
            ),
        idleRingOpacity =
            prefs
                .getInt(PrefsManager.KEY_IDLE_RING_OPACITY, PrefsManager.DEFAULT_IDLE_RING_OPACITY)
                .coerceIn(0, 100),
        showDownloadCount =
            prefs.getBoolean(
                PrefsManager.KEY_SHOW_DOWNLOAD_COUNT,
                PrefsManager.DEFAULT_SHOW_DOWNLOAD_COUNT,
            ),
        finishStyle =
            prefs.getString(PrefsManager.KEY_FINISH_STYLE, PrefsManager.DEFAULT_FINISH_STYLE)
                ?: PrefsManager.DEFAULT_FINISH_STYLE,
        finishHoldMs =
            prefs
                .getInt(PrefsManager.KEY_FINISH_HOLD_MS, PrefsManager.DEFAULT_FINISH_HOLD_MS)
                .coerceIn(PrefsManager.MIN_FINISH_HOLD_MS, PrefsManager.MAX_FINISH_HOLD_MS),
        finishExitMs =
            prefs
                .getInt(PrefsManager.KEY_FINISH_EXIT_MS, PrefsManager.DEFAULT_FINISH_EXIT_MS)
                .coerceIn(PrefsManager.MIN_FINISH_EXIT_MS, PrefsManager.MAX_FINISH_EXIT_MS),
        finishIntensity =
            prefs.getString(
                PrefsManager.KEY_FINISH_INTENSITY,
                PrefsManager.DEFAULT_FINISH_INTENSITY,
            )
                ?: PrefsManager.DEFAULT_FINISH_INTENSITY,
        finishUseFlashColor =
            prefs.getBoolean(
                PrefsManager.KEY_FINISH_USE_FLASH_COLOR,
                PrefsManager.DEFAULT_FINISH_USE_FLASH_COLOR,
            ),
        finishFlashColor =
            prefs.getInt(
                PrefsManager.KEY_FINISH_FLASH_COLOR,
                PrefsManager.DEFAULT_FINISH_FLASH_COLOR,
            ),
        minVisibilityEnabled =
            prefs.getBoolean(
                PrefsManager.KEY_MIN_VISIBILITY_ENABLED,
                PrefsManager.DEFAULT_MIN_VISIBILITY_ENABLED,
            ),
        minVisibilityMs =
            prefs
                .getInt(PrefsManager.KEY_MIN_VISIBILITY_MS, PrefsManager.DEFAULT_MIN_VISIBILITY_MS)
                .coerceIn(PrefsManager.MIN_MIN_VISIBILITY_MS, PrefsManager.MAX_MIN_VISIBILITY_MS),
        completionPulseEnabled =
            prefs.getBoolean(
                PrefsManager.KEY_COMPLETION_PULSE_ENABLED,
                PrefsManager.DEFAULT_COMPLETION_PULSE_ENABLED,
            ),
        percentTextEnabled =
            prefs.getBoolean(
                PrefsManager.KEY_PERCENT_TEXT_ENABLED,
                PrefsManager.DEFAULT_PERCENT_TEXT_ENABLED,
            ),
        percentTextPosition =
            prefs.getString(
                PrefsManager.KEY_PERCENT_TEXT_POSITION,
                PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
            )
                ?: PrefsManager.DEFAULT_PERCENT_TEXT_POSITION,
    )
