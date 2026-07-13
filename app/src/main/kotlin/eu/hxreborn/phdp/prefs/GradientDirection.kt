package eu.hxreborn.phdp.prefs

enum class GradientDirection(
    val storedValue: String,
) {
    SWEEP("sweep"),
    LINEAR("linear"),
    ;

    companion object {
        fun fromStoredValue(value: String): GradientDirection =
            entries.firstOrNull { it.storedValue == value } ?: SWEEP
    }
}
