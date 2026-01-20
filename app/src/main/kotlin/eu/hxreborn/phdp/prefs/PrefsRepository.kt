package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.phdp.ui.state.PrefsState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface PrefsRepository {
    val state: Flow<PrefsState>

    fun save(
        key: String,
        value: Any,
    )

    fun resetDefaults()
}

class PrefsRepositoryImpl(
    private val localPrefs: SharedPreferences,
    private val remotePrefsProvider: () -> SharedPreferences?,
) : PrefsRepository {
    override val state: Flow<PrefsState> =
        callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    trySend(localPrefs.toPrefsState())
                }
            trySend(localPrefs.toPrefsState())
            localPrefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { localPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    override fun save(
        key: String,
        value: Any,
    ) {
        localPrefs.edit { putAny(key, value) }
        remotePrefsProvider()?.edit(commit = true) { putAny(key, value) }
    }

    override fun resetDefaults() {
        localPrefs.edit { PrefsManager.DEFAULTS.forEach { (k, v) -> putAny(k, v) } }
        remotePrefsProvider()?.edit(commit = true) {
            PrefsManager.DEFAULTS.forEach { (k, v) ->
                putAny(k, v)
            }
        }
    }

    private fun SharedPreferences.toPrefsState(): PrefsState =
        PrefsState(
            enabled = readEnabled(),
            color = readColor(),
            strokeWidth = readStrokeWidth(),
            ringGap = readRingGap(),
            opacity = readOpacity(),
            hooksFeedback = readHooksFeedback(),
            clockwise = readClockwise(),
            progressEasing = readProgressEasing(),
            errorColor = readErrorColor(),
            powerSaverMode = readPowerSaverMode(),
            idleRingEnabled = readIdleRingEnabled(),
            idleRingOpacity = readIdleRingOpacity(),
            showDownloadCount = readShowDownloadCount(),
            finishStyle = readFinishStyle(),
            finishHoldMs = readFinishHoldMs(),
            finishExitMs = readFinishExitMs(),
            finishUseFlashColor = readFinishUseFlashColor(),
            finishFlashColor = readFinishFlashColor(),
            minVisibilityEnabled = readMinVisibilityEnabled(),
            minVisibilityMs = readMinVisibilityMs(),
            completionPulseEnabled = readCompletionPulseEnabled(),
            percentTextEnabled = readPercentTextEnabled(),
            percentTextPosition = readPercentTextPosition(),
            filenameTextEnabled = readFilenameTextEnabled(),
            filenameTextPosition = readFilenameTextPosition(),
        )
}
