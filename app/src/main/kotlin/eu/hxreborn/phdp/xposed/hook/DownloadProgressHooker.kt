package eu.hxreborn.phdp.xposed.hook

import android.app.Notification
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.util.accessibleFieldFromHierarchy
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.util.logDebug
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object DownloadProgressHooker {
    private const val EXTRA_PROGRESS = "android.progress"
    private const val EXTRA_PROGRESS_MAX = "android.progressMax"
    private const val EXTRA_TITLE = "android.title"
    private const val STALE_AGE_MS = 5 * 60 * 1000L
    private const val SIGNIFICANT_DROP_PERCENT = 25
    private const val LOW_PROGRESS_THRESHOLD = 5
    private const val REASON_APP_CANCEL = 8
    private const val REASON_APP_CANCEL_ALL = 9
    private const val RV_ACTIONS_FIELD = "mActions"
    private const val RV_SET_PROGRESS = "setProgress"
    private const val RV_SET_MAX = "setMax"

    data class DownloadState(
        val packageName: String,
        val filename: String?,
        val progress: Int,
        val lastUpdate: Long,
    )

    @Volatile
    private var getPackageNameMethod: Method? = null

    @Volatile
    private var getNotificationMethod: Method? = null

    @Volatile
    private var getIdMethod: Method? = null

    private val activeDownloads = ConcurrentHashMap<String, DownloadState>()

    var onProgressChanged: ((Int) -> Unit)? = null
    var onDownloadComplete: (() -> Unit)? = null
    var onDownloadCancelled: (() -> Unit)? = null
    var onActiveCountChanged: ((Int) -> Unit)? = null
    var onFilenameChanged: ((String?) -> Unit)? = null
    var onPackageChanged: ((String?) -> Unit)? = null

    fun clearActiveDownloads() {
        activeDownloads.clear()
        onActiveCountChanged?.invoke(0)
        onProgressChanged?.invoke(0)
        onFilenameChanged?.invoke(null)
        onPackageChanged?.invoke(null)
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
                logDebug { "Removed stale: $staleId at ${state.progress}%" }
            }
    }

    fun processNotification(sbn: Any) {
        val pkg = getPackageName(sbn) ?: return
        logDebug { "Notification from: $pkg" }

        if (pkg !in PrefsManager.selectedPackages) return

        val id = getNotificationId(sbn) ?: return
        val notification = getNotification(sbn) ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(EXTRA_TITLE)?.toString()

        val (progress, max) =
            resolveProgress(
                extras.getInt(EXTRA_PROGRESS, -1),
                extras.getInt(EXTRA_PROGRESS_MAX, -1),
                notification,
            ) ?: run {
                // some browsers use max=0 as a completion signal rather than removing the notification
                activeDownloads.remove(id)?.let {
                    logDebug { "Download ${it.packageName}: 100% (max=0)" }
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

        if (oldState?.progress != percent) {
            activeDownloads[id] = newState
            logDebug { "Download $pkg: $percent%" }
            updateProgress()
            if (wasNew) onActiveCountChanged?.invoke(activeDownloads.size)
        }

        if (percent == 100) {
            activeDownloads.remove(id)
            onActiveCountChanged?.invoke(activeDownloads.size)
            onDownloadComplete?.invoke()
            updateProgress()
        }
    }

    private fun updateProgress() {
        val size = activeDownloads.size
        val avg = if (size == 0) 0 else activeDownloads.values.sumOf { it.progress } / size
        logDebug { "Progress: $avg% avg ($size active)" }
        onProgressChanged?.invoke(avg)
        updateFilename()
    }

    private fun updateFilename() {
        val leadingEntry = activeDownloads.maxByOrNull { it.value.progress }
        val filename =
            leadingEntry?.value?.filename?.takeUnless {
                it.contains("Untitled", ignoreCase = true)
            }
        onFilenameChanged?.invoke(filename)
        onPackageChanged?.invoke(leadingEntry?.value?.packageName)
    }

    private val Notification.isOngoing: Boolean
        get() = flags and Notification.FLAG_ONGOING_EVENT != 0

    private fun resolveProgress(
        progress: Int,
        max: Int,
        notification: Notification,
    ): Pair<Int, Int>? {
        if (progress >= 0 && max > 0) {
            return progress to max
        }

        logDebug { "flags=${notification.flags} ongoing=${notification.isOngoing}" }

        if (!notification.isOngoing) {
            return null
        }

        // some apps skip standard extras and encode progress in RemoteViews actions like Opera
        return extractRemoteViewsProgress(notification)?.also { (remoteProgress, remoteMax) ->
            logDebug { "RemoteViews progress: $remoteProgress/$remoteMax" }
        }
    }

    private fun remoteViewsActions(views: Any): ArrayList<*>? =
        runCatching {
            views.javaClass
                .accessibleFieldFromHierarchy(
                    RV_ACTIONS_FIELD,
                )?.get(views) as? ArrayList<*>
        }.getOrNull()

    private fun actionMethodName(action: Any): String? =
        (
            action.javaClass.accessibleFieldFromHierarchy("mMethodName") // API 35+
                ?: action.javaClass.accessibleFieldFromHierarchy("methodName")
        )?.get(action) as? String

    private fun actionIntValue(action: Any): Int? =
        (
            action.javaClass.accessibleFieldFromHierarchy("mValue") // API 35+
                ?: action.javaClass.accessibleFieldFromHierarchy("value")
        )?.get(action) as? Int

    private fun extractRemoteViewsProgress(notification: Notification): Pair<Int, Int>? {
        logDebug {
            "contentView=${notification.contentView} bigContentView=${notification.bigContentView}"
        }
        val views = notification.contentView ?: notification.bigContentView ?: return null
        val actions =
            remoteViewsActions(views) ?: run {
                logDebug { "mActions not found" }
                return null
            }
        logDebug {
            "actions count=${actions.size} types=${
                actions.filterNotNull().map {
                    it.javaClass.simpleName
                }.distinct()
            }"
        }
        val byMethod =
            actions
                .filterNotNull()
                .mapNotNull { action ->
                    val method = actionMethodName(action) ?: return@mapNotNull null
                    val value = actionIntValue(action) ?: return@mapNotNull null
                    method to value
                }.toMap()
        logDebug { "byMethod keys=${byMethod.keys}" }
        val p = byMethod[RV_SET_PROGRESS] ?: return null
        val m = byMethod[RV_SET_MAX]?.takeIf { it > 0 } ?: return null
        return p to m
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

    fun onNotificationRemoved(
        sbn: Any,
        reason: Int = -1,
    ) {
        val id = getNotificationId(sbn) ?: return
        logDebug { "Notification removed: $id (reason=$reason)" }

        val wasTracking = activeDownloads.remove(id)
        if (wasTracking != null) {
            onActiveCountChanged?.invoke(activeDownloads.size)
            updateProgress()
            when {
                wasTracking.progress >= 100 -> {
                    logDebug { "Download complete" }
                    onProgressChanged?.invoke(100)
                    onDownloadComplete?.invoke()
                }

                reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL -> {
                    logDebug { "Download completed (app removed at ${wasTracking.progress}%)" }
                    onProgressChanged?.invoke(100)
                    onDownloadComplete?.invoke()
                }

                wasTracking.progress >= LOW_PROGRESS_THRESHOLD -> {
                    logDebug { "Download cancelled at ${wasTracking.progress}%" }
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
