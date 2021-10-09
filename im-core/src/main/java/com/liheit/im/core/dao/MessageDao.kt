package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.*
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson

/**
 * Created by daixun on 2018/7/10.
 * 用于聊天信息维护的数据库抽象类
 */
@Dao
abstract class MessageDao {


    @Query("delete From ChatMessage")
    abstract fun deleteAll()

    @Insert
    abstract fun insert(msg: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveOrUpdate(msg: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveOrUpdateBatch(msg: List<ChatMessage>)

    @Transaction
    open fun insertBatch(msgs: List<ChatMessage>) {
        msgs.forEach { m ->
            val msg = getMessageById(m.mid)
            if (msg == null) {
                try {
                    insert(m)
                } catch (e: Exception) {
                    Log.e("消息格式错误msg=${gson.toJson(m)}")
                }
            }
        }
    }

    @Transaction
    open fun runOnTransaction(exec: () -> Unit) {
        exec.invoke()
    }

    @Query("select * from ChatMessage  where mid = :mId")
    abstract fun getMessageById(mId: String): ChatMessage?

    @Query("update ChatMessage set flag = (flag | :flag) where mid = :mid")
    abstract fun addFlag(mid: String, @MsgFlag flag: Int)

    @Query("update ChatMessage set flag = (flag | :flag) where sid = :sid and t <= :lastReadTime and flag & :flagMask =:nowFlag")
    abstract fun addFlagBySid(
        sid: String,
        @MsgFlag flag: Int,
        lastReadTime: Long,
        nowFlag: Int,
        flagMask: Int
    )

    @Query("select count(*) from ChatMessage where flag & :flag = :f ")
    abstract fun getMessageCountByFlag(flag: Int, f: Int): Long

    @Query("update ChatMessage set sendStatus = :newStatus where sendStatus = :oldStatus")
    abstract fun resetAllStatus(oldStatus: Int, newStatus: Int)
    /*

    fun saveHistoryMsgs(msgs: MutableList<ChatMessage>?) {
        msgs?.let { ms ->
            FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
                ms.forEach {
                    it.preProcess()
                    it.save()
                    saveAttach(it)
                }
            }
        }
    }

    fun saveMsg(msg: ChatMessage) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            msg.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
            msg.preProcess()
            msg.save()
            saveAttach(msg)
        }
    }

    fun saveAttach(msg: ChatMessage) {
        if (msg.msgs != null && msg.msgs?.size ?: -1 > 0 && msg.msgs!![0].mtype == ChatMessage.MSG_TYPE_ANNEX) {
            val body = msg.msgs!![0]
            MessageFile(mid = msg.mid, sid = msg.sid, isReceive = msg.fromid != IMClient.getCurrentUserId(),
                    type = ChatMessage.MSG_TYPE_ANNEX, token = body.token, bytes = body.bytes,
                    sizew = body.sizew, sizeh = body.sizeh, md5 = body.md5, t = msg.t,
                    name = body.name).save()
        }
    }*/

    /*fun messageAddFlag(mid: String, flag: Int) {
        var name = ChatMessage_Table.flag.nameAlias.fullName()
        SQLite.update(ChatMessage::class.java)
                .set(UnSafeStringOperator("${name}=(${name} | ?)", arrayOf(flag.toString())))
                .where(ChatMessage_Table.mid.eq(mid))
                .execute()
    }*/

    /*fun setUserReceipted(mid: String, uid: Long) {
        SQLite.update(ReceiptStatus::class.java)
                .set(ReceiptStatus_Table.isReceipted.`is`(true))
                .where(ReceiptStatus_Table.mId.eq(mid))
                .and(ReceiptStatus_Table.uid.eq(uid))
                .executeUpdateDelete()
    }

    fun getReceiptedCount(mid: String, receipted: Boolean): Long {
        return SQLite.select(Method.count(ReceiptStatus_Table.isReceipted)).from(ReceiptStatus::class.java)
                .where(ReceiptStatus_Table.mId.eq(mid))
                .and(ReceiptStatus_Table.isReceipted.`is`(receipted))
                .longValue()
    }

    fun getMsgUsersReceiptStatus(mid: String): MutableList<ReceiptStatus> {
        return SQLite.select()
                .from(ReceiptStatus::class.java)
                .where(ReceiptStatus_Table.mId.eq(mid))
                .queryList()

    }

    fun save(msg: ChatMessage) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            if (msg.fromid == IMClient.getCurrentUserId() && msg.type != MSG_TYPE_SINGLE_CHAT && msg.isNeedReceipt()) {
                when (msg.type) {
                    MSG_TYPE_GROUP_FIXED,
                    MSG_TYPE_GROUP_DEPARTMENT,
                    MSG_TYPE_GROUP_NORMAL,
                    MSG_TYPE_GROUP_TEMP -> {
                        val sessionMembers = SessionDao.getSessionMemberIds(msg.sid)
                        msg.unReceiptCount = sessionMembers.size ?: 0
                        sessionMembers.forEach { member ->
                            if (member != IMClient.getCurrentUserId())
                                ReceiptStatus(msg.mid, member, false).save()
                        }

                    }
                }
            }
            msg.preProcess()
            msg.save()
        }
    }

    fun deleteConversation(sid: String, deleteMsg: Boolean) {
        runInTransaction {
            SQLite.update(Conversation::class.java)
                    .set(Conversation_Table.isDelete.`is`(true))
                    .where(Conversation_Table.sid.eq(sid))
                    .execute()
            if (deleteMsg) {
                SQLite.delete()
                        .from(ChatMessage::class.java)
                        .where(ChatMessage_Table.sid.eq(sid))
                        .execute()
            }
        }
    }*/

    /*fun topConversation(sid: String, isTop: Boolean) {
        SQLite.update(Conversation::class.java)
                .set(Conversation_Table.isTop.`is`(isTop))
                .where(Conversation_Table.sid.eq(sid))
                .execute()

    }

    private fun runInTransaction(call: () -> Unit) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            call.invoke()
        }
    }*/

    @Update
    abstract fun update(msg: ChatMessage)

    @Update()
    abstract fun updateAll(msg: List<ChatMessage>)

    @Query("select * from ChatMessage where sid = :sid and flag & :flag = :f")
    abstract fun findMessagesBySidAndFlag(sid: String, flag: Int, f: Int): MutableList<ChatMessage> /*{
        var name = ChatMessage_Table.flag.nameAlias.fullName()
        return SQLite.select().from(ChatMessage::class.java)
                .where(ChatMessage_Table.mid.eq(sid))
                .and(UnSafeStringOperator("${name} & ? != ? ", arrayOf(ChatMessage.FLAG_READ.toString(), ChatMessage.FLAG_READ.toString())))
                .queryList()
    }*/

    @Query("select count(*) from ChatMessage where sid = :sid and flag & :flag = :f")
    abstract fun findMessagesCountBySidAndFlag(sid: String, flag: Int, f: Int): Long

    @Query("select * from ChatMessage where sid = :sid order by t desc limit 1")
    abstract fun findLastMessage(sid: String): ChatMessage?

    @Query("select * from ChatMessage where sid = :sid and bodyType = :bodyType order by t desc limit 1")
    abstract fun findLastTypeMessage(sid: String, bodyType: Int): ChatMessage?
/*
    fun messageIsAllReceipted(mid: String): Boolean {
        return SQLite.select()
                .from(ReceiptStatus::class.java)
                .where(ReceiptStatus_Table.mId.eq(mid))
                .and(ReceiptStatus_Table.isReceipted.`is`(false))
                .longValue() > 0
    }

    fun removeMessage(mid: String) {
        SQLite.delete().from(ChatMessage::class.java)
                .where(ChatMessage_Table.mid.eq(mid))
                .queryList()
    }*/

    @Query("select * from ChatMessage where sid= :sid and content like :keyword escape '\\' order by t desc")
    abstract fun searchMessage(sid: String, keyword: String): MutableList<ChatMessage>

    @Query("select * from ChatMessage where sid= :sid and content like :keyword escape '\\' and t < :startTime order by t desc limit :pageSize")
    abstract fun searchMessage(sid: String, keyword: String, startTime: Long, pageSize: Int): MutableList<ChatMessage> /*{
        return SQLite.select()
                .from(ChatMessage::class.java)
                .where(ChatMessage_Table.sid.eq(sid))
                .and(ChatMessage_Table.content.like(keyword))
                .and(ChatMessage_Table.t.lessThan(startTime))
                .orderBy(OrderBy.fromProperty(ChatMessage_Table.t).ascending())
                .limit(pageSize)
                .queryList()
    }*/


    @Query("select * from ChatMessage where sid = :sid and bodyType = :bodyType order by t asc limit :pageSize Offset :offset")
    abstract fun findPage(
        sid: String,
        bodyType: Int,
        pageSize: Int,
        offset: Long
    ): MutableList<ChatMessage>

    /*fun searchMessageFile(keyword: String, pageSize: Int): MutableList<MessageFile> {
        return SQLite.select()
                .from(MessageFile::class.java)
                .where(MessageFile_Table.name.like(keyword))
//                .and(MessageFile_Table.t.lessThan(startTime))
                .orderBy(OrderBy.fromProperty(MessageFile_Table.t).ascending())
                .limit(pageSize)
                .queryList()
    }*/

    @Query("select count(*) from ChatMessage where sid = :sid and content like :keyword")
    abstract fun searchMessageCount(sid: String, keyword: String): Long /*{
        return SQLite.select(Method.count(ChatMessage_Table.sid))
                .from(ChatMessage::class.java)
                .where(ChatMessage_Table.sid.eq(sid))
                .and(ChatMessage_Table.content.like(keyword))
                .longValue()
    }*/

    @Query("select sid, type, count(*) as count from ChatMessage where content like :keyword escape '\\' group by sid limit :pageSize offset :offset")
    abstract fun searchByGroup(keyword: String, pageSize: Int, offset: Long): List<MessageGroup>

    @Query("select * from ChatMessage where sid= :sid limit 1")
    abstract fun peekMessage(sid: String): ChatMessage?

    @Query("select * from ChatMessage where sid = :sid and t >= :t order by t desc")
    abstract fun loadMessageUntil(sid: String, t: Long): MutableList<ChatMessage> /*{
        val msg = getMessageById(mid)
        return SQLite.select()
                .from(ChatMessage::class.java)
                .where(ChatMessage_Table.sid.eq(sid))
                .and(ChatMessage_Table.t.greaterThanOrEq(msg!!.t))
                .orderBy(OrderBy.fromProperty(ChatMessage_Table.t).descending())
                .queryList()
    }*/

    @Delete
    abstract fun delete(m: ChatMessage)

    @Query("delete from ChatMessage where mid = :mid")
    abstract fun deleteById(mid: String)

    @Query("delete from ChatMessage where mid in (:mids)")
    abstract fun deleteByIds(mids: List<String>)

    @Query("select * from ChatMessage where mid = :mid")
    abstract fun findById(mid: String): ChatMessage?

    @Query("select * from ChatMessage where sid = :sid and t < :t order by t desc  limit :pageSize")
    abstract fun findUpBySidPage(sid: String, t: Long, pageSize: Int): MutableList<ChatMessage>

    @Query("select * from ChatMessage where sid = :sid and t > :t order by t desc limit :pageSize")
    abstract fun findDownBySidPage(sid: String, t: Long, pageSize: Int): MutableList<ChatMessage>

    @Query("select * from ChatMessage where sid = :sid order by t desc  limit :pageSize")
    abstract fun findUpBySidPage(sid: String, pageSize: Int): MutableList<ChatMessage>

    @Query("select * from ChatMessage where sid = :sid order by t desc limit :pageSize")
    abstract fun findDownBySidPage(sid: String, pageSize: Int): MutableList<ChatMessage>

    @Query("delete from ChatMessage where sid = :sid")
    abstract fun deleteBySid(sid: String)

    @Query("select t from ChatMessage order by t desc limit 0,1")
    abstract fun getLastMsgTime(): Long

    @Query("select * from ChatMessage where sid = :sid order by t desc")
    abstract fun findAllBySid(sid: String): MutableList<ChatMessage>
}

@Dao
interface ReceiptStatusDao {

    @Query("select count(*) from ReceiptStatus where mId= :mid and isReceipted = :isReceipted")
    fun getReceiptedCount(mid: String, isReceipted: Boolean): Long

    @Query("select * from ReceiptStatus where mid = :mid")
    fun findByMid(mid: String): MutableList<ReceiptStatus>

    @Query("select * from ReceiptStatus where mid = :mid and uid = :uid")
    fun findByMidAndUid(mid: String, uid: Long): ReceiptStatus

    @Update
    fun update(it: ReceiptStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(status: ReceiptStatus)

    @Query("DELETE FROM ReceiptStatus")
    fun deleteAll()
}

/**
 * 聊天文件表操作
 */
@Dao
interface MessageFileDao {
    @Query("select * from MessageFile where mid =:mid")
    fun findById(mid: String): MessageFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdate(m: MessageFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdateBatch(m: List<MessageFile>)

    @Update
    fun update(m: MessageFile)

    @Query("select * from MessageFile where name like :keyword limit :pageSize offset :offset")
    fun searchMessageFile(keyword: String, pageSize: Int, offset: Long): MutableList<MessageFile>

    @Query("select * from MessageFile where sid=:sid and name like :keyword limit :pageSize offset :offset")
    fun searchMessageFileBySid(
        sid: String,
        keyword: String,
        pageSize: Int,
        offset: Long
    ): MutableList<MessageFile>


    @Query("DELETE FROM MessageFile")
    fun deleteAll()

    @Query("DELETE FROM MessageFile where mid=:mid")
    fun delete(mid: String): Int

}
