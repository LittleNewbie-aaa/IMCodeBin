package com.liheit.im.core.protocol

class DeleteSecrecyMsgReq(
        var mid: String,//消息ID
        var sid: String,//会话ID
        var noticeuserid: Long//被通知人id
)

class DeleteSecrecyMsgRsp(
        var mid: String//消息ID
) : Rsp()

data class DeleteSecrecyMsgNotice(
        var mid: String,//消息ID
        var userid: Long,//发送人id
        var sendterm: Int//发送人终端类型
)

class CheckSecrecyMsgExistReq(
        var mids: List<String>//消息ID数组
)

class CheckSecrecyMsgExistRsp(
        var msgs: List<SecrecyMsgExistState>//消息ID
) : Rsp()

data class SecrecyMsgExistState(
        var mid: String,//消息ID
        var state: Int//1代表存在，0代表删除
)