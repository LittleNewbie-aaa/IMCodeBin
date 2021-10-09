package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/25.
 */

data class SendMsgRsp(
        var t: Long = 0,
        var mid: String = "",
        var sid: String = "",
        val flag: Int = 0
) : Rsp()
