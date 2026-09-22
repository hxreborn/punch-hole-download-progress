package eu.hxreborn.phdp.xposed

import android.content.Context
import android.os.Build
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.util.Logger
import eu.hxreborn.phdp.util.log
import eu.hxreborn.phdp.xposed.hook.IndicatorState
import eu.hxreborn.phdp.xposed.hook.SAVED_CONTEXT
import eu.hxreborn.phdp.xposed.hook.SystemUIHook
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
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

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        val state = SystemUIHook.hotReloadState()
        if (state == null) {
            log("hot reload rejected pkg=$SYSTEMUI_PACKAGE reason=not-attached")
            return false
        }
        param.setSavedInstanceState(state)
        SystemUIHook.prepareHotReload()
        IndicatorState.release()
        log("hot reload preparing pkg=$SYSTEMUI_PACKAGE")
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        module = this
        Logger.attach(this)
        runCatching { super.onHotReloaded(param) }
            .onFailure { log("hot reload unhook failed pkg=$SYSTEMUI_PACKAGE", it) }
        val saved = param.savedInstanceState as? Map<*, *>
        val context = saved?.get(SAVED_CONTEXT) as? Context
        if (context == null) {
            log("hot reload aborted pkg=$SYSTEMUI_PACKAGE reason=missing-state")
            return
        }
        IndicatorState.init(this)
        SystemUIHook.rehook(context)
        log("hot reloaded v${BuildConfig.VERSION_NAME} origin=reload")
    }

    companion object {
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"
    }
}
