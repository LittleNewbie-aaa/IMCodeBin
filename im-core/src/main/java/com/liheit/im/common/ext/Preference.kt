package com.liheit.im.common.ext

import android.content.Context
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class Preference<T> constructor(private var key: String, private var default: T) : ReadWriteProperty<Any, T> {

    companion object {
        val prefs by lazy { Ext.ctx.getSharedPreferences("default", Context.MODE_PRIVATE) }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return findPreference(key, default)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        putPreference(key, value)

    }

    fun delete(vararg key: String): Unit {
        if (key.size == 0) {
            prefs.edit().clear().commit()
            return
        }
        for (i in 0..key.size) {
            prefs.edit().remove(key[i]).commit()
        }
    }

    private fun <T> findPreference(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> this!!.getString(name, default)!!
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException("The data can not be saved")
        }
        res as T
    }

    private fun <U> putPreference(name: String, value: U) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("The data can not be saved")
        }.apply()
    }
}