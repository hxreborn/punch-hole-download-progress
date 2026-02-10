package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
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
        localPrefs.edit { Prefs.resettable.forEach { it.reset(this) } }
        remotePrefsProvider()?.edit(commit = true) { Prefs.resettable.forEach { it.reset(this) } }
    }

    private fun SharedPreferences.toPrefsState(): PrefsState =
        PrefsState(
            enabled = Prefs.enabled.read(this),
            color = Prefs.color.read(this),
            strokeWidth = Prefs.strokeWidth.read(this),
            ringGap = Prefs.ringGap.read(this),
            opacity = Prefs.opacity.read(this),
            hooksFeedback = Prefs.hooksFeedback.read(this),
            clockwise = Prefs.clockwise.read(this),
            progressEasing = Prefs.progressEasing.read(this),
            errorColor = Prefs.errorColor.read(this),
            strokeCapStyle = Prefs.strokeCapStyle.read(this),
            backgroundRingEnabled = Prefs.backgroundRingEnabled.read(this),
            backgroundRingColor = Prefs.backgroundRingColor.read(this),
            backgroundRingOpacity = Prefs.backgroundRingOpacity.read(this),
            powerSaverMode = Prefs.powerSaverMode.read(this),
            showDownloadCount = Prefs.showDownloadCount.read(this),
            badgeOffsetX = Prefs.badgeOffsetX.read(this),
            badgeOffsetY = Prefs.badgeOffsetY.read(this),
            badgeTextSize = Prefs.badgeTextSize.read(this),
            finishStyle = Prefs.finishStyle.read(this),
            finishHoldMs = Prefs.finishHoldMs.read(this),
            finishExitMs = Prefs.finishExitMs.read(this),
            finishUseFlashColor = Prefs.finishUseFlashColor.read(this),
            finishFlashColor = Prefs.finishFlashColor.read(this),
            minVisibilityEnabled = Prefs.minVisibilityEnabled.read(this),
            minVisibilityMs = Prefs.minVisibilityMs.read(this),
            completionPulseEnabled = Prefs.completionPulseEnabled.read(this),
            percentTextEnabled = Prefs.percentTextEnabled.read(this),
            percentTextPosition = Prefs.percentTextPosition.read(this),
            percentTextOffsetX = Prefs.percentTextOffsetX.read(this),
            percentTextOffsetY = Prefs.percentTextOffsetY.read(this),
            percentTextSize = Prefs.percentTextSize.read(this),
            filenameTextEnabled = Prefs.filenameTextEnabled.read(this),
            filenameTextPosition = Prefs.filenameTextPosition.read(this),
            filenameTextOffsetX = Prefs.filenameTextOffsetX.read(this),
            filenameTextOffsetY = Prefs.filenameTextOffsetY.read(this),
            filenameTextSize = Prefs.filenameTextSize.read(this),
            filenameMaxChars = Prefs.filenameMaxChars.read(this),
            filenameTruncateEnabled = Prefs.filenameTruncateEnabled.read(this),
            percentTextBold = Prefs.percentTextBold.read(this),
            percentTextItalic = Prefs.percentTextItalic.read(this),
            filenameTextBold = Prefs.filenameTextBold.read(this),
            filenameTextItalic = Prefs.filenameTextItalic.read(this),
            filenameEllipsize = Prefs.filenameEllipsize.read(this),
            previewFilenameText = Prefs.previewFilenameText.read(this),
            darkThemeConfig = readDarkThemeConfig(),
            useDynamicColor = Prefs.useDynamicColor.read(this),
            ringScaleX = Prefs.ringScaleX.read(this),
            ringScaleY = Prefs.ringScaleY.read(this),
            ringScaleLinked = Prefs.ringScaleLinked.read(this),
            ringOffsetX = Prefs.ringOffsetX.read(this),
            ringOffsetY = Prefs.ringOffsetY.read(this),
            selectedPackages = Prefs.selectedPackages.read(this),
            showSystemPackages = Prefs.showSystemPackages.read(this),
        )

    private fun SharedPreferences.readDarkThemeConfig(): DarkThemeConfig {
        val value = Prefs.darkThemeConfig.read(this)
        return when (value) {
            "light" -> DarkThemeConfig.LIGHT
            "dark" -> DarkThemeConfig.DARK
            else -> DarkThemeConfig.FOLLOW_SYSTEM
        }
    }
}

private fun SharedPreferences.Editor.putAny(
    key: String,
    value: Any,
): SharedPreferences.Editor =
    apply {
        when (value) {
            is Int -> {
                putInt(key, value)
            }

            is Long -> {
                putLong(key, value)
            }

            is Float -> {
                putFloat(key, value)
            }

            is Boolean -> {
                putBoolean(key, value)
            }

            is String -> {
                putString(key, value)
            }

            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                putStringSet(key, value as Set<String>)
            }
        }
    }
