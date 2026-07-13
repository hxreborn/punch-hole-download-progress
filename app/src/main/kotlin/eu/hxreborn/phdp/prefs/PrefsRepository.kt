package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.hxreborn.phdp.ui.state.AppPrefs
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface PrefsRepository {
    val state: Flow<AppPrefs>

    fun <T : Any> save(
        pref: PrefSpec<T>,
        value: T,
    )

    fun resetDefaults()

    fun syncToRemote()
}

class PrefsRepositoryImpl(
    private val localPrefs: SharedPreferences,
    private val remotePrefsProvider: () -> SharedPreferences?,
) : PrefsRepository {
    override val state: Flow<AppPrefs> =
        callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    trySend(localPrefs.toAppPrefs())
                }
            trySend(localPrefs.toAppPrefs())
            localPrefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { localPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    override fun <T : Any> save(
        pref: PrefSpec<T>,
        value: T,
    ) {
        localPrefs.edit { pref.write(this, value) }
        remotePrefsProvider()?.edit(commit = true) { pref.write(this, value) }
    }

    override fun resetDefaults() {
        localPrefs.edit { Prefs.resettable.forEach { it.reset(this) } }
        remotePrefsProvider()?.edit(commit = true) { Prefs.resettable.forEach { it.reset(this) } }
    }

    override fun syncToRemote() {
        val remote = remotePrefsProvider() ?: return
        val snapshot = localPrefs.all
        runCatching {
            remote.edit(commit = true) {
                for ((key, value) in snapshot) {
                    when (value) {
                        is Boolean -> {
                            putBoolean(key, value)
                        }

                        is Int -> {
                            putInt(key, value)
                        }

                        is Long -> {
                            putLong(key, value)
                        }

                        is Float -> {
                            putFloat(key, value)
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
            }
        }
    }

    private fun SharedPreferences.toAppPrefs(): AppPrefs =
        AppPrefs(
            enabled = Prefs.enabled.read(this),
            color = Prefs.color.read(this),
            gradientEnabled = Prefs.gradientEnabled.read(this),
            gradientStartColor = Prefs.gradientStartColor.read(this),
            gradientEndColor = Prefs.gradientEndColor.read(this),
            gradientDirection =
                GradientDirection
                    .fromStoredValue(Prefs.gradientDirection.read(this))
                    .storedValue,
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
            glowEnabled = Prefs.glowEnabled.read(this),
            glowRadius = Prefs.glowRadius.read(this),
            hdrEnabled = Prefs.hdrEnabled.read(this),
            hdrHeadroom = Prefs.hdrHeadroom.read(this),
            percentTextShadowMode = Prefs.percentTextShadowMode.read(this),
            percentTextShadowColor = Prefs.percentTextShadowColor.read(this),
            percentTextShadowRadius = Prefs.percentTextShadowRadius.read(this),
            percentTextShadowDy = Prefs.percentTextShadowDy.read(this),
            percentTextShadowOpacity = Prefs.percentTextShadowOpacity.read(this),
            percentTextStrokeWidth = Prefs.percentTextStrokeWidth.read(this),
            percentTextStrokeColor = Prefs.percentTextStrokeColor.read(this),
            filenameTextShadowMode = Prefs.filenameTextShadowMode.read(this),
            filenameTextShadowColor = Prefs.filenameTextShadowColor.read(this),
            filenameTextShadowRadius = Prefs.filenameTextShadowRadius.read(this),
            filenameTextShadowDy = Prefs.filenameTextShadowDy.read(this),
            filenameTextShadowOpacity = Prefs.filenameTextShadowOpacity.read(this),
            filenameTextStrokeWidth = Prefs.filenameTextStrokeWidth.read(this),
            filenameTextStrokeColor = Prefs.filenameTextStrokeColor.read(this),
            percentTextLockRotation = Prefs.percentTextLockRotation.read(this),
            filenameTextLockRotation = Prefs.filenameTextLockRotation.read(this),
            appIconLockRotation = Prefs.appIconLockRotation.read(this),
            badgeLockRotation = Prefs.badgeLockRotation.read(this),
            powerSaverMode = Prefs.powerSaverMode.read(this),
            showDownloadCount = Prefs.showDownloadCount.read(this),
            badgeOffsets = Prefs.badgeOffsets.read(this),
            badgeTextSize = Prefs.badgeTextSize.read(this),
            finishStyle = Prefs.finishStyle.read(this),
            finishHoldMs = Prefs.finishHoldMs.read(this),
            finishExitMs = Prefs.finishExitMs.read(this),
            finishUseFlashColor = Prefs.finishUseFlashColor.read(this),
            finishFlashColor = Prefs.finishFlashColor.read(this),
            minVisibilityEnabled = Prefs.minVisibilityEnabled.read(this),
            minVisibilityMs = Prefs.minVisibilityMs.read(this),
            effectSpeed = Prefs.effectSpeed.read(this),
            effectIntensity = Prefs.effectIntensity.read(this),
            effectReverse = Prefs.effectReverse.read(this),
            effectRepeat = Prefs.effectRepeat.read(this),
            segmentCount = Prefs.segmentCount.read(this),
            segmentGapDegrees = Prefs.segmentGapDegrees.read(this),
            materialYouEnabled = Prefs.materialYouEnabled.read(this),
            materialYouProgressPalette = Prefs.materialYouProgressPalette.read(this),
            materialYouProgressShade = Prefs.materialYouProgressShade.read(this),
            materialYouSuccessPalette = Prefs.materialYouSuccessPalette.read(this),
            materialYouSuccessShade = Prefs.materialYouSuccessShade.read(this),
            materialYouErrorPalette = Prefs.materialYouErrorPalette.read(this),
            materialYouErrorShade = Prefs.materialYouErrorShade.read(this),
            percentTextEnabled = Prefs.percentTextEnabled.read(this),
            percentTextPosition = Prefs.percentTextPosition.read(this),
            percentTextOffsets = Prefs.percentTextOffsets.read(this),
            percentTextSize = Prefs.percentTextSize.read(this),
            filenameTextEnabled = Prefs.filenameTextEnabled.read(this),
            filenameTextPosition = Prefs.filenameTextPosition.read(this),
            filenameTextOffsets = Prefs.filenameTextOffsets.read(this),
            filenameTextSize = Prefs.filenameTextSize.read(this),
            filenameMaxChars = Prefs.filenameMaxChars.read(this),
            filenameTruncateEnabled = Prefs.filenameTruncateEnabled.read(this),
            percentTextBold = Prefs.percentTextBold.read(this),
            percentTextItalic = Prefs.percentTextItalic.read(this),
            filenameTextBold = Prefs.filenameTextBold.read(this),
            filenameTextItalic = Prefs.filenameTextItalic.read(this),
            filenameEllipsize = Prefs.filenameEllipsize.read(this),
            filenameVerticalText = Prefs.filenameVerticalText.read(this),
            appIconEnabled = Prefs.appIconEnabled.read(this),
            appIconPosition = Prefs.appIconPosition.read(this),
            appIconSize = Prefs.appIconSize.read(this),
            appIconMonochrome = Prefs.appIconMonochrome.read(this),
            appIconOffsets = Prefs.appIconOffsets.read(this),
            previewFilenameText = Prefs.previewFilenameText.read(this),
            darkThemeConfig = readDarkThemeConfig(),
            useDynamicColor = Prefs.useDynamicColor.read(this),
            floatingNavBar = Prefs.floatingNavBar.read(this),
            hideNavBarOnScroll = Prefs.hideNavBarOnScroll.read(this),
            ringScaleX = Prefs.ringScaleX.read(this),
            ringScaleY = Prefs.ringScaleY.read(this),
            ringScaleLinked = Prefs.ringScaleLinked.read(this),
            ringOffsetX = Prefs.ringOffsetX.read(this),
            ringOffsetY = Prefs.ringOffsetY.read(this),
            pathMode = Prefs.pathMode.read(this),
            selectedPackages = Prefs.selectedPackages.read(this),
            showSystemPackages = Prefs.showSystemPackages.read(this),
            verboseLogging = Prefs.verboseLogging.read(this),
            burnInHideMs = Prefs.burnInHideMs.read(this),
            progressAnimMs = Prefs.progressAnimMs.read(this),
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
