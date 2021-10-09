package com.liheit.im.core.service

import android.content.SharedPreferences
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.*
import com.liheit.im.core.bean.SessionType.*
import com.liheit.im.core.protocol.FileBody
import com.liheit.im.core.protocol.ImgBody
import com.liheit.im.core.protocol.RecallBody
import com.liheit.im.core.protocol.VoiceChatBody
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.Log
import com.liheit.im.utils.SharedPreferencesUtil

/**
 * 聊天消息数据库操作管理类
 */

object MessageService {

    private val msgDao get() = DbUtils.currentDB.msgDao()
    private val statusDao get() = DbUtils.currentDB.receiptStatusDao()
    private val fileDao get() = DbUtils.currentDB.msgFileDao()

    fun findById(mid: String): ChatMessage? {
        return msgDao.getMessageById(mid)
    }

    fun update(msg: ChatMessage) {
        save(msg)
    }

    fun save(msg: ChatMessage) {
        if (msg.bodyType == MessageType.VOICECHAT.value && (msg.getMessageBody() as? VoiceChatBody)?.audiotype == 5) {
            return
        }
        msgDao.runOnTransaction {
            if (msg.fromid == IMClient.getCurrentUserId() && msg.type != SESSION_P2P.value && msg.isNeedReceipt()) {
                when (msg.type) {
                    SESSION_FIX.value,
                    SESSION_DEPT.value,
                    SESSION.value,
                    SESSION_DISC.value -> {
                        val sessionMembers = SessionService.getSessionMemberIds(msg.sid)
                        //判断当前用户是否存在，如果存在则发送，否则停止
                        var filterNum = sessionMembers.filter {
                            var ss = UserService.findById(it)
                            var isExit: Boolean
                            isExit = ss != null
                            isExit
                        }
                        msg.unReceiptCount = filterNum.size ?: 0
                        filterNum.forEach { member ->
                            if (member != IMClient.getCurrentUserId())//当前用户的id
                                statusDao.insert(ReceiptStatus(msg.mid, member, false))
                        }
                    }
                }
            }
            msg.preProcess()
            msgDao.saveOrUpdate(msg)
            saveAttach(msg)
        }
    }

    fun deleteHistory(sid: String) {
        msgDao.deleteBySid(sid)
    }

    fun peekMessage(sid: String): ChatMessage? {
        return msgDao.peekMessage(sid)
    }

    fun insertBatch(msgs: List<ChatMessage>) {
        msgs.forEach { it.preProcess() }
        runInTransaction {
            //处理所有的回执消息
            msgs.forEach { msg ->
                if (msg.fromid == IMClient.getCurrentUserId() && msg.type != SESSION_P2P.value && msg.isNeedReceipt()) {
                    when (msg.type) {
                        SESSION_FIX.value,
                        SESSION_DEPT.value,
                        SESSION.value,
                        SESSION_DISC.value -> {
                            val sessionMembers = SessionService.getSessionMemberIds(msg.sid)
                            var filterNum = sessionMembers.filter {
                                var ss = UserService.findById(it)
                                var isExit: Boolean
                                isExit = ss != null
                                isExit
                            }
                            msg.unReceiptCount = filterNum.size - 1
                            filterNum.filter { it != msg.fromid }.forEach { member ->
                                if (member != IMClient.getCurrentUserId())
                                    statusDao.insert(ReceiptStatus(msg.mid, member, false))
                            }
                        }
                    }
                }
            }

            msgDao.insertBatch(msgs)

            batchSaveAttach(msgs)
        }
    }

    fun insert(msg: ChatMessage) {
        if (msg.bodyType == MessageType.VOICECHAT.value && (msg.getMessageBody() as? VoiceChatBody)?.audiotype == 5) {
            return
        }
        msgDao.insert(msg)
        saveAttach(msg)
    }

    /**
     * 获取回执消息回执人数
     * mid: 消息id
     * receipted  是否回执（true 回执人数  false 未回执人数）
     */
    fun getReceiptedCount(mid: String, receipted: Boolean): Long {
        return statusDao.getReceiptedCount(mid, receipted)
    }

    fun findPage(sid: String, bodyType: MessageType, pageSize: Int, offset: Long): MutableList<ChatMessage> {
        return msgDao.findPage(sid, bodyType.value, pageSize, offset)
    }

    fun messageAddFlag(mid: String, flag: Int) {
        msgDao.addFlag(mid, flag)
    }

    private inline fun runInTransaction(crossinline action: () -> Unit) {
        msgDao.runOnTransaction {
            action.invoke()
        }
    }

    fun saveHistoryMsgs(msgs: MutableList<ChatMessage>?) {
        msgs?.let {
            it.forEach { it.preProcess() }
            runInTransaction {
                msgDao.saveOrUpdateBatch(it)
                batchSaveAttach(it)
            }
        }
    }

    private fun saveAttach(msg: ChatMessage) {
        if (msg.msgs != null && msg.msgs?.size ?: -1 > 0 && msg.msgs!![0].mtype == MessageType.ANNEX.value) {
            fileDao.saveOrUpdate(convertToFile(msg))
        }
    }

    private fun convertToFile(msg: ChatMessage): MessageFile {
        val body = msg.getMessageBody()!!
        if (body is FileBody) {
            var width = 0
            var height = 0
            if (body is ImgBody) {
                width = body.sizew
                height = body.sizeh
            }
            return MessageFile(mid = msg.mid, sid = msg.sid, isReceive = msg.fromid != IMClient.getCurrentUserId(),
                    type = MessageType.ANNEX.value, token = body.token,
                    bytes = body.bytes,
                    sizew = width,
                    sizeh = height,
                    md5 = body.md5, t = msg.t,
                    name = body.name, localPath = body.localPath)
        } else {
            throw RuntimeException("cannot convert ${body::class.java.name} to MessageFile")
        }
    }

    private fun batchSaveAttach(msgs: List<ChatMessage>) {
        msgs?.filter { msg -> msg.msgs != null && msg.msgs?.size ?: -1 > 0 && msg.msgs!![0].mtype == MessageType.ANNEX.value }
                ?.map { msg -> convertToFile(msg) }?.let {
                    if (it.isNotEmpty()) {
                        fileDao.saveOrUpdateBatch(it)
                    }
                }
    }

    //回执消息的处理
    fun insertMessageAndReceiptStatus(msg: ChatMessage) {
        if (msg.bodyType == MessageType.VOICECHAT.value && (msg.getMessageBody() as? VoiceChatBody)?.audiotype == 5) {
            return
        }
        msgDao.runOnTransaction {
            msg.save()
            if (msg.fromid == IMClient.getCurrentUserId() && msg.type != SESSION_P2P.value && msg.isNeedReceipt()) {
                when (msg.type) {
                    SESSION_FIX.value,
                    SESSION_DEPT.value,
                    SESSION.value,
                    SESSION_DISC.value -> {
                        val sessionMembers = SessionService.getSessionMemberIds(msg.sid)
                        var filterNum = sessionMembers.filter {
                            var ss = UserService.findById(it)
                            var isExit: Boolean
                            isExit = ss != null
                            isExit
                        }
                        msg.unReceiptCount = filterNum.size - 1
                        filterNum.filter { it != msg.fromid }.forEach { member ->
                            if (member != IMClient.getCurrentUserId())
                                statusDao.insert(ReceiptStatus(msg.mid, member, false))
                        }
                    }
                }
            }
        }
    }

    fun setStatus(oldStatus: Int, newStatus: Int) {
        return msgDao.resetAllStatus(oldStatus, newStatus)
    }

    fun searchMessage(sid: String, keyword: String): MutableList<ChatMessage> {
        return msgDao.searchMessage(sid, "%${keyword}%")
    }

    fun searchMessage(sid: String, keyword: String, startTime: Long, pageSize: Int): MutableList<ChatMessage> {
        return msgDao.searchMessage(sid, keyword, startTime, pageSize)
    }

    fun searchMessageFile(keyword: String, pageSize: Int, offset: Long = 0): MutableList<MessageFile> {
        return fileDao.searchMessageFile(keyword, pageSize, offset)
    }

    fun searchMessageFileBySid(sid: String, keyword: String, pageSize: Int, offset: Long): MutableList<MessageFile> {
        return fileDao.searchMessageFileBySid(sid, keyword, pageSize, offset)
    }

    fun searchMessageByGroup(keyword: String, pageSize: Int, offset: Long): List<MessageGroup> {
        return msgDao.searchByGroup(keyword, pageSize, offset)
    }

    fun searchMessageCount(sid: String, keyword: String): Long {
        return msgDao.searchMessageCount(sid, keyword)
    }

    fun loadMessageUntil(sid: String, mid: String): MutableList<ChatMessage> {
        var msg = findById(mid)
        var msgs = msgDao.loadMessageUntil(sid, msg!!.t)
        msgs.forEach {
            if (it.type != SESSION_P2P.value && it.isNeedReceipt()) {
                it.receiptCount = getReceiptedCount(it.mid, true).toInt()
                it.unReceiptCount = getReceiptedCount(it.mid, false).toInt()
            }
        }
        return msgs
    }

    fun getMsgUsersReceiptStatus(mid: String): MutableList<ReceiptStatus> {
        return statusDao.findByMid(mid)
    }

    fun setUserReceipted(mid: String, fromid: Long) {
        statusDao.findByMidAndUid(mid, fromid)?.let {
            it.isReceipted = true
            statusDao.update(it)
        }
    }

    fun messageIsAllReceipted(mid: String): Boolean {
        return statusDao.getReceiptedCount(mid, false) == 0L
    }

    fun deleteConversation(sid: String, deleteMsg: Boolean) {
        msgDao.runOnTransaction {
            ConversationService.delete(sid)
            if (deleteMsg) {
                //TODO
                /*SQLite.delete()
                        .from(ChatMessage::class.java)
                        .where(ChatMessage_Table.sid.eq(sid))
                        .execute()*/
            }
        }
    }

    fun deleteConversationMessage(sid: String, deleteMsg: Boolean) {
        msgDao.deleteBySid(sid)
    }

    fun getUnReadMessagesBySid(sid: String): MutableList<ChatMessage> {
        return msgDao.findMessagesBySidAndFlag(sid, ChatMessage.MASK_READ, ChatMessage.FLAG_UNREAD)
    }

    fun getUnReadMessageCount(sid: String): Long {
        val unReadNum = msgDao.findMessagesCountBySidAndFlag(sid, ChatMessage.MASK_READ, ChatMessage.FLAG_UNREAD)
        val setUpUnRead = SharedPreferencesUtil.getInstance(IMClient.context).getSP("${sid}UnRead").toIntOrNull() ?: 0
        return unReadNum + setUpUnRead
    }

    fun setUnReadMessageCount(sid: String, unReadNum: String) {
        SharedPreferencesUtil.getInstance(IMClient.context).putSP("${sid}UnRead", unReadNum)
    }

    fun getUnReadMessageCount(): Long {
        return msgDao.getMessageCountByFlag(ChatMessage.MASK_READ, ChatMessage.FLAG_UNREAD)
    }

    fun setRead(msgList: MutableList<ChatMessage>) {
        msgDao.runOnTransaction {
            msgList.forEach {
                it.flag = it.flag or ChatMessage.FLAG_READ
            }
            msgDao.updateAll(msgList)
        }
    }

    fun setMsgRead(sid: String, maxReadTime: Long) {
        msgDao.addFlagBySid(sid, ChatMessage.FLAG_READ, maxReadTime, ChatMessage.FLAG_UNREAD, ChatMessage.MASK_READ)
    }

    fun getLastMessage(sid: String): ChatMessage? {
        return msgDao.findLastMessage(sid)
    }

    fun getLastTypeMessage(sid: String, bodyType: Int): ChatMessage? {
        return msgDao.findLastTypeMessage(sid, bodyType)
    }

    fun delete(mid: String) {
        msgDao.findById(mid)?.let(msgDao::delete)
    }

    fun findAllMessage(sid: String): MutableList<ChatMessage> {
        var msgs = msgDao.findAllBySid(sid)
        return msgs
    }

    fun findMessage(sid: String, startMsgId: String?, pageSize: Int, isUp: Boolean): MutableList<ChatMessage> {
        var startTime = 0L
        if (!startMsgId.isNullOrEmpty()) {
            var msg = findById(startMsgId)
            if (msg != null) {
                startTime = msg.t
            }
        }

        var msgs = if (isUp)
            if (startTime == 0L) {
                msgDao.findUpBySidPage(sid, pageSize)
            } else {
                msgDao.findUpBySidPage(sid, startTime, pageSize)
            }
        else
            if (startTime == 0L) {
                msgDao.findDownBySidPage(sid, pageSize)
            } else {
                msgDao.findDownBySidPage(sid, startTime, pageSize)
            }

        msgs.forEach {
            if (it.type != SESSION_P2P.value && it.isNeedReceipt()) {
                it.receiptCount = getReceiptedCount(it.mid, true).toInt()
                it.unReceiptCount = getReceiptedCount(it.mid, false).toInt()
            }
        }

        return msgs
    }

    fun getLastMsgTime(): Long {
        return msgDao.getLastMsgTime()
    }

    fun deleteMessageFile(mid: String) {
        fileDao.delete(mid)
    }

    fun recallMessage(recallMessage: ChatMessage) {
        msgDao.runOnTransaction {
            (recallMessage.getMessageBody() as? RecallBody)?.let { body ->
                msgDao.deleteById(body.mid)
                recallMessage.t = body.t
            }
            msgDao.saveOrUpdate(recallMessage)
        }
    }
}

fun ChatMessage.save() {
    this.preProcess()
    val msg = MessageService.findById(this.mid)
    if (msg == null) {
        MessageService.insert(this)
    } else {
        MessageService.update(this)
    }
}