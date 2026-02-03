package eu.hxreborn.phdp.prefs

import android.content.SharedPreferences

sealed class PrefSpec<T : Any>(
    val key: String,
    val default: T,
) {
    abstract fun read(prefs: SharedPreferences): T

    abstract fun write(
        editor: SharedPreferences.Editor,
        value: T,
    )

    fun reset(editor: SharedPreferences.Editor) = write(editor, default)
}

class BoolPref(
    key: String,
    default: Boolean,
) : PrefSpec<Boolean>(key, default) {
    override fun read(prefs: SharedPreferences): Boolean = prefs.getBoolean(key, default)

    override fun write(
        editor: SharedPreferences.Editor,
        value: Boolean,
    ) {
        editor.putBoolean(key, value)
    }
}

class IntPref(
    key: String,
    default: Int,
    val range: IntRange? = null,
) : PrefSpec<Int>(key, default) {
    override fun read(prefs: SharedPreferences): Int {
        val raw = prefs.getInt(key, default)
        return range?.let { raw.coerceIn(it) } ?: raw
    }

    override fun write(
        editor: SharedPreferences.Editor,
        value: Int,
    ) {
        editor.putInt(key, value)
    }
}

class FloatPref(
    key: String,
    default: Float,
    val range: ClosedFloatingPointRange<Float>? = null,
) : PrefSpec<Float>(key, default) {
    override fun read(prefs: SharedPreferences): Float {
        val raw = prefs.getFloat(key, default)
        return range?.let { raw.coerceIn(it) } ?: raw
    }

    override fun write(
        editor: SharedPreferences.Editor,
        value: Float,
    ) {
        editor.putFloat(key, value)
    }
}

class StringPref(
    key: String,
    default: String,
) : PrefSpec<String>(key, default) {
    override fun read(prefs: SharedPreferences): String = prefs.getString(key, default) ?: default

    override fun write(
        editor: SharedPreferences.Editor,
        value: String,
    ) {
        editor.putString(key, value)
    }
}

class SetPref(
    key: String,
    default: Set<String>,
) : PrefSpec<Set<String>>(key, default) {
    override fun read(prefs: SharedPreferences): Set<String> =
        (prefs.getStringSet(key, default) ?: default).toSet()

    override fun write(
        editor: SharedPreferences.Editor,
        value: Set<String>,
    ) {
        editor.putStringSet(key, value)
    }
}
