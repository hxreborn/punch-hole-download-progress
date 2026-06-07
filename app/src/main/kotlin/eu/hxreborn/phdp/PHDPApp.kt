package eu.hxreborn.phdp

import android.app.Application
import android.content.Context
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.PrefsRepository
import eu.hxreborn.phdp.prefs.PrefsRepositoryImpl
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

class PHDPApp :
    Application(),
    XposedServiceHelper.OnServiceListener {
    @Volatile
    var mService: XposedService? = null
        private set

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
        mService = svc
        // Push any writes that happened while the binder was dead so the hook
        // process sees the current state. Safe to run on every (re)bind because
        // edit() is idempotent.
        prefs.syncToRemote()
        listeners.forEach { it.onServiceBind(svc) }
    }

    override fun onServiceDied(svc: XposedService) {
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
