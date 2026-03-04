package eu.hxreborn.phdp

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

class PHDPApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        XposedServiceHelper.registerListener(
            object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(svc: XposedService) {
                    service = svc
                    listeners.forEach { it.onServiceBind(svc) }
                }

                override fun onServiceDied(svc: XposedService) {
                    service = null
                    listeners.forEach { it.onServiceDied(svc) }
                }
            },
        )
    }

    companion object {
        lateinit var instance: PHDPApp
            private set

        var service: XposedService? = null
            private set

        private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

        fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.add(listener)
            service?.let { listener.onServiceBind(it) }
        }

        fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
            listeners.remove(listener)
        }
    }
}
