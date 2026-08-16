package eu.hxreborn.phdp.prefs

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager

enum class FoldPosture {
    UNAVAILABLE,
    CLOSED,
    HALF_OPEN,
    OPEN,
    ;

    fun toSlot(): FoldSlot =
        when (this) {
            UNAVAILABLE, CLOSED -> FoldSlot.CLOSED
            HALF_OPEN, OPEN -> FoldSlot.OPEN
        }
}

data class FoldState(
    val applicable: Boolean,
    val posture: FoldPosture,
) {
    val slot: FoldSlot get() = posture.toSlot()
}

object FoldPostureResolver {
    internal const val CLOSED_MAX_DEGREES = 30f
    internal const val OPEN_MIN_DEGREES = 150f
    private const val COVER_AREA_NUMERATOR = 3
    private const val COVER_AREA_DENOMINATOR = 4

    fun isFoldable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)) {
            return true
        }
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) != null
    }

    fun resolve(
        context: Context,
        hingeAngle: Float? = null,
    ): FoldPosture =
        from(
            foldable = isFoldable(context),
            coverDisplay = isCoverDisplay(context),
            hingeAngle = hingeAngle,
        )

    fun from(
        foldable: Boolean,
        coverDisplay: Boolean,
        hingeAngle: Float?,
    ): FoldPosture {
        if (!foldable) return FoldPosture.UNAVAILABLE
        if (hingeAngle != null) return fromHingeAngle(hingeAngle)
        return if (coverDisplay) FoldPosture.CLOSED else FoldPosture.OPEN
    }

    fun fromHingeAngle(angle: Float): FoldPosture =
        when {
            angle < CLOSED_MAX_DEGREES -> FoldPosture.CLOSED
            angle < OPEN_MIN_DEGREES -> FoldPosture.HALF_OPEN
            else -> FoldPosture.OPEN
        }

    fun isCoverDisplay(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val current = wm.currentWindowMetrics.bounds
        val max = wm.maximumWindowMetrics.bounds
        val currentArea = current.width().toLong() * current.height()
        val maxArea = max.width().toLong() * max.height()
        return maxArea > 0L && currentArea * COVER_AREA_DENOMINATOR < maxArea * COVER_AREA_NUMERATOR
    }
}
