package eu.hxreborn.phdp.prefs

data class ScaleXy(
    val x: Float = 1f,
    val y: Float = 1f,
)

data class FoldScales(
    val closed: ScaleXy = ScaleXy(),
    val open: ScaleXy = ScaleXy(),
) {
    operator fun get(fold: FoldSlot): ScaleXy =
        when (fold) {
            FoldSlot.CLOSED -> closed
            FoldSlot.OPEN -> open
        }

    fun with(
        fold: FoldSlot,
        scale: ScaleXy,
    ): FoldScales =
        when (fold) {
            FoldSlot.CLOSED -> copy(closed = scale)
            FoldSlot.OPEN -> copy(open = scale)
        }

    // Compact string: "x,y" when both postures match, otherwise "x,y|x,y" (closed|open)
    fun serialize(): String {
        if (open == closed) return "${closed.x},${closed.y}"
        return "${closed.x},${closed.y}|${open.x},${open.y}"
    }

    companion object {
        val DEFAULT = FoldScales()

        fun deserialize(s: String): FoldScales =
            runCatching {
                val parts = s.split("|")

                fun parse(i: Int): ScaleXy {
                    val (x, y) = parts[i].split(",").map { it.toFloat() }
                    return ScaleXy(x, y)
                }
                when (parts.size) {
                    1 -> {
                        val scale = parse(0)
                        FoldScales(closed = scale, open = scale)
                    }

                    2 -> {
                        FoldScales(closed = parse(0), open = parse(1))
                    }

                    else -> {
                        DEFAULT
                    }
                }
            }.getOrDefault(DEFAULT)
    }
}
