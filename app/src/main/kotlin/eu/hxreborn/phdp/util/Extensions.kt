package eu.hxreborn.phdp.util

import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.reflect.Field

internal fun Class<*>.accessibleField(name: String): Field = getDeclaredField(name).apply { isAccessible = true }

fun View.weakVibrate() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}
