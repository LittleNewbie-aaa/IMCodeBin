package com.liheit.im.core.protocol

import com.liheit.im.core.bean.ChatMessage

/**
 * Created by daixun on 2018/7/3.
 */

data class GetOfflineMsgRsp(
        var t: Long = 0,
        var messages: MutableList<ChatMessage>? = null
) : Rsp()
