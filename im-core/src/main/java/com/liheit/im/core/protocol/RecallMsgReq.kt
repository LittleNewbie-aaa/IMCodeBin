package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/7/10.
 */

open  class RecallMsgReq(
        var mid: String = "",
        var sid: String = "",
        var fromid: Long = 0,//åéèID
        var t: Long = 0
)
