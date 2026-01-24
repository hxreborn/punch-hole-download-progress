package eu.hxreborn.phdp.xposed.hook

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
    private const val STALE_AGE_MS = 5 * 60 * 1000L
    private const val SIGNIFICANT_DROP_PERCENT = 25
    private const val LOW_PROGRESS_THRESHOLD = 5

    data class DownloadState(
        val packageName: String,
        val filename: String?,
        val progress: Int,
        val lastUpdate: Long,
    )

    private inline fun debug(msg: () -> String) {
        if (BuildConfig.DEBUG) log(msg())
    }

    @Volatile
    private var getPackageNameMethod: Method? = null

    @Volatile
    private var getNotificationMethod: Method? = null

    @Volatile
    private var getIdMethod: Method? = null

    private val SUPPORTED_PACKAGES =
        setOf(
            // System
            "com.android.providers.downloads",
            // Firefox and forks
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
            // Chrome and Chromium forks
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

    private val activeDownloads = ConcurrentHashMap<String, DownloadState>()

    var onProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onDownloadCancelled: (() -> Unit)? = null
    var onActiveCountChanged: ((Int) -> Unit)? = null
    var onFilenameChanged: ((String?) -> Unit)? = null

    fun clearActiveDownloads() {
        activeDownloads.clear()
        onActiveCountChanged?.invoke(0)
        onProgressChanged?.invoke(0)
        onFilenameChanged?.invoke(null)
        log("Downloads cleared manually")
    }

    private fun cleanupStaleSamePackage(
        pkg: String,
        currentId: String,
    ) {
        val now = System.currentTimeMillis()
        activeDownloads.entries
            .filter { it.key != currentId && it.value.packageName == pkg }
            .filter { now - it.value.lastUpdate > STALE_AGE_MS }
            .forEach { (staleId, state) ->
                activeDownloads.remove(staleId)
                log("Removed stale: $staleId at ${state.progress}%")
            }
    }

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

        // Browsers signal completion via max=0 update, not notification removal
        if (progress < 0 || max <= 0) {
            activeDownloads.remove(id)?.let {
                log("Download ${it.packageName}: 100% (max=0)")
                onActiveCountChanged?.invoke(activeDownloads.size)
                onDownloadComplete?.invoke()
                updateProgress()
                updateFilename()
            }
            return
        }

        val percent = (progress * 100 / max).coerceIn(0, 100)
        val oldState = activeDownloads[id]
        val reset = oldState != null && oldState.progress - percent >= SIGNIFICANT_DROP_PERCENT
        if (reset) activeDownloads.remove(id)
        val wasNew = oldState == null || reset

        cleanupStaleSamePackage(pkg, id)

        val newState =
            DownloadState(
                packageName = pkg,
                filename = title ?: oldState?.filename,
                progress = percent,
                lastUpdate = System.currentTimeMillis(),
            )

        if (oldState?.progress != percent || wasNew) {
            activeDownloads[id] = newState
            log("Download $pkg: $percent%")
            updateProgress()
            if (wasNew) onActiveCountChanged?.invoke(activeDownloads.size)
        }

        if (percent == 100) {
            activeDownloads.remove(id)
            onActiveCountChanged?.invoke(activeDownloads.size)
            onDownloadComplete?.invoke()
            updateFilename()
        }
    }

    private fun updateProgress() {
        val avg =
            if (activeDownloads.isEmpty()) {
                0
            } else {
                activeDownloads.values
                    .map { it.progress }
                    .average()
                    .toInt()
            }
        debug { "Progress: $avg% avg (${activeDownloads.size} active)" }
        onProgressChanged?.invoke(avg)
        updateFilename()
    }

    private fun updateFilename() {
        val leadingEntry = activeDownloads.maxByOrNull { it.value.progress }
        val filename = leadingEntry?.value?.filename
        onFilenameChanged?.invoke(filename)
    }

    private fun getPackageName(sbn: Any): String? =
        runCatching {
            val method =
                getPackageNameMethod ?: sbn.javaClass
                    .getMethod("getPackageName")
                    .also { getPackageNameMethod = it }
            method.invoke(sbn) as? String
        }.getOrNull()

    private fun getNotification(sbn: Any): Notification? =
        runCatching {
            val method =
                getNotificationMethod ?: sbn.javaClass
                    .getMethod(
                        "getNotification",
                    ).also { getNotificationMethod = it }
            method.invoke(sbn) as? Notification
        }.getOrNull()

    private fun getNotificationId(sbn: Any): String? {
        val pkg = getPackageName(sbn) ?: return null
        val id =
            runCatching {
                val method =
                    getIdMethod ?: sbn.javaClass.getMethod("getId").also { getIdMethod = it }
                method.invoke(sbn) as? Int
            }.getOrNull() ?: return null
        return "$pkg:$id"
    }

    fun onNotificationRemoved(sbn: Any) {
        val id = getNotificationId(sbn) ?: return
        debug { "Notification removed: $id" }

        val wasTracking = activeDownloads.remove(id)
        if (wasTracking != null) {
            onActiveCountChanged?.invoke(activeDownloads.size)
            updateProgress()
            when {
                wasTracking.progress >= 100 -> {
                    log("Download complete")
                    onProgressChanged?.invoke(100)
                    onDownloadComplete?.invoke()
                }

                wasTracking.progress >= LOW_PROGRESS_THRESHOLD -> {
                    log("Download cancelled at ${wasTracking.progress}%")
                    onDownloadCancelled?.invoke()
                }
            }
        }
    }

    internal fun isStatusBarNotification(obj: Any): Boolean =
        obj.javaClass.name.contains("StatusBarNotification")

    internal inline fun processNotificationArg(
        arg: Any?,
        action: (Any) -> Unit,
    ) {
        if (arg == null) return
        when {
            isStatusBarNotification(arg) -> {
                action(arg)
            }

            arg.javaClass.name.contains("NotificationEntry") -> {
                runCatching { arg.javaClass.accessibleField("mSbn").get(arg) }
                    .getOrNull()
                    ?.let(action)
            }
        }
    }
}

@XposedHooker
class NotificationAddHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            if (BuildConfig.DEBUG) log("NotificationAddHooker: ${callback.args.size} args")
            callback.args.forEach {
                DownloadProgressHooker.processNotificationArg(
                    it,
                    DownloadProgressHooker::processNotification,
                )
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
            if (BuildConfig.DEBUG) log("NotificationRemoveHooker: ${callback.args.size} args")
            callback.args.forEach {
                DownloadProgressHooker.processNotificationArg(
                    it,
                    DownloadProgressHooker::onNotificationRemoved,
                )
            }
        }
    }
}
