package eu.hxreborn.phdp.prefs

import eu.hxreborn.phdp.ui.theme.MaterialPalette

object Prefs {
    const val GROUP = "phdp_settings"

    // Core
    val enabled = BoolPref("enabled", true)
    val hooksFeedback = BoolPref("hooks_feedback", false)
    val appVisible = BoolPref("app_visible", false)

    // Appearance
    val color = IntPref("color", MaterialPalette.Blue500)
    val strokeWidth = FloatPref("stroke_width", 2f, 0.5f..10f)
    val ringGap = FloatPref("ring_gap", 1.155f, 0.5f..3f)
    val opacity = IntPref("opacity", 90, 1..100)
    val clockwise = BoolPref("clockwise", true)
    val errorColor = IntPref("error_color", MaterialPalette.Red500)
    val strokeCapStyle = StringPref("stroke_cap_style", "flat")

    // Background ring
    val backgroundRingEnabled = BoolPref("background_ring_enabled", true)
    val backgroundRingColor = IntPref("background_ring_color", MaterialPalette.Grey)
    val backgroundRingOpacity = IntPref("background_ring_opacity", 30, 1..100)

    // Geometry
    val ringScaleX = FloatPref("ring_scale_x", 1f, 0.25f..3f)
    val ringScaleY = FloatPref("ring_scale_y", 1f, 0.25f..3f)
    val ringScaleLinked = BoolPref("ring_scale_linked", true)
    val ringOffsetX = FloatPref("ring_offset_x", 0f, -500f..500f)
    val ringOffsetY = FloatPref("ring_offset_y", 0f, -500f..500f)
    val pathMode = BoolPref("path_mode", false)

    // Animation
    val progressEasing = StringPref("progress_easing", "linear")
    val finishStyle = StringPref("finish_style", "pop")
    val finishHoldMs = IntPref("finish_hold_ms", 500, 0..5000)
    val finishExitMs = IntPref("finish_exit_ms", 500, 50..3000)
    val finishUseFlashColor = BoolPref("finish_use_flash_color", true)
    val finishFlashColor = IntPref("finish_flash_color", MaterialPalette.White)
    val completionPulseEnabled = BoolPref("completion_pulse_enabled", true)

    // Text overlays
    val percentTextEnabled = BoolPref("percent_text_enabled", false)
    val percentTextPosition = StringPref("percent_text_position", "right")
    val percentTextOffsetX = FloatPref("percent_text_offset_x", 0f, -200f..200f)
    val percentTextOffsetY = FloatPref("percent_text_offset_y", 0f, -200f..200f)
    val percentTextSize = FloatPref("percent_text_size", 8f, 4f..20f)
    val filenameTextEnabled = BoolPref("filename_text_enabled", false)
    val filenameTextPosition = StringPref("filename_text_position", "top_right")
    val filenameTextOffsetX = FloatPref("filename_text_offset_x", 0f, -200f..200f)
    val filenameTextOffsetY = FloatPref("filename_text_offset_y", 0f, -200f..200f)
    val filenameTextSize = FloatPref("filename_text_size", 7f, 4f..20f)
    val filenameMaxChars = IntPref("filename_max_chars", 20, 5..100)
    val filenameTruncateEnabled = BoolPref("filename_truncate_enabled", true)
    val percentTextBold = BoolPref("percent_text_bold", true)
    val percentTextItalic = BoolPref("percent_text_italic", false)
    val filenameTextBold = BoolPref("filename_text_bold", false)
    val filenameTextItalic = BoolPref("filename_text_italic", false)
    val filenameEllipsize = StringPref("filename_ellipsize", "middle")
    val previewFilenameText =
        StringPref(
            "preview_filename_text",
            "EvolutionX-16.0-20260116-RMX2170-11.6-Unofficial.zip",
        )

    // Timing
    val minVisibilityEnabled = BoolPref("min_visibility_enabled", true)
    val minVisibilityMs = IntPref("min_visibility_ms", 500, 0..2000)
    val showDownloadCount = BoolPref("show_download_count", false)
    val badgeOffsetX = FloatPref("badge_offset_x", 0f, -200f..200f)
    val badgeOffsetY = FloatPref("badge_offset_y", 0f, -200f..200f)
    val badgeTextSize = FloatPref("badge_text_size", 10f, 4f..20f)

    // Power
    val powerSaverMode = StringPref("power_saver_mode", "normal")

    // UI-only
    val darkThemeConfig = StringPref("dark_theme_config", "follow_system")
    val useDynamicColor = BoolPref("use_dynamic_color", true)

    val defaultSupportedPackages: Set<String> =
        setOf(
            // System
            "com.android.providers.downloads",
            // Download managers
            "com.dv.adm",
            "com.dv.adm.pay",
            "idm.internet.download.manager",
            "idm.internet.download.manager.plus",
            "com.downloadmanager.android",
            "com.download.video.manager.downloader",
            // Firefox and forks
            "org.mozilla.firefox",
            "org.mozilla.fenix",
            "org.mozilla.firefox_beta",
            "org.mozilla.fennec_aurora",
            "org.mozilla.fennec_fdroid",
            "org.mozilla.focus",
            "io.github.forkmaintainers.iceraven",
            "us.spotco.fennec_dos",
            "org.torproject.torbrowser",
            "org.torproject.torbrowser_alpha",
            // Chrome and Chromium forks
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome",
            "org.cromite.cromite",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_nightly",
            "app.vanadium.browser",
            "com.kiwibrowser.browser",
            "com.vivaldi.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.microsoft.emmx",
            "com.duckduckgo.mobile.android",
            "com.yandex.browser",
            // Samsung
            "com.sec.android.app.sbrowser",
            // App stores
            "com.android.vending",
            "org.fdroid.fdroid",
            "com.looker.droidify",
            "com.machiav3lli.fdroid",
            "com.aurora.store",
            "dev.imranr.obtainium",
        )

    val selectedPackages = SetPref("selected_packages", defaultSupportedPackages)
    val showSystemPackages = BoolPref("show_system_packages", false)

    // Trigger-only (ephemeral, not in resettable)
    val testProgress = IntPref("test_progress", -1)
    val testError = BoolPref("test_error", false)
    val previewTrigger = IntPref("preview_trigger", 0)
    val clearDownloadsTrigger = IntPref("clear_downloads_trigger", 0)
    val persistentPreview = BoolPref("persistent_preview", false)

    // All resettable prefs (excludes enabled and triggers)
    val resettable: List<PrefSpec<*>> =
        listOf(
            color,
            strokeWidth,
            ringGap,
            opacity,
            clockwise,
            errorColor,
            strokeCapStyle,
            backgroundRingEnabled,
            backgroundRingColor,
            backgroundRingOpacity,
            ringScaleX,
            ringScaleY,
            ringScaleLinked,
            ringOffsetX,
            ringOffsetY,
            pathMode,
            progressEasing,
            finishStyle,
            finishHoldMs,
            finishExitMs,
            finishUseFlashColor,
            finishFlashColor,
            completionPulseEnabled,
            percentTextEnabled,
            percentTextPosition,
            percentTextOffsetX,
            percentTextOffsetY,
            percentTextSize,
            filenameTextEnabled,
            filenameTextPosition,
            filenameTextOffsetX,
            filenameTextOffsetY,
            filenameTextSize,
            filenameMaxChars,
            filenameTruncateEnabled,
            percentTextBold,
            percentTextItalic,
            filenameTextBold,
            filenameTextItalic,
            filenameEllipsize,
            previewFilenameText,
            minVisibilityEnabled,
            minVisibilityMs,
            showDownloadCount,
            badgeOffsetX,
            badgeOffsetY,
            badgeTextSize,
            powerSaverMode,
            hooksFeedback,
        )

    // Keys that trigger geometry preview
    val visualKeys: Set<String> =
        setOf(
            color.key,
            strokeWidth.key,
            ringGap.key,
            opacity.key,
            clockwise.key,
            strokeCapStyle.key,
            backgroundRingEnabled.key,
            backgroundRingColor.key,
            backgroundRingOpacity.key,
            ringScaleX.key,
            ringScaleY.key,
            ringOffsetX.key,
            ringOffsetY.key,
            pathMode.key,
            percentTextPosition.key,
            percentTextOffsetX.key,
            percentTextOffsetY.key,
            percentTextSize.key,
            filenameTextPosition.key,
            filenameTextOffsetX.key,
            filenameTextOffsetY.key,
            filenameTextSize.key,
            filenameMaxChars.key,
            filenameTruncateEnabled.key,
            percentTextBold.key,
            percentTextItalic.key,
            filenameTextBold.key,
            filenameTextItalic.key,
            filenameEllipsize.key,
            previewFilenameText.key,
            badgeOffsetX.key,
            badgeOffsetY.key,
            badgeTextSize.key,
        )
}
