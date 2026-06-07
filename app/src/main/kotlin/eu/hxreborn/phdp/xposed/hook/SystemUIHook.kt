package eu.hxreborn.phdp.xposed.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
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
object SystemUIHook {
    @Volatile
    private var attached = false

    @Volatile
    private var indicatorView: IndicatorView? = null

    @Volatile
    private var powerSaveReceiver: BroadcastReceiver? = null

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

    // hops to the view UI thread and no-ops until the overlay attaches
    private fun onView(block: IndicatorView.() -> Unit) {
        indicatorView?.let { view -> view.post { view.block() } }
    }

    private fun wireCallbacks() {
        DownloadProgressHook.onProgressChanged =
            { progress -> onView { this.progress = progress } }
        DownloadProgressHook.onDownloadComplete = {
            triggerHapticFeedback()
            onView { progress = 100 }
        }
        DownloadProgressHook.onDownloadCancelled = { onView { showError() } }
        DownloadProgressHook.onActiveCountChanged =
            { count -> onView { activeDownloadCount = count } }
        DownloadProgressHook.onFilenameChanged =
            { filename -> onView { currentFilename = filename } }
        DownloadProgressHook.onPackageChanged =
            { packageName -> onView { currentPackageName = packageName } }
        DownloadProgressHook.onActivity = { onView { touchActivity() } }

        IndicatorState.onAppVisibilityChanged = { visible -> onView { appVisible = visible } }
        IndicatorState.onTestProgressChanged = { progress -> onView { this.progress = progress } }
        IndicatorState.onPreviewTriggered = { onView { startDynamicPreviewAnim() } }
        IndicatorState.onGeometryPreviewTriggered = {
            // Keep preview persistent if calibration screen is open
            val autoHide = !IndicatorState.persistentPreviewActive
            onView { showStaticPreviewAnim(autoHide) }
        }
        IndicatorState.onDownloadComplete = { triggerHapticFeedback() }
        IndicatorState.onTestErrorChanged = { isError -> if (isError) onView { showError() } }
        IndicatorState.onClearDownloadsTriggered = { DownloadProgressHook.clearActiveDownloads() }
        IndicatorState.onPersistentPreviewChanged = { enabled ->
            onView {
                if (enabled) {
                    showStaticPreviewAnim(
                        autoHide = false,
                    )
                } else {
                    cancelStaticPreviewAnim()
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
        if (!IndicatorState.hooksFeedback) return
        onView { ViewCompat.performHapticFeedback(this, HapticFeedbackConstantsCompat.CONFIRM) }
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

        DownloadProgressHook.onProgressChanged = null
        DownloadProgressHook.onDownloadComplete = null
        DownloadProgressHook.onDownloadCancelled = null
        DownloadProgressHook.onActiveCountChanged = null
        DownloadProgressHook.onFilenameChanged = null
        DownloadProgressHook.onPackageChanged = null
        DownloadProgressHook.onActivity = null

        IndicatorState.onAppVisibilityChanged = null
        IndicatorState.onTestProgressChanged = null
        IndicatorState.onPreviewTriggered = null
        IndicatorState.onGeometryPreviewTriggered = null
        IndicatorState.onDownloadComplete = null
        IndicatorState.onTestErrorChanged = null
        IndicatorState.onClearDownloadsTriggered = null
        IndicatorState.onPersistentPreviewChanged = null

        attached = false
    }

    private val notificationAddHooker =
        XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            val args = chain.args
            logDebug { "NotificationAdd: ${args.size} args" }
            // indexed loop avoids an iterator alloc on every posted notification
            for (i in args.indices) {
                DownloadProgressHook.processNotificationArg(
                    args[i],
                    DownloadProgressHook::processNotification,
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
            // indexed loop avoids an iterator alloc on every removed notification
            for (i in args.indices) {
                val arg = args[i] ?: continue
                when {
                    DownloadProgressHook.isStatusBarNotification(arg) -> {
                        DownloadProgressHook.onNotificationRemoved(arg, reason)
                    }

                    arg.javaClass.name.contains("NotificationEntry") -> {
                        DownloadProgressHook.sbnFromEntry(arg)?.let {
                            DownloadProgressHook.onNotificationRemoved(it, reason)
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
