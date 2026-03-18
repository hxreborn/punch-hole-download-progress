package eu.hxreborn.phdp.util

import android.util.Log
import eu.hxreborn.phdp.BuildConfig
import io.github.libxposed.api.XposedModule

object Logger {
    @PublishedApi
    internal const val TAG = "PHDP"

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
        if (throwable != null) {
            m.log(Log.ERROR, TAG, message, throwable)
        } else {
            m.log(Log.INFO, TAG, message)
        }
    }

    inline fun logDebug(message: () -> String) {
        if (!BuildConfig.DEBUG) return
        val msg = message()
        module?.log(Log.DEBUG, TAG, msg)
        Log.d(TAG, msg)
    }
}

fun log(
    message: String,
    throwable: Throwable? = null,
): Unit = Logger.log(message, throwable)

inline fun logDebug(message: () -> String): Unit = Logger.logDebug(message)
