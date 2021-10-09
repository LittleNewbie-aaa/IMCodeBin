package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.Conversation

/**
 * 会话表操作
 */

@Dao
interface ConversationDao {
    @Delete
    fun delete(conversation: Conversation)

    @Update
    fun update(conversation: Conversation)

    @Insert
    fun insert(conversation: Conversation)

    @Query("select * from Conversation where sid= :sid")
    fun getById(sid: String): Conversation?

    @Query("select * from Conversation where name like :keyword escape '\\' order by lastMsgDate desc limit :pageSize offset :offset ")
    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Conversation>

    @Query("select * from Conversation")
    fun findAll(): MutableList<Conversation>

    @Query("select sid from Conversation where type=130")
    fun findSubscriptionConversation(): MutableList<String>

    @Query("update Conversation set lastMessageId= :mid , isDelete = 0 , lastMsgDate=:lastMsgDate where sid=:sid")
    fun updateLastMessage(sid: String, mid: String, lastMsgDate: Long): Long

    @Query("update Conversation set type= :type where sid=:sid")
    fun updateType(sid: String, type:Int): Long

    @Query("update Conversation set draft=:msg where sid=:sid")
    fun updateDraft(sid: String, msg: String): Long

    @Query("update Conversation set name= :title where sid=:sid")
    fun updateTitle(sid: String, title: String)

    @Query("delete From Conversation")
    fun deleteAll()
    @Query("delete From Conversation where sid = :sid")
    fun deleteById(sid:String)

}
