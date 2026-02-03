package eu.hxreborn.phdp.ui.state

import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig

data class PrefsState(
    val enabled: Boolean = Prefs.enabled.default,
    val color: Int = Prefs.color.default,
    val strokeWidth: Float = Prefs.strokeWidth.default,
    val ringGap: Float = Prefs.ringGap.default,
    val opacity: Int = Prefs.opacity.default,
    val hooksFeedback: Boolean = Prefs.hooksFeedback.default,
    val clockwise: Boolean = Prefs.clockwise.default,
    val progressEasing: String = Prefs.progressEasing.default,
    val errorColor: Int = Prefs.errorColor.default,
    val powerSaverMode: String = Prefs.powerSaverMode.default,
    val showDownloadCount: Boolean = Prefs.showDownloadCount.default,
    val finishStyle: String = Prefs.finishStyle.default,
    val finishHoldMs: Int = Prefs.finishHoldMs.default,
    val finishExitMs: Int = Prefs.finishExitMs.default,
    val finishUseFlashColor: Boolean = Prefs.finishUseFlashColor.default,
    val finishFlashColor: Int = Prefs.finishFlashColor.default,
    val minVisibilityEnabled: Boolean = Prefs.minVisibilityEnabled.default,
    val minVisibilityMs: Int = Prefs.minVisibilityMs.default,
    val completionPulseEnabled: Boolean = Prefs.completionPulseEnabled.default,
    val percentTextEnabled: Boolean = Prefs.percentTextEnabled.default,
    val percentTextPosition: String = Prefs.percentTextPosition.default,
    val filenameTextEnabled: Boolean = Prefs.filenameTextEnabled.default,
    val filenameTextPosition: String = Prefs.filenameTextPosition.default,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = Prefs.useDynamicColor.default,
    val ringScaleX: Float = Prefs.ringScaleX.default,
    val ringScaleY: Float = Prefs.ringScaleY.default,
    val ringScaleLinked: Boolean = Prefs.ringScaleLinked.default,
    val ringOffsetX: Float = Prefs.ringOffsetX.default,
    val ringOffsetY: Float = Prefs.ringOffsetY.default,
    val selectedPackages: Set<String> = Prefs.selectedPackages.default,
    val showSystemPackages: Boolean = Prefs.showSystemPackages.default,
)
