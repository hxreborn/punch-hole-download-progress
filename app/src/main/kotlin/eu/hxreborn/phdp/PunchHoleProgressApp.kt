package eu.hxreborn.phdp

import android.app.Application
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

class PunchHoleProgressApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        log("onCreate - registering listener")
        XposedServiceHelper.registerListener(
            object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(svc: XposedService) {
                    log("onServiceBind: $svc")
                    service = svc
                    listeners.forEach { it.onServiceBind(svc) }
                }

                override fun onServiceDied(svc: XposedService) {
                    log("onServiceDied")
                    service = null
                    listeners.forEach { it.onServiceDied(svc) }
                }
            },
        )
    }

    companion object {
        private const val TAG = "PHDP"

        lateinit var instance: PunchHoleProgressApp
            private set

        var service: XposedService? = null
            private set

        private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

        private fun log(msg: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, msg)
        }

        fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.add(listener)
            service?.let { listener.onServiceBind(it) }
        }

        fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.remove(listener)
        }
    }
}
