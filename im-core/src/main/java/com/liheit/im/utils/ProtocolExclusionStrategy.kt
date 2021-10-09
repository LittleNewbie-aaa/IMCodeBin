package com.liheit.im.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes


/**
 * Created by daixun on 2018/7/12.
 */

class ProtocolExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return if (f.getAnnotation(ProtocolExclude::class.java) != null) {
            true
        } else false
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        return false
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(allowedTargets = arrayOf(AnnotationTarget.FIELD))
    annotation class ProtocolExclude
}

