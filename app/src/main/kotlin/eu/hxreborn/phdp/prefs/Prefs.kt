package eu.hxreborn.phdp.prefs

object Prefs {
    const val GROUP = "phdp_settings"

    // Core
    val enabled = BoolPref("enabled", true)
    val hooksFeedback = BoolPref("hooks_feedback", false)
    val appVisible = BoolPref("app_visible", false)

    // Appearance
    val color = IntPref("color", 0xFF2196F3.toInt())
    val strokeWidth = FloatPref("stroke_width", 2f, 0.5f..10f)
    val ringGap = FloatPref("ring_gap", 1.155f, 0.5f..3f)
    val opacity = IntPref("opacity", 90, 1..100)
    val clockwise = BoolPref("clockwise", true)
    val errorColor = IntPref("error_color", 0xFFF44336.toInt())

    // Geometry
    val ringScaleX = FloatPref("ring_scale_x", 1f, 0.25f..3f)
    val ringScaleY = FloatPref("ring_scale_y", 1f, 0.25f..3f)
    val ringScaleLinked = BoolPref("ring_scale_linked", true)
    val ringOffsetX = FloatPref("ring_offset_x", 0f, -500f..500f)
    val ringOffsetY = FloatPref("ring_offset_y", 0f, -500f..500f)

    // Animation
    val progressEasing = StringPref("progress_easing", "linear")
    val finishStyle = StringPref("finish_style", "pop")
    val finishHoldMs = IntPref("finish_hold_ms", 500, 0..5000)
    val finishExitMs = IntPref("finish_exit_ms", 500, 50..3000)
    val finishUseFlashColor = BoolPref("finish_use_flash_color", true)
    val finishFlashColor = IntPref("finish_flash_color", 0xFFFFFFFF.toInt())
    val completionPulseEnabled = BoolPref("completion_pulse_enabled", true)

    // Text overlays
    val percentTextEnabled = BoolPref("percent_text_enabled", false)
    val percentTextPosition = StringPref("percent_text_position", "right")
    val filenameTextEnabled = BoolPref("filename_text_enabled", false)
    val filenameTextPosition = StringPref("filename_text_position", "top_right")

    // Timing
    val minVisibilityEnabled = BoolPref("min_visibility_enabled", true)
    val minVisibilityMs = IntPref("min_visibility_ms", 500, 0..2000)
    val showDownloadCount = BoolPref("show_download_count", false)

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
            ringScaleX,
            ringScaleY,
            ringScaleLinked,
            ringOffsetX,
            ringOffsetY,
            progressEasing,
            finishStyle,
            finishHoldMs,
            finishExitMs,
            finishUseFlashColor,
            finishFlashColor,
            completionPulseEnabled,
            percentTextEnabled,
            percentTextPosition,
            filenameTextEnabled,
            filenameTextPosition,
            minVisibilityEnabled,
            minVisibilityMs,
            showDownloadCount,
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
            ringScaleX.key,
            ringScaleY.key,
            ringOffsetX.key,
            ringOffsetY.key,
        )
}
