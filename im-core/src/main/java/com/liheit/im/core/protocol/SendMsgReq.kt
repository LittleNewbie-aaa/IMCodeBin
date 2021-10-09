package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/25.
 */

data class SendMsgReq(
        var t: Long = 0,//int n 消息发送时间 UTC（需要与服务器同步校准［用心跳校准］）
        var mid: String = "",// string n 消息 ID，标识一条消息的 GUID
        var sid: String = "", // string n 会话 ID，标识一个会话的 GUID
        var type: Int = 0,// int n 会话类型，参考后面的会话类型定义描述
        var name: String = "",// string n 发送者名字，消息头显示
        var from: String = "",// string n 发送者账号
        var fromid: Long = 0,// int n 发送者 ID
        var toid: Long = 0,// int y 单聊的接收者 ID；其它为无
        var flag: Int = 0,// int y 标识，用于功能扩展，参考 Msg***Flag
        var msgs: MutableList<MsgBody>? = null// Msg[] n 消息内容，具体结构参考 Msg
)


class MessageUtils {

    companion object {
        /*@JvmStatic
        fun createTextMsg(toId: Long, content: String): SendMsgReq {
            var msgType = MSG_TYPE_SINGLE_CHAT
            var senderId = IMClient.getCurrentUserId()
            var senderName = IMClient.getCurrentUser()?.cname ?: ""
            var senderAccount = IMClient.account ?: ""
            var msgFlag = 0

            var msgs = mutableListOf<MessageBody>(MessageBody(0, content))
            return SendMsgReq(TimeUtils.getServerTime(),
                    IDUtil.generatorMsgId(),
                    IDUtil.createSingleChatId(senderId, toId),
                    msgType,
                    senderName,
                    senderAccount,
                    senderId,
                    toId,
                    msgFlag,
                    msgs
            )
        }*/
    }
}

class MessageFlag {
    // Message.Flag标识位置功能定义
    companion object {
        val MsgReadFlag = (0x00000001) // Bit0: 0-未读；1-已读
        val MsgReceiptFlag = (0x00000002) // Bit1: 0-不需要回执；1-需要回执
        val MsgReceiptedFlag = (0x00000004) // Bit2: 0-还未回执；1-已经回执(只有当为MsgReceiptFlag时才有用)
        val MsgRecallFlag = (0x00000008) // Bit3: 0-没有撤回的消息；1-已经撤回的消息
    }

}
