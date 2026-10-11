package eu.hxreborn.phdp.xposed.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import eu.hxreborn.phdp.prefs.SettingsPush
import eu.hxreborn.phdp.util.accessibleField
import eu.hxreborn.phdp.util.accessibleFieldFromHierarchy
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.util.logDebug
import eu.hxreborn.phdp.xposed.indicator.IndicatorView
import eu.hxreborn.phdp.xposed.module
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val SYSTEMUI_PKG = "com.android.systemui"
private const val CENTRAL_SURFACES_IMPL = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
private const val STATUS_BAR = "com.android.systemui.statusbar.phone.StatusBar"
private const val NOTIF_COLLECTION =
    "com.android.systemui.statusbar.notification.collection.NotifCollection"
private const val NOTIFICATION_LISTENER = "com.android.systemui.statusbar.NotificationListener"
private const val REMOVE_TIMEOUT_MS = 1000L
internal const val SAVED_CONTEXT = "context"

// Scoped to process lifetime, cannot leak
@SuppressLint("StaticFieldLeak")
object SystemUIHook {
    @Volatile
    private var attached = false

    @Volatile
    private var indicatorView: IndicatorView? = null

    @Volatile
    private var powerSaveReceiver: BroadcastReceiver? = null

    @Volatile
    private var settingsPushReceiver: BroadcastReceiver? = null

    @Volatile
    private var systemUIContext: Context? = null

    private val cancellationReasonField = ConcurrentHashMap<Class<*>, Optional<Field>>()

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
                    log("load failed pkg=$SYSTEMUI_PKG class=$NOTIFICATION_LISTENER", it)
                }.getOrNull() ?: return

        targetClass.declaredMethods.filter { it.name == "onNotificationPosted" }.forEach { method ->
            runCatching {
                module.hook(method).intercept(notificationAddHooker)
            }.onSuccess {
                log("hooked notif-post pkg=$SYSTEMUI_PKG class=NotificationListener")
            }
        }
    }

    // Earliest reliable SystemUI context for overlay attach
    // CentralSurfacesImpl (13+), StatusBar (9-12L)
    private fun hookSystemUIEntry(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(CENTRAL_SURFACES_IMPL) }
                .recoverCatching {
                    classLoader.loadClass(STATUS_BAR)
                }.onFailure {
                    log("load failed pkg=$SYSTEMUI_PKG class=CentralSurfaces|StatusBar", it)
                }.getOrNull()
                ?: return

        val startMethod =
            targetClass.declaredMethods.find { it.name == "start" && it.parameterCount == 0 }
        if (startMethod == null) {
            log("lookup failed pkg=$SYSTEMUI_PKG class=${targetClass.name} method=start")
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
                    log(
                        "extract failed pkg=$SYSTEMUI_PKG class=${instance.javaClass.simpleName} field=mContext",
                    )
                    return@intercept result
                }
                runCatching {
                    systemUIContext = context
                    DuoDownloadPublisher.attach(context)
                    val view = IndicatorView.attach(context)
                    markAttached(view, context)
                    log("attached overlay pkg=$SYSTEMUI_PKG")
                }.onFailure { log("attach failed pkg=$SYSTEMUI_PKG target=overlay", it) }
                result
            }
        }.onSuccess {
            log(
                "hooked entry pkg=$SYSTEMUI_PKG class=${targetClass.simpleName} method=start",
            )
        }.onFailure {
            log(
                "hook failed pkg=$SYSTEMUI_PKG class=${targetClass.simpleName} method=start",
                it,
            )
        }
    }

    private fun hookNotifications(classLoader: ClassLoader) {
        val targetClass =
            runCatching { classLoader.loadClass(NOTIF_COLLECTION) }
                .onFailure {
                    log("load failed pkg=$SYSTEMUI_PKG class=$NOTIF_COLLECTION", it)
                }.getOrNull() ?: return

        // postNotification entry point for new notifications on Android 12 and later
        targetClass.declaredMethods.filter { it.name == "postNotification" }.forEach { method ->
            runCatching {
                module.hook(method).intercept(notificationAddHooker)
            }.onSuccess {
                log("hooked notif-post pkg=$SYSTEMUI_PKG class=NotifCollection")
            }
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
                }.onSuccess {
                    log(
                        "hooked notif-remove pkg=$SYSTEMUI_PKG class=NotifCollection method=${method.name}",
                    )
                }
            }
    }

    // hops to the view UI thread and no-ops until the overlay attaches
    private fun onView(block: IndicatorView.() -> Unit) {
        indicatorView?.let { view -> view.post { view.block() } }
    }

    private fun wireCallbacks() {
        DownloadProgressHook.onProgressChanged = { progress ->
            log(
                "download progress route progress=$progress " +
                    "duoMode=${IndicatorState.duoStatusBarMode}",
            )
            if (IndicatorState.duoStatusBarMode) {
                DuoDownloadPublisher.onProgress(progress)
            } else {
                onView { this.progress = progress }
            }
        }
        DownloadProgressHook.onDownloadComplete = {
            triggerHapticFeedback()
            log("download complete route duoMode=${IndicatorState.duoStatusBarMode}")
            if (IndicatorState.duoStatusBarMode) {
                DuoDownloadPublisher.onComplete()
            } else {
                onView { progress = 100 }
            }
        }
        DownloadProgressHook.onDownloadCancelled = {
            log("download cancelled route duoMode=${IndicatorState.duoStatusBarMode}")
            if (IndicatorState.duoStatusBarMode) {
                DuoDownloadPublisher.onFailed()
            } else {
                onView { showError() }
            }
        }
        DownloadProgressHook.onActiveCountChanged = { count ->
            onView { activeDownloadCount = count }
            if (count > 0) NetworkSpeedMonitor.start() else NetworkSpeedMonitor.stop()
        }
        DownloadProgressHook.onFilenameChanged =
            { filename -> onView { currentFilename = filename } }
        DownloadProgressHook.onPackageChanged =
            { packageName -> onView { currentPackageName = packageName } }
        DownloadProgressHook.onActivity = {
            onView { touchActivity() }
            DuoDownloadPublisher.onActivity()
        }
        NetworkSpeedMonitor.onSpeedChanged = { speed -> onView { currentSpeed = speed } }

        IndicatorState.onAppVisibilityChanged = { visible -> onView { appVisible = visible } }
        IndicatorState.onTestProgressChanged = { progress ->
            log("test progress route progress=$progress duoMode=${IndicatorState.duoStatusBarMode}")
            if (IndicatorState.duoStatusBarMode) {
                DuoDownloadPublisher.onProgress(progress)
            } else {
                onView { this.progress = progress }
            }
        }
        IndicatorState.onPreviewTriggered = { onView { startDynamicPreviewAnim() } }
        IndicatorState.onGeometryPreviewTriggered = {
            // Keep preview persistent if calibration screen is open
            val autoHide = !IndicatorState.persistentPreviewActive
            if (IndicatorState.duoStatusBarMode) {
                onView { cancelStaticPreviewAnim() }
                DuoDownloadPublisher.showStaticPreview(autoHide)
            } else {
                DuoDownloadPublisher.cancelStaticPreview()
                onView { showStaticPreviewAnim(autoHide) }
            }
        }
        IndicatorState.onDownloadComplete = { triggerHapticFeedback() }
        IndicatorState.onTestErrorChanged = { isError ->
            if (isError) {
                if (IndicatorState.duoStatusBarMode) {
                    DuoDownloadPublisher.onFailed()
                } else {
                    onView { showError() }
                }
            }
        }
        IndicatorState.onDuoModeChanged = { enabled ->
            log("duo mode callback enabled=$enabled ctxPkg=${systemUIContext?.packageName}")
            applyDuoMode(enabled)
        }
        IndicatorState.onPrefsChanged = { DuoDownloadPublisher.refresh() }
        IndicatorState.onClearDownloadsTriggered = { DownloadProgressHook.clearActiveDownloads() }
        IndicatorState.onPersistentPreviewChanged = { enabled ->
            if (IndicatorState.duoStatusBarMode) {
                onView { cancelStaticPreviewAnim() }
                if (enabled) {
                    DuoDownloadPublisher.showStaticPreview(autoHide = false)
                } else {
                    DuoDownloadPublisher.cancelStaticPreview()
                }
            } else {
                DuoDownloadPublisher.cancelStaticPreview()
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
    }

    fun markAttached(
        view: IndicatorView,
        context: Context,
    ) {
        attached = true
        indicatorView = view
        systemUIContext = context
        NetworkSpeedMonitor.attach(context)
        registerPowerSaveReceiver(context)
        registerSettingsPushReceiver(context)
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
                    DuoDownloadPublisher.setPowerSave(isPowerSave)
                }
            }

        runCatching {
            context.registerReceiver(
                powerSaveReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            )
            val powerSave =
                context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
            indicatorView?.isPowerSaveActive = powerSave
            DuoDownloadPublisher.setPowerSave(powerSave)
        }.onFailure { log("register failed pkg=$SYSTEMUI_PKG target=power-save-receiver", it) }
    }

    private fun registerSettingsPushReceiver(context: Context) {
        if (settingsPushReceiver != null) return
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context?,
                    intent: Intent?,
                ) {
                    log(
                        "settings push received ctxPkg=${ctx?.packageName} " +
                            "key=${intent?.getStringExtra(SettingsPush.EXTRA_KEY)}",
                    )
                    IndicatorState.dispatchPushedPref(intent)
                }
            }
        settingsPushReceiver = receiver
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(SettingsPush.ACTION),
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                context.registerReceiver(receiver, IntentFilter(SettingsPush.ACTION))
            }
            log("settings push receiver registered ctxPkg=${context.packageName}")
        }.onFailure { log("register failed pkg=$SYSTEMUI_PKG target=settings-push", it) }
    }

    private fun triggerHapticFeedback() {
        if (!IndicatorState.hooksFeedback) return
        onView { ViewCompat.performHapticFeedback(this, HapticFeedbackConstantsCompat.CONFIRM) }
    }

    fun hotReloadState(): HashMap<String, Any?>? {
        val context = systemUIContext ?: return null
        return hashMapOf(SAVED_CONTEXT to context)
    }

    fun prepareHotReload() {
        NetworkSpeedMonitor.shutdown()
        val view = indicatorView ?: return
        val removed = CountDownLatch(1)
        view.post {
            view.removeFromWindow()
            removed.countDown()
        }
        if (!removed.await(REMOVE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            log("remove timed out pkg=$SYSTEMUI_PKG target=overlay")
        }
    }

    fun rehook(context: Context) {
        hook(context.classLoader)
        Handler(Looper.getMainLooper()).post {
            runCatching {
                systemUIContext = context
                DuoDownloadPublisher.attach(context)
                val view = IndicatorView.attach(context)
                markAttached(view, context)
                log("attached overlay pkg=$SYSTEMUI_PKG source=hot-reload")
            }.onFailure { log("attach failed pkg=$SYSTEMUI_PKG target=overlay", it) }
        }
    }

    fun isAttached(): Boolean = attached

    fun detach() {
        val receiverContext = indicatorView?.context ?: systemUIContext
        powerSaveReceiver?.let { receiver ->
            runCatching {
                receiverContext?.unregisterReceiver(receiver)
            }.onFailure {
                log(
                    "unregister failed pkg=$SYSTEMUI_PKG target=power-save-receiver",
                    it,
                )
            }
        }
        powerSaveReceiver = null
        settingsPushReceiver?.let { receiver ->
            runCatching {
                receiverContext?.unregisterReceiver(receiver)
            }.onFailure {
                log(
                    "unregister failed pkg=$SYSTEMUI_PKG target=settings-push",
                    it,
                )
            }
        }
        settingsPushReceiver = null
        indicatorView = null

        DownloadProgressHook.onProgressChanged = null
        DownloadProgressHook.onDownloadComplete = null
        DownloadProgressHook.onDownloadCancelled = null
        DownloadProgressHook.onActiveCountChanged = null
        DownloadProgressHook.onFilenameChanged = null
        DownloadProgressHook.onPackageChanged = null
        DownloadProgressHook.onActivity = null

        NetworkSpeedMonitor.stop()
        NetworkSpeedMonitor.onSpeedChanged = null

        IndicatorState.onAppVisibilityChanged = null
        IndicatorState.onTestProgressChanged = null
        IndicatorState.onPreviewTriggered = null
        IndicatorState.onGeometryPreviewTriggered = null
        IndicatorState.onDownloadComplete = null
        IndicatorState.onTestErrorChanged = null
        IndicatorState.onClearDownloadsTriggered = null
        IndicatorState.onPersistentPreviewChanged = null
        IndicatorState.onDuoModeChanged = null
        IndicatorState.onPrefsChanged = null
        DuoDownloadPublisher.detach()
        systemUIContext = null

        attached = false
    }

    private fun applyDuoMode(enabled: Boolean) {
        val context = systemUIContext ?: return
        log(
            "apply duo mode enabled=$enabled ctxPkg=${context.packageName} " +
                "hasView=${indicatorView != null} activeDownloads=${DownloadProgressHook.hasActiveDownloads()}",
        )
        if (indicatorView == null) {
            runCatching {
                val view = IndicatorView.attach(context)
                markAttached(view, context)
                log("attached overlay pkg=$SYSTEMUI_PKG")
            }.onFailure { log("attach failed pkg=$SYSTEMUI_PKG target=overlay", it) }
        } else {
            indicatorView?.post {
                if (enabled) indicatorView?.cancelStaticPreviewAnim()
                indicatorView?.visibility = View.VISIBLE
                indicatorView?.invalidate()
            }
        }
        if (!enabled && DownloadProgressHook.hasActiveDownloads()) {
            val progress = DownloadProgressHook.currentAverage()
            onView { this.progress = progress }
            NetworkSpeedMonitor.start()
        }
        DuoDownloadPublisher.refresh()
        if (enabled && IndicatorState.persistentPreviewActive) {
            DuoDownloadPublisher.showStaticPreview(autoHide = false)
        } else if (!enabled) {
            DuoDownloadPublisher.cancelStaticPreview()
        }
    }

    private val notificationAddHooker =
        XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            val args = chain.args
            logDebug { "intercept notif-add pkg=$SYSTEMUI_PKG args=${args.size}" }
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
            logDebug { "intercept notif-remove pkg=$SYSTEMUI_PKG args=${args.size}" }
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
        val entry = args.firstOrNull() ?: return -1
        if (!entry.javaClass.name.contains("NotificationEntry")) return -1
        val field =
            cancellationReasonField
                .getOrPut(entry.javaClass) {
                    Optional.ofNullable(
                        runCatching {
                            entry.javaClass.accessibleField(
                                "mCancellationReason",
                            )
                        }.getOrNull(),
                    )
                }.orElse(null) ?: return -1
        return runCatching { field.getInt(entry) }.getOrDefault(-1)
    }
}
