package eu.hxreborn.phdp.util

import android.util.Log
import eu.hxreborn.phdp.BuildConfig
import io.github.libxposed.api.XposedModule

object Logger {
    @PublishedApi
    internal val tag = BuildConfig.APPLICATION_ID

    @Volatile
    @PublishedApi
    internal var module: XposedModule? = null

    fun attach(module: XposedModule) {
        this.module = module
    }

    fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        val m = module ?: return
        throwable?.let { m.log(message, it) } ?: m.log(message)
    }

    inline fun logDebug(message: () -> String) {
        if (!BuildConfig.DEBUG) return
        val msg = message()
        module?.log(msg)
        Log.d(tag, msg)
    }
}

fun log(
    message: String,
    throwable: Throwable? = null,
): Unit = Logger.log(message, throwable)

inline fun logDebug(message: () -> String): Unit = Logger.logDebug(message)
