package eu.hxreborn.phdp.ui.state

enum class SystemUiHook { NOT_IN_SCOPE, NOT_LOADED, OUTDATED, FAILED, RUNNING, IN_SCOPE }

sealed interface ModuleStatus {
    data object Checking : ModuleStatus

    data object Inactive : ModuleStatus

    data class Active(
        val frameworkName: String,
        val frameworkVersion: String,
        val apiVersion: Int,
        val systemUi: SystemUiHook,
    ) : ModuleStatus
}
