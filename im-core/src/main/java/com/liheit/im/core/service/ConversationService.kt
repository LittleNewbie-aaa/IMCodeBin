package com.liheit.im.core.service

import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.Conversation
import com.liheit.im.core.bean.MessageType
import com.liheit.im.core.bean.SessionType
import com.liheit.im.core.protocol.VoiceChatBody
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.IDUtil
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson

/**
 * 会话管理器
 */

object ConversationService {

    private val convDao get() = DbUtils.currentDB.convDao()
    private val msgDao get() = DbUtils.currentDB.msgDao()

    fun findById(sid: String): Conversation? {
        return convDao.getById(sid)
    }

    fun findAll(): MutableList<Conversation> {
        return convDao.findAll()
    }

    /**
     * 获取所有公众号会话
     */
    fun findSubscriptionConversation(): MutableList<String> {
        return convDao.findSubscriptionConversation()
    }

    /**
     * 删除会话
     */
    fun delete(sid: String) {
        convDao.deleteById(sid)
        IMClient.chatManager.triggerConversationListener("")
    }

    /**
     * 更新会话
     */
    fun update(conv: Conversation) {
        convDao.update(conv)
    }

    /**
     * 更新会话标题
     */
    fun updateTitle(sid: String, title: String) {
        convDao.updateTitle(sid, title)
    }

    /**
     * 插入会话
     */
    fun insert(conv: Conversation) {
        convDao.insert(conv)
    }

    /**
     * 搜索会话
     */
    fun search(keyword: String, pageSize: Int, offset: Long = 0): MutableList<Conversation> {
        return convDao.search(keyword, pageSize, offset)
    }

    /**
     * 更新会话
     */
    fun updateConversation(msg: ChatMessage) {
        if (msg.bodyType == MessageType.VOICECHAT.value && (msg.getMessageBody() as? VoiceChatBody)?.audiotype == 5) {
            return
        }
        val updateCount = convDao.updateLastMessage(msg.sid, msg.mid, msg.t)
        if (updateCount <= 0 && IMClient.getCurrentUserId() != 0L) {
            var sessionExist = true
            var convName = when (msg.type) {
                com.liheit.im.core.bean.SessionType.FILE_HELP.value,
                com.liheit.im.core.bean.SessionType.SESSION_P2P.value -> {
                    var uid = IDUtil.parseTargetId(IMClient.getCurrentUserId(), msg.sid)
                    UserService.findById(uid)?.cname ?: ""
                }
                com.liheit.im.core.bean.SessionType.DISSOLVE.value,
                com.liheit.im.core.bean.SessionType.SESSION_FIX.value,
                com.liheit.im.core.bean.SessionType.SESSION_DISC.value,
                com.liheit.im.core.bean.SessionType.SESSION_DEPT.value,
                com.liheit.im.core.bean.SessionType.SESSION.value -> {
                    val group = SessionService.findById(msg.sid)
                    sessionExist = group != null
                    group?.title ?: ""
                }
                com.liheit.im.core.bean.SessionType.OFFICIAL_ACCOUNTS.value -> {
                    SubscriptionService.findBySid(msg.sid)?.name?:""
                }
//                com.liheit.im.core.bean.SessionType.SYSTEM_NOTICE.value,
//                com.liheit.im.core.bean.SessionType.SYSTEM_NOTICEUSERS.value -> "系统通知"
//                com.liheit.im.core.bean.SessionType.WEB_APP_NOTICE.value -> "轻应用通知"
                else -> {
                    msg.name
                }
            }
            val session = SessionService.findById(msg.sid)
            val conversation = Conversation(sid = msg.sid, name = convName, type = session?.type ?: msg.type,
                lastMessageId = msg.mid, lastMsgDate = msg.t, isTop = false, isNotification = false, draft = "")
            Log.e("aaa conversation插入 ${gson.toJson(conversation)}")
            convDao.insert(conversation)
        }else if(msg.type == SessionType.OFFICIAL_ACCOUNTS.value){
            val conversation = Conversation(sid = msg.sid, name = SubscriptionService.findBySid(msg.sid)?.name?:"",
                    type = SessionType.OFFICIAL_ACCOUNTS.value, lastMessageId = msg.mid,
                    lastMsgDate = msg.t, isTop = false, isNotification = false, draft = "")
            convDao.update(conversation)
        }
    }

    /**
     * 更新会话草稿
     */
    fun updateDraft(sid: String, msg: String) {
        convDao.updateDraft(sid, msg)
    }

    /**
     * 更新会话类型
     */
    fun updateType(sid: String, type: Int) {
        convDao.updateType(sid, type)
    }
}

