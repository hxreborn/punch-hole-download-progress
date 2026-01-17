package eu.hxreborn.phdp

import android.os.Build
import android.util.Log
import eu.hxreborn.phdp.hook.SystemUIHook
import eu.hxreborn.phdp.prefs.PrefsManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

internal lateinit var module: PunchHoleProgressModule

class PunchHoleProgressModule(
    base: XposedInterface,
    param: ModuleLoadedParam,
) : XposedModule(base, param) {
    init {
        module = this
        Companion.log("Module v${BuildConfig.VERSION_NAME} on ${base.frameworkName} ${base.frameworkVersion}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != SYSTEMUI_PACKAGE || !param.isFirstPackage) return

        Companion.log("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        PrefsManager.init(this)
        runCatching { SystemUIHook.hook(param.classLoader) }
            .onSuccess { Companion.log("Hooks registered") }
            .onFailure { Companion.log("Hook failed", it) }
    }

    companion object {
        private const val TAG = "PHDP"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        fun log(
            msg: String,
            t: Throwable? = null,
        ) {
            // LSPosed Manager logs
            if (t != null) module.log(msg, t) else module.log(msg)
            // ADB logcat (stripped in release by proguard)
            if (t != null) Log.d(TAG, msg, t) else Log.d(TAG, msg)
        }
    }
}
