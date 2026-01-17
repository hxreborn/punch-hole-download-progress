package eu.hxreborn.phpm.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import eu.hxreborn.phpm.PunchHoleMonitorModule.Companion.log
import eu.hxreborn.phpm.module
import eu.hxreborn.phpm.prefs.PrefsManager
import eu.hxreborn.phpm.util.accessibleField
import eu.hxreborn.phpm.view.IndicatorView
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker

private const val CENTRAL_SURFACES_IMPL = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
private const val NOTIF_COLLECTION =
    "com.android.systemui.statusbar.notification.collection.NotifCollection"

object SystemUIHook {
    @Volatile
    private var attached = false

    @Volatile
    private var indicatorView: IndicatorView? = null

    @Volatile
    private var systemUIContext: Context? = null

    @Volatile
    private var powerSaveReceiver: BroadcastReceiver? = null

    fun hook(classLoader: ClassLoader) {
        hookCentralSurfaces(classLoader)
        hookNotifications(classLoader)
    }

    // CentralSurfacesImpl.start() is the earliest point where we can get SystemUI context
    private fun hookCentralSurfaces(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(CENTRAL_SURFACES_IMPL) }
                .onFailure { log("Failed to load $CENTRAL_SURFACES_IMPL", it) }
                .getOrNull() ?: return

        val startMethod =
            targetClass.declaredMethods.find { it.name == "start" && it.parameterCount == 0 }
        if (startMethod == null) {
            log("start() not found in $CENTRAL_SURFACES_IMPL")
            return
        }

        runCatching { module.hook(startMethod, StartHooker::class.java) }
            .onSuccess { log("Hooked CentralSurfacesImpl.start()") }
            .onFailure { log("Hook failed", it) }
    }

    private fun hookNotifications(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(NOTIF_COLLECTION) }
                .getOrNull() ?: return

        // postNotification: entry point for new notifications on Android 12+
        targetClass.declaredMethods.filter { it.name == "postNotification" }.forEach { method ->
            runCatching { module.hook(method, NotificationAddHooker::class.java) }
                .onSuccess { log("Hooked NotifCollection.postNotification") }
        }

        // retractNotification: called when notifications are dismissed or cancelled
        targetClass.declaredMethods.filter { it.name == "retractNotification" }.forEach { method ->
            runCatching { module.hook(method, NotificationRemoveHooker::class.java) }
                .onSuccess { log("Hooked NotifCollection.retractNotification") }
        }

        wireCallbacks()
    }

    private fun wireCallbacks() {
        DownloadProgressHook.onProgressChanged = { progress ->
            indicatorView?.post { indicatorView?.progress = progress }
        }
        DownloadProgressHook.onDownloadComplete = { triggerHapticFeedback() }
        DownloadProgressHook.onDownloadCancelled =
            { indicatorView?.post { indicatorView?.showError() } }
        DownloadProgressHook.onActiveCountChanged = { count ->
            indicatorView?.post { indicatorView?.activeDownloadCount = count }
        }

        PrefsManager.onAppVisibilityChanged = { visible ->
            indicatorView?.post { indicatorView?.appVisible = visible }
        }
        PrefsManager.onTestModeChanged = { inTestMode ->
            indicatorView?.post { indicatorView?.testMode = inTestMode }
        }
        PrefsManager.onTestProgressChanged = { progress ->
            indicatorView?.post { indicatorView?.progress = progress }
        }
        PrefsManager.onPreviewTriggered = {
            indicatorView?.post { indicatorView?.startPreview() }
        }
        PrefsManager.onGeometryPreviewTriggered = {
            indicatorView?.post { indicatorView?.showGeometryPreview() }
        }
        PrefsManager.onDownloadComplete = { triggerHapticFeedback() }
        PrefsManager.onTestErrorChanged = { isError ->
            if (isError) indicatorView?.post { indicatorView?.showError() }
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
                    indicatorView?.post { indicatorView?.isPowerSaveActive = isPowerSave }
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
        val context = systemUIContext ?: return
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) vibrator.vibrate(createHapticEffect())
        }
    }

    private fun createHapticEffect(): VibrationEffect {
        val amplitude =
            when (PrefsManager.hapticStrength) {
                "low" -> 80
                "high" -> 255
                else -> 150
            }
        return when (PrefsManager.hapticPattern) {
            "tick" -> {
                VibrationEffect.createOneShot(20, amplitude)
            }

            "double_click" -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 50, 30),
                    intArrayOf(0, amplitude, 0, amplitude),
                    -1,
                )
            }

            "heavy" -> {
                VibrationEffect.createOneShot(80, amplitude)
            }

            "long" -> {
                VibrationEffect.createOneShot(150, amplitude)
            }

            "pulse" -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 30, 40, 30, 40),
                    intArrayOf(
                        0,
                        amplitude,
                        0,
                        (amplitude * 0.7).toInt(),
                        0,
                        (amplitude * 0.4).toInt(),
                    ),
                    -1,
                )
            }

            else -> {
                VibrationEffect.createOneShot(40, amplitude)
            }
        }
    }

    fun isAttached(): Boolean = attached

    fun getIndicatorView(): IndicatorView? = indicatorView

    fun detach() {
        powerSaveReceiver?.let { receiver ->
            runCatching { systemUIContext?.unregisterReceiver(receiver) }
                .onFailure { log("Failed to unregister power save receiver", it) }
        }
        powerSaveReceiver = null
        systemUIContext = null
        indicatorView = null

        DownloadProgressHook.onProgressChanged = null
        DownloadProgressHook.onDownloadComplete = null
        DownloadProgressHook.onDownloadCancelled = null
        DownloadProgressHook.onActiveCountChanged = null

        PrefsManager.onAppVisibilityChanged = null
        PrefsManager.onTestModeChanged = null
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
            if (SystemUIHook.isAttached()) return

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
                SystemUIHook.markAttached(view, context)
                log("IndicatorView attached")
            }.onFailure { log("Failed to attach IndicatorView", it) }
        }
    }
}
