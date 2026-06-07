package eu.hxreborn.phdp

import android.app.Application
import android.content.Context
import android.util.Log
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.PrefsRepository
import eu.hxreborn.phdp.prefs.PrefsRepositoryImpl
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "PHDP"

class PHDPApp :
    Application(),
    XposedServiceHelper.OnServiceListener {
    @Volatile
    private var mService: XposedService? = null

    private val listeners = CopyOnWriteArrayList<XposedServiceHelper.OnServiceListener>()

    lateinit var prefs: PrefsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val local = getSharedPreferences(Prefs.GROUP, MODE_PRIVATE)
        prefs =
            PrefsRepositoryImpl(local) {
                runCatching { mService?.getRemotePreferences(Prefs.GROUP) }.getOrNull()
            }
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(svc: XposedService) {
        Log.i(TAG, "service bound v${svc.frameworkVersion}")
        mService = svc
        prefs.syncToRemote()
        listeners.forEach { it.onServiceBind(svc) }
    }

    override fun onServiceDied(svc: XposedService) {
        Log.i(TAG, "service died")
        mService = null
        listeners.forEach { it.onServiceDied(svc) }
    }

    fun addServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.add(listener)
        mService?.let { listener.onServiceBind(it) }
    }

    fun removeServiceListener(listener: XposedServiceHelper.OnServiceListener) {
        listeners.remove(listener)
    }

    companion object {
        fun from(context: Context): PHDPApp = context.applicationContext as PHDPApp
    }
}
