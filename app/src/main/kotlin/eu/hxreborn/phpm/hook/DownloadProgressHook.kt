package eu.hxreborn.phpm.hook

import android.app.Notification
import eu.hxreborn.phpm.PunchHoleMonitorModule.Companion.log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

object DownloadProgressHook {
    private const val EXTRA_PROGRESS = "android.progress"
    private const val EXTRA_PROGRESS_MAX = "android.progressMax"

    // Browser/downloader packages using standard android.progress/android.progressMax extras
    private val SUPPORTED_PACKAGES =
        setOf(
            // System
            "com.android.providers.downloads",
            // Firefox & forks
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fennec_aurora",
            "org.mozilla.fennec_fdroid",
            "org.mozilla.focus",
            "org.mozilla.klar",
            "io.github.nicktechnik.niceraven",
            "io.github.forkmaintainers.iceraven",
            "us.nickel.nickel_niceraven",
            "us.nickel.nickel.niceraven",
            "us.spotco.fennec_dos",
            "org.torproject.torbrowser",
            "org.torproject.torbrowser_alpha",
            // Chrome & Chromium forks
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome",
            "com.brave.browser",
            "com.brave.browser_beta",
            "com.brave.browser_nightly",
            "org.nickel.nickel",
            "app.nickel.nickel",
            "app.nickelglo.nickelglo",
            "app.nickelglo.nickelglo.nickelglo",
            "app.nickel.nickel.nickel",
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
        )

    // Active downloads: id -> progress (0-100)
    private val activeDownloads = mutableMapOf<String, Int>()

    var onProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onDownloadCancelled: (() -> Unit)? = null
    var onActiveCountChanged: ((Int) -> Unit)? = null

    fun onNotificationAdded(sbn: Any) {
        processNotification(sbn)
    }

    private fun processNotification(sbn: Any) {
        val pkg =
            runCatching {
                sbn.javaClass.getMethod("getPackageName").invoke(sbn) as? String
            }.getOrNull() ?: return

        if (pkg !in SUPPORTED_PACKAGES) return

        val id = getNotificationId(sbn) ?: return
        val notification =
            runCatching {
                sbn.javaClass.getMethod("getNotification").invoke(sbn) as? Notification
            }.getOrNull() ?: return

        val extras = notification.extras ?: return
        val progress = extras.getInt(EXTRA_PROGRESS, -1)
        val max = extras.getInt(EXTRA_PROGRESS_MAX, -1)

        if (progress >= 0 && max > 0) {
            val percent = (progress * 100 / max).coerceIn(0, 100)

            val oldPercent = activeDownloads[id]
            val wasNew = oldPercent == null
            if (oldPercent != percent) {
                activeDownloads[id] = percent
                log("Download $pkg: $percent% (id=$id)")
                updateProgress()
                if (wasNew) {
                    onActiveCountChanged?.invoke(activeDownloads.size)
                }
            }

            // Clean up completed downloads and trigger callback
            if (percent == 100) {
                activeDownloads.remove(id)
                onActiveCountChanged?.invoke(activeDownloads.size)
                onDownloadComplete?.invoke()
            }
        } else {
            // Some browsers replace progress notification with "Download complete" text notification
            val wasTracking = activeDownloads.remove(id)
            if (wasTracking != null) {
                log("Download $pkg replaced with non-progress notification at $wasTracking% - treating as complete")
                onActiveCountChanged?.invoke(activeDownloads.size)
                // Trigger 100% to start finish animation, then clear
                onProgressChanged?.invoke(100)
                onDownloadComplete?.invoke()
                // After animation starts, clear progress
                updateProgress()
            }
        }
    }

    private fun updateProgress() {
        val maxProgress = activeDownloads.values.maxOrNull() ?: 0
        log("Overall progress: $maxProgress% (${activeDownloads.size} active)")
        onProgressChanged?.invoke(maxProgress)
    }

    private fun getNotificationId(sbn: Any): String? {
        return runCatching {
            val pkg =
                sbn.javaClass.getMethod("getPackageName").invoke(sbn) as? String ?: return null
            val id = sbn.javaClass.getMethod("getId").invoke(sbn) as? Int ?: return null
            "$pkg:$id"
        }.getOrNull()
    }

    fun onNotificationRemoved(sbn: Any) {
        val id = getNotificationId(sbn) ?: return
        val wasTracking = activeDownloads.remove(id)
        if (wasTracking != null) {
            onActiveCountChanged?.invoke(activeDownloads.size)
            if (wasTracking < 100) {
                // Download was cancelled mid-progress
                log("Download notification removed while at $wasTracking% (id=$id) - cancelled")
                onDownloadCancelled?.invoke()
            } else {
                // Download completed normally
                log("Download notification removed at 100% (id=$id) - complete")
                onProgressChanged?.invoke(100)
                onDownloadComplete?.invoke()
            }
            updateProgress()
        }
    }

    fun clearAll() {
        activeDownloads.clear()
        onProgressChanged?.invoke(0)
    }
}

@XposedHooker
class NotificationAddHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            callback.args?.forEach { arg ->
                if (arg != null && isStatusBarNotification(arg)) {
                    DownloadProgressHook.onNotificationAdded(arg)
                }
            }
        }

        private fun isStatusBarNotification(obj: Any): Boolean = obj.javaClass.name.contains("StatusBarNotification")
    }
}

@XposedHooker
class NotificationRemoveHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            callback.args?.forEach { arg ->
                if (arg == null) return@forEach

                // Direct StatusBarNotification
                if (isStatusBarNotification(arg)) {
                    DownloadProgressHook.onNotificationRemoved(arg)
                    return@forEach
                }

                // Android 12+ wraps SBN in NotificationEntry
                if (arg.javaClass.name.contains("NotificationEntry")) {
                    runCatching {
                        val sbnField = arg.javaClass.getDeclaredField("mSbn")
                        sbnField.isAccessible = true
                        val sbn = sbnField.get(arg)
                        if (sbn != null) {
                            DownloadProgressHook.onNotificationRemoved(sbn)
                        }
                    }
                }
            }
        }

        private fun isStatusBarNotification(obj: Any): Boolean = obj.javaClass.name.contains("StatusBarNotification")
    }
}
