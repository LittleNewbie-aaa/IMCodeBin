package com.liheit.im.core.protocol

/**
 * 消息已读同步请求 如果消息变为已读，则发送此命令
 * Created by daixun on 2018/7/8.
 */
data class SyncMsgReq(
        var t: Long = 0, //离线消息:最大的离线消息时间; 会话消息:最大的会话消息时间;
        var sid: String? = null//新消息已读同步时为会话 ID;否则为空字符串
)

data class SyncMsgInfo(
        var sid: String = "",
        var t: Long = 0
)

data class SyncMsgRsp(
        var t: Long = 0, //离线消息:最大的离线消息时间; 会话消息:最大的会话消息时间;
        var sid: String? = null//新消息已读同步时为会话 ID;否则为空字符串
) : Rsp()

/**
 * 新消息已读同步通知
 */
data class SyncMsgNotice(
        var t: Long = 0, //最大的会话消息时间
        var sid: String? = null//新消息已读同步的会话 ID
)

data class SyncReadStateRsp(
        val readstates: List<SyncMsgNotice>
) : Rsp()

/**
 * 新消息已读同步通知应答  和通知一毛一样
 */
typealias  SyncMsgNoticeAck = SyncMsgNotice
/*
data class SyncMsgNoticeAck(
        var t: Long = 0,//原样返回通知内容
        var sid: String? = null //原样返回通知内容
)*/
