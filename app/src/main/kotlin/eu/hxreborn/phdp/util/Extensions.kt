package eu.hxreborn.phdp.util

import java.lang.reflect.Field

internal fun Class<*>.accessibleField(name: String): Field =
    getDeclaredField(name).apply {
        isAccessible =
            true
    }

fun labelFromValues(
    value: String,
    entries: List<String>,
    values: List<String>,
): String? = entries.getOrNull(values.indexOf(value))
