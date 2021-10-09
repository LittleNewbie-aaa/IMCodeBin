package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/7/10.
 */

open  class RecallMsgReq(
        var mid: String = "",
        var sid: String = "",
        var fromid: Long = 0,//发送者ID
        var t: Long = 0
)
