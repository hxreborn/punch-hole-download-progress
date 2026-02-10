package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log

object PrefsManager {
    @Volatile
    private var remotePrefs: SharedPreferences? = null

    // Cached values
    @Volatile
    var enabled = Prefs.enabled.default
        private set

    @Volatile
    var color = Prefs.color.default
        private set

    @Volatile
    var strokeWidth = Prefs.strokeWidth.default
        private set

    @Volatile
    var ringGap = Prefs.ringGap.default
        private set

    @Volatile
    var opacity = Prefs.opacity.default
        private set

    @Volatile
    var hooksFeedback = Prefs.hooksFeedback.default
        private set

    @Volatile
    var appVisible = Prefs.appVisible.default
        private set

    @Volatile
    var clockwise = Prefs.clockwise.default
        private set

    @Volatile
    var progressEasing = Prefs.progressEasing.default
        private set

    @Volatile
    var errorColor = Prefs.errorColor.default
        private set

    @Volatile
    var powerSaverMode = Prefs.powerSaverMode.default
        private set

    @Volatile
    var showDownloadCount = Prefs.showDownloadCount.default
        private set

    @Volatile
    var badgeOffsetX = Prefs.badgeOffsetX.default
        private set

    @Volatile
    var badgeOffsetY = Prefs.badgeOffsetY.default
        private set

    @Volatile
    var badgeTextSize = Prefs.badgeTextSize.default
        private set

    @Volatile
    var finishStyle = Prefs.finishStyle.default
        private set

    @Volatile
    var finishHoldMs = Prefs.finishHoldMs.default
        private set

    @Volatile
    var finishExitMs = Prefs.finishExitMs.default
        private set

    @Volatile
    var finishUseFlashColor = Prefs.finishUseFlashColor.default
        private set

    @Volatile
    var finishFlashColor = Prefs.finishFlashColor.default
        private set

    @Volatile
    var minVisibilityEnabled = Prefs.minVisibilityEnabled.default
        private set

    @Volatile
    var minVisibilityMs = Prefs.minVisibilityMs.default
        private set

    @Volatile
    var completionPulseEnabled = Prefs.completionPulseEnabled.default
        private set

    @Volatile
    var percentTextEnabled = Prefs.percentTextEnabled.default
        private set

    @Volatile
    var percentTextPosition = Prefs.percentTextPosition.default
        private set

    @Volatile
    var percentTextOffsetX = Prefs.percentTextOffsetX.default
        private set

    @Volatile
    var percentTextOffsetY = Prefs.percentTextOffsetY.default
        private set

    @Volatile
    var percentTextSize = Prefs.percentTextSize.default
        private set

    @Volatile
    var filenameTextEnabled = Prefs.filenameTextEnabled.default
        private set

    @Volatile
    var filenameTextPosition = Prefs.filenameTextPosition.default
        private set

    @Volatile
    var filenameTextOffsetX = Prefs.filenameTextOffsetX.default
        private set

    @Volatile
    var filenameTextOffsetY = Prefs.filenameTextOffsetY.default
        private set

    @Volatile
    var filenameTextSize = Prefs.filenameTextSize.default
        private set

    @Volatile
    var filenameMaxChars = Prefs.filenameMaxChars.default
        private set

    @Volatile
    var filenameTruncateEnabled = Prefs.filenameTruncateEnabled.default
        private set

    @Volatile
    var percentTextBold = Prefs.percentTextBold.default
        private set

    @Volatile
    var percentTextItalic = Prefs.percentTextItalic.default
        private set

    @Volatile
    var filenameTextBold = Prefs.filenameTextBold.default
        private set

    @Volatile
    var filenameTextItalic = Prefs.filenameTextItalic.default
        private set

    @Volatile
    var filenameEllipsize = Prefs.filenameEllipsize.default
        private set

    @Volatile
    var previewFilenameText = Prefs.previewFilenameText.default
        private set

    @Volatile
    var ringScaleX = Prefs.ringScaleX.default
        private set

    @Volatile
    var ringScaleY = Prefs.ringScaleY.default
        private set

    @Volatile
    var ringScaleLinked = Prefs.ringScaleLinked.default
        private set

    @Volatile
    var ringOffsetX = Prefs.ringOffsetX.default
        private set

    @Volatile
    var ringOffsetY = Prefs.ringOffsetY.default
        private set

    @Volatile
    var strokeCapStyle = Prefs.strokeCapStyle.default
        private set

    @Volatile
    var backgroundRingEnabled = Prefs.backgroundRingEnabled.default
        private set

    @Volatile
    var backgroundRingColor = Prefs.backgroundRingColor.default
        private set

    @Volatile
    var backgroundRingOpacity = Prefs.backgroundRingOpacity.default
        private set

    @Volatile
    var selectedPackages = Prefs.selectedPackages.default
        private set

    @Volatile
    var persistentPreviewActive = false
        private set

    // Callbacks
    var onPrefsChanged: (() -> Unit)? = null
    var onAppVisibilityChanged: ((Boolean) -> Unit)? = null
    var onTestProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onTestErrorChanged: ((Boolean) -> Unit)? = null
    var onPreviewTriggered: (() -> Unit)? = null
    var onGeometryPreviewTriggered: (() -> Unit)? = null
    var onClearDownloadsTriggered: (() -> Unit)? = null
    var onPersistentPreviewChanged: ((Boolean) -> Unit)? = null

    fun init(xposed: io.github.libxposed.api.XposedInterface) {
        runCatching {
            remotePrefs = xposed.getRemotePreferences(Prefs.GROUP)
            refreshCache()
            log("Package selection: ${selectedPackages.size} packages")

            remotePrefs?.registerOnSharedPreferenceChangeListener { prefs, key ->
                runCatching {
                    refreshCache()
                    when (key) {
                        Prefs.appVisible.key -> {
                            onAppVisibilityChanged?.invoke(appVisible)
                        }

                        Prefs.testProgress.key -> {
                            val progress = Prefs.testProgress.read(prefs)
                            if (progress >= 0) {
                                onTestProgressChanged?.invoke(progress)
                                if (progress == 100) onDownloadComplete?.invoke()
                            }
                        }

                        Prefs.testError.key -> {
                            onTestErrorChanged?.invoke(Prefs.testError.read(prefs))
                        }

                        Prefs.previewTrigger.key -> {
                            onPreviewTriggered?.invoke()
                        }

                        Prefs.clearDownloadsTrigger.key -> {
                            onClearDownloadsTriggered?.invoke()
                        }

                        Prefs.persistentPreview.key -> {
                            val enabled = Prefs.persistentPreview.read(prefs)
                            persistentPreviewActive = enabled
                            onPersistentPreviewChanged?.invoke(enabled)
                        }

                        in Prefs.visualKeys -> {
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

    private fun refreshCache() {
        runCatching {
            remotePrefs?.let { prefs ->
                enabled = Prefs.enabled.read(prefs)
                color = Prefs.color.read(prefs)
                strokeWidth = Prefs.strokeWidth.read(prefs)
                ringGap = Prefs.ringGap.read(prefs)
                opacity = Prefs.opacity.read(prefs)
                hooksFeedback = Prefs.hooksFeedback.read(prefs)
                appVisible = Prefs.appVisible.read(prefs)
                clockwise = Prefs.clockwise.read(prefs)
                progressEasing = Prefs.progressEasing.read(prefs)
                errorColor = Prefs.errorColor.read(prefs)
                powerSaverMode = Prefs.powerSaverMode.read(prefs)
                showDownloadCount = Prefs.showDownloadCount.read(prefs)
                badgeOffsetX = Prefs.badgeOffsetX.read(prefs)
                badgeOffsetY = Prefs.badgeOffsetY.read(prefs)
                badgeTextSize = Prefs.badgeTextSize.read(prefs)
                finishStyle = Prefs.finishStyle.read(prefs)
                finishHoldMs = Prefs.finishHoldMs.read(prefs)
                finishExitMs = Prefs.finishExitMs.read(prefs)
                finishUseFlashColor = Prefs.finishUseFlashColor.read(prefs)
                finishFlashColor = Prefs.finishFlashColor.read(prefs)
                minVisibilityEnabled = Prefs.minVisibilityEnabled.read(prefs)
                minVisibilityMs = Prefs.minVisibilityMs.read(prefs)
                completionPulseEnabled = Prefs.completionPulseEnabled.read(prefs)
                percentTextEnabled = Prefs.percentTextEnabled.read(prefs)
                percentTextPosition = Prefs.percentTextPosition.read(prefs)
                percentTextOffsetX = Prefs.percentTextOffsetX.read(prefs)
                percentTextOffsetY = Prefs.percentTextOffsetY.read(prefs)
                percentTextSize = Prefs.percentTextSize.read(prefs)
                filenameTextEnabled = Prefs.filenameTextEnabled.read(prefs)
                filenameTextPosition = Prefs.filenameTextPosition.read(prefs)
                filenameTextOffsetX = Prefs.filenameTextOffsetX.read(prefs)
                filenameTextOffsetY = Prefs.filenameTextOffsetY.read(prefs)
                filenameTextSize = Prefs.filenameTextSize.read(prefs)
                filenameMaxChars = Prefs.filenameMaxChars.read(prefs)
                filenameTruncateEnabled = Prefs.filenameTruncateEnabled.read(prefs)
                percentTextBold = Prefs.percentTextBold.read(prefs)
                percentTextItalic = Prefs.percentTextItalic.read(prefs)
                filenameTextBold = Prefs.filenameTextBold.read(prefs)
                filenameTextItalic = Prefs.filenameTextItalic.read(prefs)
                filenameEllipsize = Prefs.filenameEllipsize.read(prefs)
                previewFilenameText = Prefs.previewFilenameText.read(prefs)
                ringScaleX = Prefs.ringScaleX.read(prefs)
                ringScaleY = Prefs.ringScaleY.read(prefs)
                ringScaleLinked = Prefs.ringScaleLinked.read(prefs)
                ringOffsetX = Prefs.ringOffsetX.read(prefs)
                ringOffsetY = Prefs.ringOffsetY.read(prefs)
                strokeCapStyle = Prefs.strokeCapStyle.read(prefs)
                backgroundRingEnabled = Prefs.backgroundRingEnabled.read(prefs)
                backgroundRingColor = Prefs.backgroundRingColor.read(prefs)
                backgroundRingOpacity = Prefs.backgroundRingOpacity.read(prefs)
                selectedPackages = Prefs.selectedPackages.read(prefs)
            }
        }.onFailure { log("refreshCache() failed", it) }
    }
}
