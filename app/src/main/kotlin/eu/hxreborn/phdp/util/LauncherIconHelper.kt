package eu.hxreborn.phdp.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import eu.hxreborn.phdp.BuildConfig

object LauncherIconHelper {
    private const val ALIAS_NAME = "${BuildConfig.APPLICATION_ID}.LauncherAlias"

    fun isLauncherIconVisible(context: Context): Boolean {
        val info =
            context.packageManager.getPackageInfo(
                BuildConfig.APPLICATION_ID,
                PackageManager.GET_ACTIVITIES,
            )
        return info.activities?.any { it.targetActivity != null } == true
    }

    fun setLauncherIconVisible(
        context: Context,
        visible: Boolean,
    ) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ALIAS_NAME),
            if (visible) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
