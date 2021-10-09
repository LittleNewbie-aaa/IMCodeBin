package com.liheit.im.core.protocol.message

/**
 * Created by daixun on 2018/7/11.
 */

open class ReceiptMsgNotice(
        var mid: String = "",
        var sid: String = "",
        var fromid: Long = 0,
        var toid: Long = 0,//接收者ID
        var type: Int = 0,//会话类型
        var mode: Int = 0,//0: 回执(其它保留)
        var t: Long = 0//消息发送时间 UTC(需要与服务器同步校准[用心跳校准])
)
