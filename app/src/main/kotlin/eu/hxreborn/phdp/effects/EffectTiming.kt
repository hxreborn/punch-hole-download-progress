package eu.hxreborn.phdp.effects

object EffectTiming {
    const val FLICKER_PER_CYCLE_MS = 90

    fun speedFactor(speed: String): Float =
        when (speed) {
            "slow" -> 1.5f
            "fast" -> 0.6f
            else -> 1f
        }

    private fun baseDurationMs(
        id: String,
        holdMs: Int,
        exitMs: Int,
    ): Int =
        when (id) {
            "blink" -> exitMs
            else -> holdMs + exitMs
        }

    fun totalMs(
        id: String,
        holdMs: Int,
        exitMs: Int,
        speed: String,
    ): Int = (baseDurationMs(id, holdMs, exitMs) * speedFactor(speed)).toInt()

    fun perCycleMs(
        id: String,
        holdMs: Int,
        exitMs: Int,
        speed: String,
        repeat: Int,
    ): Int = totalMs(id, holdMs, exitMs, speed) / repeat.coerceAtLeast(1)

    fun isFlickerRisk(
        id: String,
        holdMs: Int,
        exitMs: Int,
        speed: String,
        repeat: Int,
    ): Boolean =
        id == "blink" && perCycleMs(id, holdMs, exitMs, speed, repeat) < FLICKER_PER_CYCLE_MS
}
