package eu.hxreborn.phdp.xposed.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.util.accessibleFieldFromHierarchy
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.util.logDebug
import eu.hxreborn.phdp.xposed.indicator.IndicatorView
import eu.hxreborn.phdp.xposed.module
import io.github.libxposed.api.XposedInterface

private const val CENTRAL_SURFACES_IMPL = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
private const val STATUS_BAR = "com.android.systemui.statusbar.phone.StatusBar"
private const val NOTIF_COLLECTION =
    "com.android.systemui.statusbar.notification.collection.NotifCollection"
private const val NOTIFICATION_LISTENER = "com.android.systemui.statusbar.NotificationListener"

// Scoped to process lifetime, cannot leak
@SuppressLint("StaticFieldLeak")
object SystemUIHooker {
    @Volatile
    private var attached = false

    @Volatile
    private var indicatorView: IndicatorView? = null

    @Volatile
    private var powerSaveReceiver: BroadcastReceiver? = null

    @Volatile
    private var systemUIContext: Context? = null

    fun hook(classLoader: ClassLoader) {
        wireCallbacks()
        hookSystemUIEntry(classLoader)
        hookNotificationListener(classLoader)
        hookNotifications(classLoader)
    }

    // Earliest SystemUI entry point, bypasses GroupCoalescer delay on some apps
    // Some latency remains because browsers use internal download managers. Would need per pkg hooks to fix
    private fun hookNotificationListener(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(NOTIFICATION_LISTENER) }
                .onFailure {
                    log(
                        "Failed to load $NOTIFICATION_LISTENER",
                        it,
                    )
                }.getOrNull() ?: return

        targetClass.declaredMethods.filter { it.name == "onNotificationPosted" }.forEach { method ->
            runCatching {
                module.hook(method).intercept(notificationAddHooker)
            }.onSuccess { log("Hooked NotificationListener.onNotificationPosted") }
        }
    }

    // Earliest reliable SystemUI context for overlay attach
    // CentralSurfacesImpl (13+), StatusBar (9-12L)
    private fun hookSystemUIEntry(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(CENTRAL_SURFACES_IMPL) }
                .recoverCatching {
                    classLoader.loadClass(STATUS_BAR)
                }.onFailure { log("Failed to load CentralSurfaces/StatusBar", it) }
                .getOrNull()
                ?: return

        val startMethod =
            targetClass.declaredMethods.find { it.name == "start" && it.parameterCount == 0 }
        if (startMethod == null) {
            log("start() not found in ${targetClass.name}")
            return
        }

        runCatching {
            module.hook(startMethod).intercept { chain ->
                val result = chain.proceed()
                if (isAttached()) return@intercept result
                val instance = chain.thisObject ?: return@intercept result
                val context =
                    runCatching {
                        instance.javaClass
                            .accessibleFieldFromHierarchy("mContext")
                            ?.get(instance) as? Context
                    }.getOrNull()
                if (context == null) {
                    log("Failed to extract Context from ${instance.javaClass.simpleName}")
                    return@intercept result
                }
                runCatching {
                    val view = IndicatorView.attach(context)
                    markAttached(view, context)
                    log("IndicatorView attached")
                }.onFailure { log("Failed to attach IndicatorView", it) }
                result
            }
        }.onSuccess { log("Hooked ${targetClass.simpleName}.start()") }
            .onFailure { log("Hook failed", it) }
    }

    private fun hookNotifications(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(NOTIF_COLLECTION) }
                .onFailure {
                    log(
                        "Failed to load $NOTIF_COLLECTION",
                        it,
                    )
                }.getOrNull() ?: return

        // postNotification entry point for new notifications on Android 12 and later
        targetClass.declaredMethods.filter { it.name == "postNotification" }.forEach { method ->
            runCatching {
                module.hook(method).intercept(notificationAddHooker)
            }.onSuccess { log("Hooked NotifCollection.postNotification") }
        }

        // Hook notification removal - method name varies by Android version
        // Android 16+: tryRemoveNotification(NotificationEntry)
        // Android 12-15: onNotificationRemoved(StatusBarNotification, RankingMap, int)
        targetClass.declaredMethods
            .filter {
                it.name == "tryRemoveNotification" || it.name == "onNotificationRemoved"
            }.forEach { method ->
                runCatching {
                    module.hook(method).intercept(notificationRemoveHooker)
                }.onSuccess { log("Hooked NotifCollection.${method.name}") }
            }
    }

    private fun wireCallbacks() {
        DownloadProgressHooker.onProgressChanged = { progress ->
            indicatorView?.let { it.post { it.progress = progress } }
        }
        DownloadProgressHooker.onDownloadComplete = {
            triggerHapticFeedback()
            indicatorView?.let { it.post { it.progress = 100 } }
        }
        DownloadProgressHooker.onDownloadCancelled = {
            indicatorView?.let { it.post { it.showError() } }
        }
        DownloadProgressHooker.onActiveCountChanged = { count ->
            indicatorView?.let { it.post { it.activeDownloadCount = count } }
        }
        DownloadProgressHooker.onFilenameChanged = { filename ->
            indicatorView?.let { it.post { it.currentFilename = filename } }
        }
        DownloadProgressHooker.onPackageChanged = { packageName ->
            indicatorView?.let { it.post { it.currentPackageName = packageName } }
        }
        DownloadProgressHooker.onActivity = {
            indicatorView?.let { it.post { it.touchActivity() } }
        }

        PrefsManager.onAppVisibilityChanged = { visible ->
            indicatorView?.let { it.post { it.appVisible = visible } }
        }
        PrefsManager.onTestProgressChanged = { progress ->
            indicatorView?.let { it.post { it.progress = progress } }
        }
        PrefsManager.onPreviewTriggered = {
            indicatorView?.let { it.post { it.startDynamicPreviewAnim() } }
        }
        PrefsManager.onGeometryPreviewTriggered = {
            // Keep preview persistent if calibration screen is open
            val autoHide = !PrefsManager.persistentPreviewActive
            indicatorView?.let { it.post { it.showStaticPreviewAnim(autoHide) } }
        }
        PrefsManager.onDownloadComplete = { triggerHapticFeedback() }
        PrefsManager.onTestErrorChanged = { isError ->
            if (isError) indicatorView?.let { it.post { it.showError() } }
        }
        PrefsManager.onClearDownloadsTriggered = {
            DownloadProgressHooker.clearActiveDownloads()
        }
        PrefsManager.onPersistentPreviewChanged = { enabled ->
            indicatorView?.let { view ->
                view.post {
                    if (enabled) {
                        view.showStaticPreviewAnim(autoHide = false)
                    } else {
                        view.cancelStaticPreviewAnim()
                    }
                }
            }
        }
    }

    fun markAttached(
        view: IndicatorView,
        context: Context,
    ) {
        attached = true
        indicatorView = view
        systemUIContext = context
        registerPowerSaveReceiver(context)
    }

    private fun registerPowerSaveReceiver(context: Context) {
        if (powerSaveReceiver != null) return

        powerSaveReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    val isPowerSave =
                        ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
                    indicatorView?.let { it.post { it.isPowerSaveActive = isPowerSave } }
                }
            }

        runCatching {
            context.registerReceiver(
                powerSaveReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            )
            indicatorView?.isPowerSaveActive =
                context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        }.onFailure { log("Failed to register power save receiver", it) }
    }

    private fun triggerHapticFeedback() {
        if (!PrefsManager.hooksFeedback) return
        val view = indicatorView ?: return
        view.post {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
        }
    }

    fun isAttached(): Boolean = attached

    fun detach() {
        powerSaveReceiver?.let { receiver ->
            runCatching {
                indicatorView?.context?.unregisterReceiver(receiver)
            }.onFailure { log("Failed to unregister power save receiver", it) }
        }
        powerSaveReceiver = null
        indicatorView = null

        DownloadProgressHooker.onProgressChanged = null
        DownloadProgressHooker.onDownloadComplete = null
        DownloadProgressHooker.onDownloadCancelled = null
        DownloadProgressHooker.onActiveCountChanged = null
        DownloadProgressHooker.onFilenameChanged = null
        DownloadProgressHooker.onPackageChanged = null
        DownloadProgressHooker.onActivity = null

        PrefsManager.onAppVisibilityChanged = null
        PrefsManager.onTestProgressChanged = null
        PrefsManager.onPreviewTriggered = null
        PrefsManager.onGeometryPreviewTriggered = null
        PrefsManager.onDownloadComplete = null
        PrefsManager.onTestErrorChanged = null
        PrefsManager.onClearDownloadsTriggered = null
        PrefsManager.onPersistentPreviewChanged = null

        attached = false
    }

    private val notificationAddHooker =
        XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            logDebug { "NotificationAdd: ${chain.args.size} args" }
            chain.args.forEach {
                DownloadProgressHooker.processNotificationArg(
                    it,
                    DownloadProgressHooker::processNotification,
                )
            }
            result
        }

    private val notificationRemoveHooker =
        XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            val args = chain.args
            logDebug { "NotificationRemove: ${args.size} args" }
            val reason = extractRemovalReason(args)
            args.forEach { arg ->
                if (arg == null) return@forEach
                when {
                    DownloadProgressHooker.isStatusBarNotification(arg) -> {
                        DownloadProgressHooker.onNotificationRemoved(arg, reason)
                    }

                    arg.javaClass.name.contains("NotificationEntry") -> {
                        runCatching {
                            arg.javaClass.accessibleField("mSbn").get(arg)
                        }.getOrNull()?.let {
                            DownloadProgressHooker.onNotificationRemoved(
                                it,
                                reason,
                            )
                        }
                    }
                }
            }
            result
        }

    private fun extractRemovalReason(args: List<Any?>): Int {
        // Android 12-15: onNotificationRemoved(sbn, ranking, int_reason)
        if (args.size >= 3) (args[2] as? Int)?.let { return it }
        // Android 16+: tryRemoveNotification(NotificationEntry)
        args.firstOrNull()?.let { entry ->
            if (entry.javaClass.name.contains("NotificationEntry")) {
                runCatching {
                    entry.javaClass.accessibleField("mCancellationReason").getInt(entry)
                }.getOrNull()?.let { return it }
            }
        }
        return -1
    }
}
