package com.liheit.im.core.bean

/**
 * Created by daixun on 2018/3/15.
 */

open class Resp(var method: String = "",
                var timestamp: String = "",
                var responseMessage:String="",
                var responseCode: String = "") {


    fun isSuccess(): Boolean {
        return "00".equals(responseCode)
    }
}
