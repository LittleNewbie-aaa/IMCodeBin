package com.liheit.im.core.protocol

import com.liheit.im.core.IMException
import com.liheit.im.core.ResultCode
import com.liheit.im.utils.Log

/**
 * Created by daixun on 2018/6/14.
 */

open class Rsp(
        var result: Long = -1
) {
    fun isSuccess(): Boolean {
        if (result == ResultCode.ErrOK) {
            return true
        } else {
            Log.e(">>>>>>>>>>>>>" + ResultCode.formatResultCode(result))
            return false
        }
    }

    fun toException(): IMException {
        return IMException.create(result)
    }


    fun getUint32(l: Long): Long {
        return l and -0x1
    }
}
