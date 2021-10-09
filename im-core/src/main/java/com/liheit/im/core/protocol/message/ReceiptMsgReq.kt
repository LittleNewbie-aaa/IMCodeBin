package com.liheit.im.core.protocol.message

/**
 * Created by daixun on 2018/7/11.
 */

open class ReceiptMsgReq(
        var mid: String = "",
        var sid: String = "",
        var fromid: Long = 0,
        var toid: Long? = null,
        var mode: Int = 0,//固定为0
        var type: Int = 0,//会话类型
        var t: Long = 0

)
