package com.liheit.im.common.ext

import android.app.Application

/**
 * Created by ishion on 2017/11/6.
 */
object Ext {
    lateinit var ctx: Application

    fun with(app: Application) {
        ctx = app
    }
}