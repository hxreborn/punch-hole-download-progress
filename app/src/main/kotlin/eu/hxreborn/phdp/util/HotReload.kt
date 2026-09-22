package eu.hxreborn.phdp.util

import io.github.libxposed.service.XposedService

object HotReload {
    fun isSupported(service: XposedService): Boolean = service.apiVersion >= XposedService.API_102

    fun reload(
        service: XposedService,
        onResult: (String) -> Unit,
    ) {
        if (service.apiVersion < XposedService.API_102) return
        val targets = runCatching { service.runningTargets }.getOrNull().orEmpty()
        if (targets.isEmpty()) {
            onResult("no hooked targets running")
            return
        }
        targets.forEach { target ->
            val name = "${target.processName} (${target.state.name.lowercase()})"
            runCatching {
                service.hotReloadModule(target, null) { _, result ->
                    val message = result.message()?.let { " - $it" }.orEmpty()
                    onResult("$name: ${result.status().name.lowercase()}$message")
                }
            }.onFailure { onResult("$name: reload failed - ${it.message ?: it}") }
        }
    }
}
