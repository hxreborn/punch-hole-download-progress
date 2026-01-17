package eu.hxreborn.phpm

import android.os.Build
import android.util.Log
import eu.hxreborn.phpm.hook.SystemUIHook
import eu.hxreborn.phpm.prefs.PrefsManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

internal lateinit var module: PunchHoleMonitorModule

class PunchHoleMonitorModule(
    base: XposedInterface,
    param: ModuleLoadedParam,
) : XposedModule(base, param) {
    init {
        module = this
        log("Module v${BuildConfig.VERSION_NAME} on ${base.frameworkName} ${base.frameworkVersion}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != SYSTEMUI_PACKAGE || !param.isFirstPackage) return

        if (BuildConfig.DEBUG) {
            log("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})")
            log("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), ROM: ${Build.DISPLAY}")
        }

        PrefsManager.init(this)
        runCatching { SystemUIHook.hook(param.classLoader) }
            .onSuccess { log("Hooks registered") }
            .onFailure { log("Hook failed", it) }
    }

    companion object {
        private const val TAG = "PHPM"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        fun log(
            msg: String,
            t: Throwable? = null,
        ) {
            if (BuildConfig.DEBUG) {
                if (t != null) Log.d(TAG, msg, t) else Log.d(TAG, msg)
            }
            if (::module.isInitialized) {
                if (t != null) module.log(msg, t) else module.log(msg)
            }
        }
    }
}
