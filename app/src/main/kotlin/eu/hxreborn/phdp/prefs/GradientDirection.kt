package eu.hxreborn.phdp.prefs

enum class GradientDirection(
    val storedValue: String,
) {
    SWEEP("sweep"),
    LEFT_TO_RIGHT("left_to_right"),
    TOP_TO_BOTTOM("top_to_bottom"),
    TOP_LEFT_TO_BOTTOM_RIGHT("top_left_to_bottom_right"),
    BOTTOM_LEFT_TO_TOP_RIGHT("bottom_left_to_top_right"),
    ;

    companion object {
        fun fromStoredValue(value: String): GradientDirection =
            entries.firstOrNull { it.storedValue == value } ?: SWEEP
    }
}
