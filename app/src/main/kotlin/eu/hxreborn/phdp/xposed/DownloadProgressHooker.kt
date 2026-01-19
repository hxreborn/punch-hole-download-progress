package eu.hxreborn.phdp.xposed

import android.app.Notification
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object DownloadProgressHooker {
    private const val EXTRA_PROGRESS = "android.progress"
    private const val EXTRA_PROGRESS_MAX = "android.progressMax"
    private const val EXTRA_TITLE = "android.title"

    // Debug logging - logs everything in debug builds, nothing in release
    private inline fun debug(msg: () -> String) {
        if (BuildConfig.DEBUG) log(msg())
    }

    // Cached reflection methods for efficiency
    @Volatile private var getPackageNameMethod: Method? = null

    @Volatile private var getNotificationMethod: Method? = null

    @Volatile private var getIdMethod: Method? = null

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
    private val activeDownloads = ConcurrentHashMap<String, Int>()

    // Active downloads: id -> filename (for display)
    private val activeFilenames = ConcurrentHashMap<String, String>()

    var onProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onDownloadCancelled: (() -> Unit)? = null
    var onActiveCountChanged: ((Int) -> Unit)? = null
    var onFilenameChanged: ((String?) -> Unit)? = null

    fun processNotification(sbn: Any) {
        val pkg = getPackageName(sbn) ?: return
        debug { "Notification from: $pkg" }

        if (pkg !in SUPPORTED_PACKAGES) return

        val id = getNotificationId(sbn) ?: return
        val notification = getNotification(sbn) ?: return
        val extras = notification.extras ?: return

        val progress = extras.getInt(EXTRA_PROGRESS, -1)
        val max = extras.getInt(EXTRA_PROGRESS_MAX, -1)
        val title = extras.getCharSequence(EXTRA_TITLE)?.toString()
        debug { "Progress: $progress/$max, title: $title" }

        if (progress >= 0 && max > 0) {
            val percent = (progress * 100 / max).coerceIn(0, 100)
            val oldPercent = activeDownloads[id]
            val wasNew = oldPercent == null

            if (oldPercent != percent) {
                activeDownloads[id] = percent
                title?.let { activeFilenames[id] = it }
                log("Download $pkg: $percent%")
                updateProgress()
                if (wasNew) onActiveCountChanged?.invoke(activeDownloads.size)
            }

            if (percent == 100) {
                activeDownloads.remove(id)
                activeFilenames.remove(id)
                onActiveCountChanged?.invoke(activeDownloads.size)
                onDownloadComplete?.invoke()
                updateFilename()
            }
        } else {
            val wasTracking = activeDownloads.remove(id)
            activeFilenames.remove(id)
            if (wasTracking != null) {
                log("Download $pkg: complete")
                onActiveCountChanged?.invoke(activeDownloads.size)
                onProgressChanged?.invoke(100)
                onDownloadComplete?.invoke()
                updateProgress()
                updateFilename()
            }
        }
    }

    private fun updateProgress() {
        val maxProgress = activeDownloads.values.maxOrNull() ?: 0
        debug { "Progress: $maxProgress% (${activeDownloads.size} active)" }
        onProgressChanged?.invoke(maxProgress)
        updateFilename()
    }

    // Find filename of download with highest progress (leading download)
    private fun updateFilename() {
        val leadingId = activeDownloads.maxByOrNull { it.value }?.key
        val filename = leadingId?.let { activeFilenames[it] }
        onFilenameChanged?.invoke(filename)
    }

    private fun getPackageName(sbn: Any): String? =
        runCatching {
            val method =
                getPackageNameMethod
                    ?: sbn.javaClass.getMethod("getPackageName").also { getPackageNameMethod = it }
            method.invoke(sbn) as? String
        }.getOrNull()

    private fun getNotification(sbn: Any): Notification? =
        runCatching {
            val method =
                getNotificationMethod
                    ?: sbn.javaClass.getMethod("getNotification").also { getNotificationMethod = it }
            method.invoke(sbn) as? Notification
        }.getOrNull()

    private fun getNotificationId(sbn: Any): String? {
        val pkg = getPackageName(sbn) ?: return null
        val id =
            runCatching {
                val method =
                    getIdMethod
                        ?: sbn.javaClass.getMethod("getId").also { getIdMethod = it }
                method.invoke(sbn) as? Int
            }.getOrNull() ?: return null
        return "$pkg:$id"
    }

    fun onNotificationRemoved(sbn: Any) {
        val id = getNotificationId(sbn) ?: return
        debug { "Notification removed: $id" }

        val wasTracking = activeDownloads.remove(id)
        activeFilenames.remove(id)
        if (wasTracking != null) {
            onActiveCountChanged?.invoke(activeDownloads.size)
            if (wasTracking < 100) {
                log("Download cancelled at $wasTracking%")
                onDownloadCancelled?.invoke()
            } else {
                log("Download complete")
                onProgressChanged?.invoke(100)
                onDownloadComplete?.invoke()
            }
            updateProgress()
        }
    }

    internal fun isStatusBarNotification(obj: Any): Boolean = obj.javaClass.name.contains("StatusBarNotification")
}

@XposedHooker
class NotificationAddHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            if (BuildConfig.DEBUG) log("NotificationAddHooker: ${callback.args?.size ?: 0} args")

            callback.args?.forEach { arg ->
                if (arg == null) return@forEach

                if (DownloadProgressHooker.isStatusBarNotification(arg)) {
                    DownloadProgressHooker.processNotification(arg)
                } else if (arg.javaClass.name.contains("NotificationEntry")) {
                    runCatching {
                        val sbn =
                            arg.javaClass
                                .getDeclaredField("mSbn")
                                .apply { isAccessible = true }
                                .get(arg)
                        if (sbn != null) DownloadProgressHooker.processNotification(sbn)
                    }
                }
            }
        }
    }
}

@XposedHooker
class NotificationRemoveHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            if (BuildConfig.DEBUG) log("NotificationRemoveHooker: ${callback.args?.size ?: 0} args")

            callback.args?.forEach { arg ->
                if (arg == null) return@forEach

                if (DownloadProgressHooker.isStatusBarNotification(arg)) {
                    DownloadProgressHooker.onNotificationRemoved(arg)
                    return@forEach
                }

                if (arg.javaClass.name.contains("NotificationEntry")) {
                    runCatching {
                        val sbn = arg.javaClass.accessibleField("mSbn").get(arg)
                        if (sbn != null) DownloadProgressHooker.onNotificationRemoved(sbn)
                    }
                }
            }
        }
    }
}
