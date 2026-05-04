package eu.hxreborn.phdp.util

import android.util.Log
import io.github.libxposed.api.XposedModule

object Logger {
    @PublishedApi
    internal const val TAG = "PHDP"

    @Volatile
    @PublishedApi
    internal var module: XposedModule? = null

    @Volatile
    @PublishedApi
    internal var verboseEnabled = false

    fun attach(module: XposedModule) {
        this.module = module
    }

    fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        val module = module ?: return

        throwable?.let {
            module.log(Log.ERROR, TAG, message, it)
            return
        }

        module.log(Log.INFO, TAG, message)
    }

    inline fun logDebug(message: () -> String) {
        if (!verboseEnabled) return
        module?.log(Log.DEBUG, TAG, message())
    }
}

fun log(
    message: String,
    throwable: Throwable? = null,
): Unit = Logger.log(message, throwable)

inline fun logDebug(message: () -> String): Unit = Logger.logDebug(message)
