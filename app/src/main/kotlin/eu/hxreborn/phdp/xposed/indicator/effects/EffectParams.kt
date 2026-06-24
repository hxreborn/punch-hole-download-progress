package eu.hxreborn.phdp.xposed.indicator.effects

import eu.hxreborn.phdp.effects.EffectTiming
import eu.hxreborn.phdp.xposed.hook.IndicatorState

data class EffectParams(
    val style: String,
    val holdMs: Int,
    val exitMs: Int,
    val speedFactor: Float,
    val intensityScale: Float,
    val reverse: Boolean,
    val repeat: Int,
    val segmentCount: Int,
    val segmentGapDegrees: Float,
) {
    fun scaled(ms: Int): Long = (ms * speedFactor).toLong()

    companion object {
        fun fromState(): EffectParams =
            EffectParams(
                style = IndicatorState.finishStyle,
                holdMs = IndicatorState.finishHoldMs,
                exitMs = IndicatorState.finishExitMs,
                speedFactor = EffectTiming.speedFactor(IndicatorState.effectSpeed),
                intensityScale = intensityScale(IndicatorState.effectIntensity),
                reverse = IndicatorState.effectReverse,
                repeat = IndicatorState.effectRepeat,
                segmentCount = IndicatorState.segmentCount,
                segmentGapDegrees = IndicatorState.segmentGapDegrees,
            )

        private fun intensityScale(intensity: String): Float =
            when (intensity) {
                "low" -> 0.6f
                "high" -> 1.5f
                else -> 1f
            }
    }
}
