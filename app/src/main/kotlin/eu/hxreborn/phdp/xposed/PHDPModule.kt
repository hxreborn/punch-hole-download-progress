package eu.hxreborn.phdp.xposed

import android.os.Build
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.util.Logger
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.xposed.hook.IndicatorState
import eu.hxreborn.phdp.xposed.hook.SystemUIHook
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

@PublishedApi
internal lateinit var module: PHDPModule

class PHDPModule : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
        Logger.attach(this)
        log("Module v${BuildConfig.VERSION_NAME} on $frameworkName $frameworkVersion")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != SYSTEMUI_PACKAGE || !param.isFirstPackage) return

        log("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        IndicatorState.init(this)
        runCatching {
            SystemUIHook.hook(param.classLoader)
        }.onSuccess { log("Hooks registered") }.onFailure { log("Hook failed", it) }
    }

    companion object {
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"
    }
}
