package eu.hxreborn.phdp.prefs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

private const val TAG = "PHDP"

object SettingsPush {
    const val ACTION = "eu.hxreborn.phdp.action.SETTING_CHANGED"
    const val PERMISSION = "eu.hxreborn.phdp.permission.PUSH_SETTINGS"
    const val EXTRA_KEY = "key"
    const val EXTRA_TYPE = "type"
    const val EXTRA_BOOL = "bool"
    const val EXTRA_INT = "int"
    const val EXTRA_STRING = "string"

    private const val SYSTEMUI = "com.android.systemui"
    private const val TYPE_BOOL = "bool"
    private const val TYPE_INT = "int"
    private const val TYPE_STRING = "string"

    private val pushedKeys =
        setOf(
            Prefs.duoStatusBarMode.key,
            Prefs.duoProgressColor.key,
            Prefs.color.key,
            Prefs.gradientEnabled.key,
            Prefs.gradientColors.key,
            Prefs.gradientDirection.key,
            Prefs.gradientAngle.key,
            Prefs.clockwise.key,
            Prefs.errorColor.key,
            Prefs.percentTextEnabled.key,
            Prefs.materialYouEnabled.key,
            Prefs.materialYouProgressPalette.key,
            Prefs.materialYouProgressShade.key,
            Prefs.materialYouSuccessPalette.key,
            Prefs.materialYouSuccessShade.key,
            Prefs.materialYouErrorPalette.key,
            Prefs.materialYouErrorShade.key,
            Prefs.powerSaverMode.key,
            Prefs.progressEasing.key,
            Prefs.progressAnimMs.key,
            Prefs.finishUseFlashColor.key,
            Prefs.finishFlashColor.key,
            Prefs.finishHoldMs.key,
            Prefs.finishExitMs.key,
            Prefs.minVisibilityEnabled.key,
            Prefs.minVisibilityMs.key,
            Prefs.burnInHideMs.key,
            Prefs.testProgress.key,
            Prefs.testError.key,
            Prefs.previewTrigger.key,
            Prefs.clearDownloadsTrigger.key,
        )

    fun <T : Any> send(
        context: Context,
        pref: PrefSpec<T>,
        value: T,
    ) {
        if (pref.key !in pushedKeys) return
        val intent =
            Intent(ACTION)
                .setPackage(SYSTEMUI)
                .putExtra(EXTRA_KEY, pref.key)
        when (value) {
            is Boolean -> {
                intent.putExtra(EXTRA_TYPE, TYPE_BOOL)
                intent.putExtra(EXTRA_BOOL, value)
            }

            is Int -> {
                intent.putExtra(EXTRA_TYPE, TYPE_INT)
                intent.putExtra(EXTRA_INT, value)
            }

            is String -> {
                intent.putExtra(EXTRA_TYPE, TYPE_STRING)
                intent.putExtra(EXTRA_STRING, value)
            }

            else -> {
                return
            }
        }
        runCatching { context.sendBroadcast(intent) }
            .onFailure { Log.w(TAG, "settings push failed key=${pref.key}", it) }
    }

    fun sendSnapshot(
        context: Context,
        prefs: SharedPreferences,
    ) {
        send(context, Prefs.duoStatusBarMode, Prefs.duoStatusBarMode.read(prefs))
        send(context, Prefs.duoProgressColor, Prefs.duoProgressColor.read(prefs))
        send(context, Prefs.color, Prefs.color.read(prefs))
        send(context, Prefs.gradientEnabled, Prefs.gradientEnabled.read(prefs))
        send(context, Prefs.gradientColors, Prefs.gradientColors.read(prefs))
        send(context, Prefs.gradientDirection, Prefs.gradientDirection.read(prefs))
        send(context, Prefs.gradientAngle, Prefs.gradientAngle.read(prefs))
        send(context, Prefs.clockwise, Prefs.clockwise.read(prefs))
        send(context, Prefs.errorColor, Prefs.errorColor.read(prefs))
        send(context, Prefs.percentTextEnabled, Prefs.percentTextEnabled.read(prefs))
        send(context, Prefs.materialYouEnabled, Prefs.materialYouEnabled.read(prefs))
        send(
            context,
            Prefs.materialYouProgressPalette,
            Prefs.materialYouProgressPalette.read(prefs),
        )
        send(context, Prefs.materialYouProgressShade, Prefs.materialYouProgressShade.read(prefs))
        send(context, Prefs.materialYouSuccessPalette, Prefs.materialYouSuccessPalette.read(prefs))
        send(context, Prefs.materialYouSuccessShade, Prefs.materialYouSuccessShade.read(prefs))
        send(context, Prefs.materialYouErrorPalette, Prefs.materialYouErrorPalette.read(prefs))
        send(context, Prefs.materialYouErrorShade, Prefs.materialYouErrorShade.read(prefs))
        send(context, Prefs.powerSaverMode, Prefs.powerSaverMode.read(prefs))
        send(context, Prefs.progressEasing, Prefs.progressEasing.read(prefs))
        send(context, Prefs.progressAnimMs, Prefs.progressAnimMs.read(prefs))
        send(context, Prefs.finishUseFlashColor, Prefs.finishUseFlashColor.read(prefs))
        send(context, Prefs.finishFlashColor, Prefs.finishFlashColor.read(prefs))
        send(context, Prefs.finishHoldMs, Prefs.finishHoldMs.read(prefs))
        send(context, Prefs.finishExitMs, Prefs.finishExitMs.read(prefs))
        send(context, Prefs.minVisibilityEnabled, Prefs.minVisibilityEnabled.read(prefs))
        send(context, Prefs.minVisibilityMs, Prefs.minVisibilityMs.read(prefs))
        send(context, Prefs.burnInHideMs, Prefs.burnInHideMs.read(prefs))
    }

    fun bool(intent: Intent): Boolean = intent.getBooleanExtra(EXTRA_BOOL, false)

    fun int(
        intent: Intent,
        default: Int = 0,
    ): Int = intent.getIntExtra(EXTRA_INT, default)

    fun string(
        intent: Intent,
        default: String = "",
    ): String = intent.getStringExtra(EXTRA_STRING) ?: default
}
