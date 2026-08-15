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

enum class FoldSlot {
    CLOSED,
    OPEN,
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
    val openR0: OffsetPx = OffsetPx(),
    val openR90: OffsetPx = OffsetPx(),
    val openR180: OffsetPx = OffsetPx(),
    val openR270: OffsetPx = OffsetPx(),
) {
    operator fun get(slot: RotationSlot): OffsetPx = get(FoldSlot.CLOSED, slot)

    operator fun get(
        fold: FoldSlot,
        slot: RotationSlot,
    ): OffsetPx =
        when (fold) {
            FoldSlot.CLOSED -> {
                when (slot) {
                    RotationSlot.R0 -> r0
                    RotationSlot.R90 -> r90
                    RotationSlot.R180 -> r180
                    RotationSlot.R270 -> r270
                }
            }

            FoldSlot.OPEN -> {
                when (slot) {
                    RotationSlot.R0 -> openR0
                    RotationSlot.R90 -> openR90
                    RotationSlot.R180 -> openR180
                    RotationSlot.R270 -> openR270
                }
            }
        }

    fun with(
        slot: RotationSlot,
        offset: OffsetPx,
    ): RotationOffsets = with(FoldSlot.CLOSED, slot, offset)

    fun with(
        fold: FoldSlot,
        slot: RotationSlot,
        offset: OffsetPx,
    ): RotationOffsets =
        when (fold) {
            FoldSlot.CLOSED -> {
                when (slot) {
                    RotationSlot.R0 -> copy(r0 = offset)
                    RotationSlot.R90 -> copy(r90 = offset)
                    RotationSlot.R180 -> copy(r180 = offset)
                    RotationSlot.R270 -> copy(r270 = offset)
                }
            }

            FoldSlot.OPEN -> {
                when (slot) {
                    RotationSlot.R0 -> copy(openR0 = offset)
                    RotationSlot.R90 -> copy(openR90 = offset)
                    RotationSlot.R180 -> copy(openR180 = offset)
                    RotationSlot.R270 -> copy(openR270 = offset)
                }
            }
        }

    // Compact string: "x,y|x,y|x,y|x,y" (closed R0|R90|R180|R270)
    // Unfolded slots append as 4 more pairs when any open offset is set.
    fun serialize(): String {
        val closed = listOf(r0, r90, r180, r270)
        val open = listOf(openR0, openR90, openR180, openR270)
        val slots = if (open.all { it == OffsetPx() }) closed else closed + open
        return slots.joinToString("|") { "${it.x},${it.y}" }
    }

    companion object {
        val EMPTY = RotationOffsets()

        fun deserialize(s: String): RotationOffsets =
            runCatching {
                val parts = s.split("|")

                fun parse(i: Int): OffsetPx {
                    val (x, y) = parts[i].split(",").map { it.toFloat() }
                    return OffsetPx(x, y)
                }
                when (parts.size) {
                    4 -> {
                        RotationOffsets(parse(0), parse(1), parse(2), parse(3))
                    }

                    8 -> {
                        RotationOffsets(
                            parse(0),
                            parse(1),
                            parse(2),
                            parse(3),
                            parse(4),
                            parse(5),
                            parse(6),
                            parse(7),
                        )
                    }

                    else -> {
                        EMPTY
                    }
                }
            }.getOrDefault(EMPTY)
    }
}
