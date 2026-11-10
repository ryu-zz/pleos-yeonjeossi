package com.example.test.yeonsu

import android.content.SharedPreferences

/**
 * 메모리에만 있는 SharedPreferences (테스트 전용). 실제 사용자 기록과 무관하다.
 * Editor 는 apply()/commit() 때 한꺼번에 반영한다(실제 SharedPreferences 와 같은 방식).
 */
class FakePrefs : SharedPreferences {
    val data = HashMap<String, Any?>()
    override fun getAll(): MutableMap<String, *> = HashMap(data)
    override fun getString(key: String?, defValue: String?): String? = (data[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val ops = ArrayList<() -> Unit>()
        override fun putString(key: String?, value: String?) = apply { ops.add { data[key!!] = value } }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { ops.add { data[key!!] = values } }
        override fun putInt(key: String?, value: Int) = apply { ops.add { data[key!!] = value } }
        override fun putLong(key: String?, value: Long) = apply { ops.add { data[key!!] = value } }
        override fun putFloat(key: String?, value: Float) = apply { ops.add { data[key!!] = value } }
        override fun putBoolean(key: String?, value: Boolean) = apply { ops.add { data[key!!] = value } }
        override fun remove(key: String?) = apply { ops.add { data.remove(key) } }
        override fun clear() = apply { ops.add { data.clear() } }
        override fun commit(): Boolean { ops.forEach { it() }; return true }
        override fun apply() { ops.forEach { it() } }
    }
}
