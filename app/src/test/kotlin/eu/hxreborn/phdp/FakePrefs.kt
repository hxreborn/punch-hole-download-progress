package eu.hxreborn.phdp

import android.content.SharedPreferences

internal class FakePrefs(
    initial: Map<String, Any> = emptyMap(),
    private val poisonedKey: String? = null,
    private val commitSucceeds: Boolean = true,
) : SharedPreferences {
    val values = LinkedHashMap<String, Any>(initial)

    override fun getAll(): MutableMap<String, *> = LinkedHashMap(values)

    override fun getString(
        key: String?,
        defValue: String?,
    ): String? = values[key] as? String ?: defValue

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getInt(
        key: String?,
        defValue: Int,
    ): Int = values[key] as? Int ?: defValue

    override fun getLong(
        key: String?,
        defValue: Long,
    ): Long = values[key] as? Long ?: defValue

    override fun getFloat(
        key: String?,
        defValue: Float,
    ): Float = values[key] as? Float ?: defValue

    override fun getBoolean(
        key: String?,
        defValue: Boolean,
    ): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    private inner class FakeEditor : SharedPreferences.Editor {
        private val staged = LinkedHashMap<String, Any?>()

        private fun stage(
            key: String?,
            value: Any?,
        ): SharedPreferences.Editor {
            if (key == poisonedKey) error("cannot write $key")
            staged[key!!] = value
            return this
        }

        override fun putString(
            key: String?,
            value: String?,
        ) = stage(key, value)

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ) = stage(key, values)

        override fun putInt(
            key: String?,
            value: Int,
        ) = stage(key, value)

        override fun putLong(
            key: String?,
            value: Long,
        ) = stage(key, value)

        override fun putFloat(
            key: String?,
            value: Float,
        ) = stage(key, value)

        override fun putBoolean(
            key: String?,
            value: Boolean,
        ) = stage(key, value)

        override fun remove(key: String?) = stage(key, null)

        override fun clear(): SharedPreferences.Editor {
            staged.clear()
            return this
        }

        override fun commit(): Boolean {
            if (!commitSucceeds) return false
            apply()
            return true
        }

        override fun apply() {
            for ((key, value) in staged) {
                if (value == null) values.remove(key) else values[key] = value
            }
        }
    }
}
