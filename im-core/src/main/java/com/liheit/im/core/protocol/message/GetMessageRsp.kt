package com.liheit.im.core.protocol.message

import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.protocol.Rsp

/**
 * Created by daixun on 2018/7/19.
 */

class GetMessageRsp(
        var sid: String = "",
        var t: Long = 0,//消息响应/应答的时间UTC
        var n: Int,//n: 表示指定时间前N条消息；n表示指定时间后n条消息;单次请求不能超过200（后台会限制最大200笔）
        var messages: MutableList<ChatMessage>? = null
) : Rsp()
