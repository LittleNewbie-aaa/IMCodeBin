package com.liheit.im.core

import com.liheit.im.utils.Log
import io.netty.util.internal.logging.InternalLogLevel
import io.netty.util.internal.logging.InternalLogger

class AndroidLog : InternalLogger {
    override fun warn(msg: String?) {
        Log.w("${msg}")
    }

    override fun warn(format: String?, arg: Any?) {
        Log.w(String.format("${format}", arg))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        Log.w(String.format("${format}", *arguments))
    }

    override fun warn(format: String?, argA: Any?, argB: Any?) {
        Log.w(String.format("${format}", argA, argB))
    }

    override fun warn(msg: String?, t: Throwable?) {
        Log.w(String.format("${msg}"))
        t?.printStackTrace()
    }

    override fun warn(t: Throwable?) {
        t?.printStackTrace()
    }

    override fun info(msg: String?) {
        Log.i("${msg}")
    }

    override fun info(format: String?, arg: Any?) {
        Log.i(String.format("${format}", arg))
    }

    override fun info(format: String?, argA: Any?, argB: Any?) {
        Log.i(String.format("${format}", argA, argB))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        Log.i(String.format("${format}", *arguments))
    }

    override fun info(msg: String?, t: Throwable?) {
        Log.i("${msg}")
        t?.printStackTrace()
    }

    override fun info(t: Throwable?) {
        t?.printStackTrace()
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun error(msg: String?) {
        Log.e("${msg}")
    }

    override fun error(format: String?, arg: Any?) {
        Log.e(String.format("${format}", arg))
    }

    override fun error(format: String?, argA: Any?, argB: Any?) {
        Log.e(String.format("${format}", argA, argB))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        Log.e(String.format("${format}", *arguments))
    }

    override fun error(msg: String?, t: Throwable?) {
        Log.e("${msg}", t!!)
    }

    override fun error(t: Throwable?) {
        Log.e("", t!!)
    }

    override fun name(): String {
        return "androidLog"
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun log(level: InternalLogLevel?, msg: String?) {
        when (level) {
            InternalLogLevel.DEBUG -> debug(msg)
            InternalLogLevel.INFO -> info(msg)
            InternalLogLevel.ERROR -> error(msg)
            InternalLogLevel.TRACE -> trace(msg)
            InternalLogLevel.WARN -> warn(msg)
            else -> {
                println("unknow level ${msg}")
            }
        }
    }

    override fun log(level: InternalLogLevel?, format: String?, arg: Any?) {
        log(level, String.format("${format}", arg))
    }

    override fun log(level: InternalLogLevel?, format: String?, argA: Any?, argB: Any?) {
        log(level, String.format("${format}", argA, argB))
    }

    override fun log(level: InternalLogLevel?, format: String?, vararg arguments: Any?) {
        log(level, String.format("${format}", *arguments))
    }

    override fun log(level: InternalLogLevel?, msg: String?, t: Throwable?) {
        log(level, "${msg} : ${t?.message}")
        t?.printStackTrace()
    }

    override fun log(level: InternalLogLevel?, t: Throwable?) {
        t?.printStackTrace()
    }

    override fun debug(msg: String?) {
        Log.d("${msg}")
    }

    override fun debug(format: String?, arg: Any?) {
        debug(String.format("${format}", arg))
    }

    override fun debug(format: String?, argA: Any?, argB: Any?) {
        debug(String.format("${format}", argA, argB))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        debug(String.format("${format}", *arguments))
    }

    override fun debug(msg: String?, t: Throwable?) {
        debug("${msg} ${t?.message}")
        t?.printStackTrace()
    }

    override fun debug(t: Throwable?) {
        debug("${t?.message}")
        t?.printStackTrace()
    }

    override fun isEnabled(level: InternalLogLevel?): Boolean {
        return when (level) {
            InternalLogLevel.WARN -> isWarnEnabled
            InternalLogLevel.DEBUG -> isDebugEnabled
            InternalLogLevel.INFO -> isInfoEnabled
            InternalLogLevel.ERROR -> isErrorEnabled
            InternalLogLevel.TRACE -> isTraceEnabled
            else -> false
        }
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun trace(msg: String?) {
        Log.v("${msg}")
    }

    override fun trace(format: String?, arg: Any?) {
        trace(String.format("${format}", arg))
    }

    override fun trace(format: String?, argA: Any?, argB: Any?) {
        trace(String.format("${format}", argA, argB))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        trace(String.format("${format}", *arguments))
    }

    override fun trace(msg: String?, t: Throwable?) {
        trace("${msg} ${t?.message}")
        t?.printStackTrace()
    }

    override fun trace(t: Throwable?) {
        t?.printStackTrace()
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isTraceEnabled(): Boolean {
        return false
    }


}
