package com.liheit.im.core

import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.Conversation
import com.liheit.im.core.bean.SessionType
import com.liheit.im.core.protocol.*
import com.liheit.im.utils.IDUtil
import com.liheit.im.utils.Log
import com.liheit.im.utils.TimeUtils

class MessageBuilder {
    companion object {
        //创建撤回消息
        @JvmStatic
        fun createRecallMessage(msg: ChatMessage): ChatMessage {
            return createMessage(msg.sid, msg.type, null).apply {
                msgs = arrayListOf(RecallBody(msg.mid, msg.t))
                flag = flag or ChatMessage.FLAG_RECALLED
            }
        }

        //创建回执消息
        @JvmStatic
        fun createReceipt(msg: ChatMessage): ChatMessage {
            return createMessage(msg.sid, msg.type, null).apply {
                msgs = arrayListOf(ReceiptBody(msg.mid))
                flag = flag or ChatMessage.FLAG_RECEIPT_TO
                toid = msg.fromid
            }
        }

        //创建文本类型消息
        @JvmStatic
        fun createTextMsg(sid: String, content: String, chatType: Int, refsMid: String? = null, isTracelessMsg: Boolean? = false): ChatMessage {
            return ChatMessage.createTextMsg(sid, content, chatType, refsMid, if (isTracelessMsg == true) 1 else 0)
        }

        //创建图片类型消息
        @JvmStatic
        fun createImgMsg(conversationId: String, filePath: String, chatType: Int, isTracelessMsg: Boolean? = false): ChatMessage {
            return ChatMessage.createImgMsg(conversationId, filePath, chatType, if (isTracelessMsg == true) 1 else 0)
        }

        //创建聊天记录类型消息
        @JvmStatic
        fun createMergeForwardMessage(conversation: Conversation, msgIds: MutableList<String>, msgtitle: String): ChatMessage {
            return ChatMessage.createMergeForwardMessage(conversation, msgIds, msgtitle)
        }

        //创建转发消息
        @JvmStatic
        fun forwardMsg(msg: ChatMessage, sid: String, type: Int): ChatMessage {
            return ChatMessage.forwardMsg(msg, sid, type)
        }

        //创建位置类型消息
        @JvmStatic
        fun createLocationMsg(sid: String, address: String, buildingName: String, latitude: Double, longitude: Double, chatType: Int): ChatMessage {
            return ChatMessage.createLocationMsg(sid, address, buildingName, latitude, longitude, chatType)
        }

        //创建视频类型消息
        @JvmStatic
        fun createVideoMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            return ChatMessage.createVideoMsg(sid, filePath, chatType)
        }

        //创建群通知类型消息(暂时没用 群通知都是服务器发送的)
        @JvmStatic
        fun createLocalNoticeMsg(sid: String, chatType: Int, ct: Long,
                                 flag: Long, type: Int, title: String, cid: Long): ChatMessage {
            return ChatMessage.createLocalNoticeMsg(sid, chatType, ct, flag, type, title, cid)
        }

        //创建语音类型消息
        @JvmStatic
        fun createVoiceMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            return ChatMessage.createVoiceMsg(sid, filePath, chatType)
        }

        //创建语音视频通话类型消息
        @JvmStatic
        fun createVoiceChatMsg(sid: String, chatType: Int, roomId: Int, audioType: Int,
                               toIDs: MutableList<Long>, addids: MutableList<Long>? = null,
                               createid: Long,trtctype:Int, inviterid: Long): ChatMessage {
            return ChatMessage.createVoiceChatMsg(sid, chatType, roomId, audioType, toIDs, addids, createid,trtctype,inviterid)
        }

        //创建视频会议类型消息
        @JvmStatic
        fun createVideoConference(sid: String, chatType: Int,
                                  createrid: String, protocoljoinurl: String,
                                  protocolhoststarturl: String, hoststarturl: String,
                                  joinurl: String, confparties: Int, duration: Int,
                                  confNumber: String, confid: String, confname: String,
                                  toIDs: MutableList<Long>, confstarttime: Long): ChatMessage {
            return ChatMessage.createVideoConferenceMsg(sid, chatType, createrid, protocoljoinurl,
                    protocolhoststarturl, hoststarturl, joinurl, confparties,
                    duration, confNumber, confid, confname, toIDs, confstarttime)
        }

        /**
         * 创建文件类型的消息
         * conversationId sessionid 也就是会话id
         * filepath :文件路径
         * chatType: 聊天类型 点对点 群组 等
         */
        @JvmStatic
        fun createFileMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            return ChatMessage.createFileMsg(sid, filePath, chatType)
        }

        //创建投票类型消息
        @JvmStatic
        fun createVoteChatMsg(sid: String, chatType: Int, createuserid: Long, voteid: Long, title: String,
                              invalidtime: Long, options: MutableList<String>): ChatMessage {
            return ChatMessage.createVoteChatMsg(sid, chatType, createuserid, voteid, title, invalidtime, options)
        }

        //创建接龙类型消息
        @JvmStatic
        fun createSolitaireChatMsg(sid: String, chatType: Int,title: String,//接龙标题
                                   example: String,//接龙例子
                                   chainsId: String,//接龙id
                                   itemList: MutableList<String>): ChatMessage {
            return ChatMessage.createSolitaireChatMsg(sid, chatType, title, example, chainsId, itemList)
        }

        //生成主消息
        private fun createMessage(sid: String, chatType: Int, refsMid: String? = null): ChatMessage {
            var senderId = IMClient.getCurrentUserId()
            var senderName = IMClient.getCurrentUser()?.cname ?: ""
            var senderAccount = IMClient.getCurrentUserAccount() ?: ""
            var msgFlag = 0

            var toId = if (chatType == SessionType.SESSION_P2P.value) {
                IDUtil.parseTargetId(IMClient.getCurrentUserId(), sid)
            } else {
                0L
            }
            var msgs = arrayListOf<MsgBody>()
            if (refsMid != null) {
                var refsMsg = IMClient.chatManager.getMessageById(refsMid)
                if (refsMsg != null) {
                    val head = RefsHeadBody().apply {
                        fromid = refsMsg.fromid
                        mid = refsMsg.mid
                        text = "「${refsMsg.name}:"
                    }

                    var end = RefsEndBody().apply {
                        id = refsMsg.fromid
                        text = "\n-----------------------------\n"
                    }
                    msgs.add(head)
                    refsMsg.msgs?.forEach { m ->
                        if (m is AtBody) {
                            msgs.add(TextBody(secrecy=0,text = m.name))
                        } else {
                            msgs.add(m)
                        }
                    }
                    msgs.add(end)
                }
            }
            return ChatMessage(
                    mid = IDUtil.generatorMsgId(),
                    t = TimeUtils.getServerTime(),
                    //TODO sessionId 需要先判断消息是发送给谁的，然后根据规则生成
                    sid = sid,
                    //TODO 这里也是
                    type = chatType,
                    name = senderName,
                    from = senderAccount,
                    fromid = senderId,
                    //TODO 单聊的接收者 ID;其它为无
                    toid = toId,
                    flag = msgFlag,
                    sendStatus = ChatMessage.SEND_STATUS_SENDING,
                    msgs = msgs)
        }

    }
}