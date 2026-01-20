package eu.hxreborn.phdp.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.xposed.PHDPModule.Companion.log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

private const val CENTRAL_SURFACES_IMPL = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
private const val NOTIF_COLLECTION =
    "com.android.systemui.statusbar.notification.collection.NotifCollection"

object SystemUIHooker {
    @Volatile
    private var attached = false

    @Volatile
    private var indicatorView: IndicatorView? = null

    @Volatile
    private var powerSaveReceiver: BroadcastReceiver? = null

    fun hook(classLoader: ClassLoader) {
        hookCentralSurfaces(classLoader)
        hookNotifications(classLoader)
    }

    // Earliest reliable SystemUI context for overlay attach is CentralSurfacesImpl.start
    private fun hookCentralSurfaces(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(CENTRAL_SURFACES_IMPL) }
                .onFailure {
                    log(
                        "Failed to load $CENTRAL_SURFACES_IMPL",
                        it,
                    )
                }.getOrNull() ?: return

        val startMethod =
            targetClass.declaredMethods.find { it.name == "start" && it.parameterCount == 0 }
        if (startMethod == null) {
            log("start() not found in $CENTRAL_SURFACES_IMPL")
            return
        }

        runCatching {
            module.hook(
                startMethod,
                StartHooker::class.java,
            )
        }.onSuccess { log("Hooked CentralSurfacesImpl.start()") }
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
                module.hook(
                    method,
                    NotificationAddHooker::class.java,
                )
            }.onSuccess { log("Hooked NotifCollection.postNotification") }
        }

        // Hook notification removal - method name varies by Android version
        // Android 16+: tryRemoveNotification(NotificationEntry)
        // Android 12-15: onNotificationRemoved(StatusBarNotification, RankingMap, int)
        targetClass.declaredMethods
            .filter { it.name == "tryRemoveNotification" || it.name == "onNotificationRemoved" }
            .forEach { method ->
                runCatching {
                    module.hook(
                        method,
                        NotificationRemoveHooker::class.java,
                    )
                }.onSuccess { log("Hooked NotifCollection.${method.name}") }
            }

        wireCallbacks()
    }

    private fun wireCallbacks() {
        DownloadProgressHooker.onProgressChanged = { progress ->
            indicatorView?.let { it.post { it.progress = progress } }
        }
        DownloadProgressHooker.onDownloadComplete = { triggerHapticFeedback() }
        DownloadProgressHooker.onDownloadCancelled = {
            indicatorView?.let { it.post { it.showError() } }
        }
        DownloadProgressHooker.onActiveCountChanged = { count ->
            indicatorView?.let { it.post { it.activeDownloadCount = count } }
        }
        DownloadProgressHooker.onFilenameChanged = { filename ->
            indicatorView?.let { it.post { it.currentFilename = filename } }
        }

        PrefsManager.onAppVisibilityChanged = { visible ->
            indicatorView?.let { it.post { it.appVisible = visible } }
        }
        PrefsManager.onTestProgressChanged = { progress ->
            indicatorView?.let { it.post { it.progress = progress } }
        }
        PrefsManager.onPreviewTriggered = {
            indicatorView?.let { it.post { it.startPreview() } }
        }
        PrefsManager.onGeometryPreviewTriggered = {
            indicatorView?.let { it.post { it.showGeometryPreview() } }
        }
        PrefsManager.onDownloadComplete = { triggerHapticFeedback() }
        PrefsManager.onTestErrorChanged = { isError ->
            if (isError) indicatorView?.let { it.post { it.showError() } }
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
        if (!PrefsManager.hooksFeedback) return
        val context = indicatorView?.context ?: return
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) vibrator.vibrate(createHapticEffect())
        }
    }

    private fun createHapticEffect(): VibrationEffect = VibrationEffect.createOneShot(40, 150)

    fun isAttached(): Boolean = attached

    fun detach() {
        powerSaveReceiver?.let { receiver ->
            runCatching { indicatorView?.context?.unregisterReceiver(receiver) }.onFailure {
                log(
                    "Failed to unregister power save receiver",
                    it,
                )
            }
        }
        powerSaveReceiver = null
        indicatorView = null

        DownloadProgressHooker.onProgressChanged = null
        DownloadProgressHooker.onDownloadComplete = null
        DownloadProgressHooker.onDownloadCancelled = null
        DownloadProgressHooker.onActiveCountChanged = null
        DownloadProgressHooker.onFilenameChanged = null

        PrefsManager.onAppVisibilityChanged = null
        PrefsManager.onTestProgressChanged = null
        PrefsManager.onPreviewTriggered = null
        PrefsManager.onGeometryPreviewTriggered = null
        PrefsManager.onDownloadComplete = null
        PrefsManager.onTestErrorChanged = null

        attached = false
    }
}

@XposedHooker
class StartHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            if (SystemUIHooker.isAttached()) return

            val instance = callback.thisObject ?: return
            val context =
                runCatching {
                    instance.javaClass.accessibleField("mContext").get(instance) as? Context
                }.getOrNull()

            if (context == null) {
                log("Failed to extract Context from CentralSurfacesImpl")
                return
            }

            runCatching {
                val view = IndicatorView.attach(context)
                SystemUIHooker.markAttached(view, context)
                log("IndicatorView attached")
            }.onFailure { log("Failed to attach IndicatorView", it) }
        }
    }
}
