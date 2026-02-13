package eu.hxreborn.phdp.util

import java.lang.reflect.Field

internal fun Class<*>.accessibleField(name: String): Field =
    getDeclaredField(name).apply {
        isAccessible = true
    }

internal fun Class<*>.accessibleFieldFromHierarchy(name: String): Field? {
    var clazz: Class<*>? = this
    while (clazz != null) {
        runCatching {
            return clazz.getDeclaredField(name).apply { isAccessible = true }
        }
        clazz = clazz.superclass
    }
    return null
}

fun labelFromValues(
    value: String,
    entries: List<String>,
    values: List<String>,
): String? = entries.getOrNull(values.indexOf(value))
