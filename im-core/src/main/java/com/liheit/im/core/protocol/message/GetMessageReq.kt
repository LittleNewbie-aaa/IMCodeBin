package com.liheit.im.core.protocol.message

/**
 * Created by daixun on 2018/7/18.
 */

class GetMessageReq(
        var sid: String = "",
        var t: Long = 0,//获取消息的起始时间(包括)
        var n: Int = 0//-n: 表示指定时间前N条消息；n表示指定时间后n条消息;单次请求不能超过200（后台会限制最大200笔）
)
