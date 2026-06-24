package eu.hxreborn.phdp.effects

import eu.hxreborn.phdp.R

enum class EffectOption { SPEED, INTENSITY, DIRECTION, REPEAT }

data class EffectInfo(
    val id: String,
    val descRes: Int,
    val options: Set<EffectOption>,
)

object EffectCatalog {
    val all: List<EffectInfo> =
        listOf(
            EffectInfo("snap", R.string.effect_desc_snap, emptySet()),
            EffectInfo("pop", R.string.effect_desc_pop, setOf(EffectOption.INTENSITY)),
            EffectInfo(
                "segmented",
                R.string.effect_desc_segmented,
                setOf(EffectOption.SPEED, EffectOption.DIRECTION),
            ),
            EffectInfo(
                "wipe",
                R.string.effect_desc_wipe,
                setOf(EffectOption.SPEED, EffectOption.DIRECTION),
            ),
            EffectInfo(
                "spinoff",
                R.string.effect_desc_spinoff,
                setOf(EffectOption.SPEED, EffectOption.INTENSITY, EffectOption.DIRECTION),
            ),
            EffectInfo(
                "blink",
                R.string.effect_desc_blink,
                setOf(EffectOption.SPEED, EffectOption.INTENSITY, EffectOption.REPEAT),
            ),
            EffectInfo(
                "sweep",
                R.string.effect_desc_sweep,
                setOf(EffectOption.SPEED, EffectOption.DIRECTION),
            ),
            EffectInfo(
                "rainbow",
                R.string.effect_desc_rainbow,
                setOf(EffectOption.SPEED, EffectOption.DIRECTION, EffectOption.REPEAT),
            ),
        )

    private val index = all.associateBy { it.id }

    fun byId(id: String): EffectInfo = index[id] ?: index.getValue("pop")
}
