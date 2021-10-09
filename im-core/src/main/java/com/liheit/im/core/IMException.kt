package com.liheit.im.core

import java.net.UnknownHostException

/**
 * Created by daixun on 2018/8/4.
 */

class IMException(var code: Long, message: String) : RuntimeException(message) {


    companion object {
        fun create(code: Long): IMException {
            return IMException(code, ResultCode.formatResultCode(code))
        }

        fun create(msg: String): IMException {
            return IMException(ResultCode.ERR_UNKNOWN, msg)
        }
    }
}

fun Throwable.toIMException(): IMException {
    if (this is IMException) {
        return this
    } else {
        return when {
            this is UnknownHostException -> IMException.create(ResultCode.CONNECTION_INTERRUPTION)
            else -> IMException.create(ResultCode.ERR_UNKNOWN)
        }
    }
}
