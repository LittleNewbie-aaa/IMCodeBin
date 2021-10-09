package com.liheit.im.utils

/**
 * Created by daixun on 2018/6/18.
 */

import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.Config
import com.liheit.im.core.service.ConfigService
import kotlin.reflect.KProperty

class DBConfigDelegates<T>(val name: String, val default: T, private var act: String? = null) {


    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getSharedPreferences(name, default)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putSharedPreferences(name, value)
    }

    private fun putSharedPreferences(name: String, value: T) {
        when (value) {
            is Long, is String, is Int, is Boolean, is Float ->ConfigService.save(Config(name, getAccount(), value.toString()))
            else -> throw IllegalArgumentException("SharedPreferences can't be save this type")
        }
    }

    private fun getAccount(): String {
        return act ?: (IMClient.getCurrentUserAccount() ?: "")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSharedPreferences(name: String, default: T): T {
        val cfg = ConfigService.findByKeyAndAccount(name, getAccount())
        var v = cfg?.value
        val res: Any = when (default) {
            is Long -> v?.toLongOrNull() ?: default
            is String -> v ?: default
            is Int -> v?.toIntOrNull() ?: default
            is Boolean -> v?.toBoolean() ?: default
            is Float -> v?.toFloatOrNull() ?: default
            else -> throw IllegalArgumentException("不支持的类型")
        }
        return res as T
    }
}
