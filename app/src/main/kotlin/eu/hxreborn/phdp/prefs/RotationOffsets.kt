package eu.hxreborn.phdp.prefs

import android.view.Surface

enum class RotationSlot {
    R0,
    R90,
    R180,
    R270,
    ;

    companion object {
        fun fromSurfaceRotation(rotation: Int): RotationSlot =
            when (rotation) {
                Surface.ROTATION_90 -> R90
                Surface.ROTATION_180 -> R180
                Surface.ROTATION_270 -> R270
                else -> R0
            }
    }
}

data class OffsetPx(
    val x: Float = 0f,
    val y: Float = 0f,
)

data class RotationOffsets(
    val r0: OffsetPx = OffsetPx(),
    val r90: OffsetPx = OffsetPx(),
    val r180: OffsetPx = OffsetPx(),
    val r270: OffsetPx = OffsetPx(),
) {
    operator fun get(slot: RotationSlot): OffsetPx =
        when (slot) {
            RotationSlot.R0 -> r0
            RotationSlot.R90 -> r90
            RotationSlot.R180 -> r180
            RotationSlot.R270 -> r270
        }

    fun with(
        slot: RotationSlot,
        offset: OffsetPx,
    ): RotationOffsets =
        when (slot) {
            RotationSlot.R0 -> copy(r0 = offset)
            RotationSlot.R90 -> copy(r90 = offset)
            RotationSlot.R180 -> copy(r180 = offset)
            RotationSlot.R270 -> copy(r270 = offset)
        }

    // Compact string: "x,y|x,y|x,y|x,y" (R0|R90|R180|R270)
    fun serialize(): String = listOf(r0, r90, r180, r270).joinToString("|") { "${it.x},${it.y}" }

    companion object {
        val EMPTY = RotationOffsets()

        fun deserialize(s: String): RotationOffsets =
            runCatching {
                val parts = s.split("|")

                fun parse(i: Int): OffsetPx {
                    val (x, y) = parts[i].split(",").map { it.toFloat() }
                    return OffsetPx(x, y)
                }
                RotationOffsets(parse(0), parse(1), parse(2), parse(3))
            }.getOrDefault(EMPTY)
    }
}
