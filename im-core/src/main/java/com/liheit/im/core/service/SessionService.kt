package com.liheit.im.core.service

import com.liheit.im.common.ext.AppNameFlag
import com.liheit.im.common.ext.getAppName
import com.liheit.im.core.Constants
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.*
import com.liheit.im.core.bean.SessionType.SESSION_DEPT
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.forEachBlock
import com.liheit.im.core.bean.SessionMember
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson
import java.math.BigInteger

/**
 * 群组数据库操作管理类
 */

object SessionService {
    private val sessionDao get() = DbUtils.currentDB.sessionDao()
    private val sessionMemberDao get() = DbUtils.currentDB.sessionMemberDao()
    private val conversationDao get() = DbUtils.currentDB.convDao()
    private val msgDao get() = DbUtils.currentDB.msgDao()

    fun findById(sid: String): Session? {
        return sessionDao.getSessionById(sid)
    }

    fun findByIds(ids: MutableList<String>): MutableList<Session> {
        return sessionDao.getSessionsByIds(ids)
    }

    fun findAll(): MutableList<Session> {
        return sessionDao.findAll()
    }

    fun findAllByType(type:Int): MutableList<Session> {
        return sessionDao.findAllType(type)
    }

    /**
     * 保存群组数据
     */
    fun save(it: Session) {
        sessionDao.saveOrUpdate(it)
    }
    fun save(sessions: List<Session>) {
        sessionDao.saveOrUpdate(sessions)
    }

    /**
     * 跟新会话标题
     */
    fun updateSessionTitle(session: Session) {
        msgDao.runOnTransaction {
            sessionDao.saveOrUpdate(session)
            ConversationService.updateTitle(session.sid, session.title)
        }
    }

    /**
     * 保存群组信息和人员
     */
    fun saveSessionAndMember(session: Session) {
        msgDao.runOnTransaction {
            save(session)
            session.ids?.forEach {
                sessionMemberDao.insert(
                    SessionMember(
                        session.sid, it, session.admins?.contains(it)
                            ?: false
                    )
                )
            }
        }
    }
    fun saveSessionAndMember(sessions: List<Session>) {
        msgDao.runOnTransaction {
            val groupSession = if(getAppName()==AppNameFlag.THE_HY_FLAG.value){
                sessions.groupBy { it.type == SessionType.REMOVE.value || it.type == SessionType.DISSOLVE.value}
            }else{
                sessions.groupBy { it.type == SessionType.REMOVE.value }
            }
            groupSession.get(true)?.forEach { s ->
                delete(s.sid)
            }
            groupSession.get(false)?.let { newSessionList ->
                sessionDao.saveOrUpdate(newSessionList)

                newSessionList.forEach { s ->
                    if (s.type == SessionType.DISSOLVE.value) {
                        val conversation = IMClient.chatManager.getConversation(s.sid)
                        if (conversation != null) {
                            if(!conversation.isDelete){
                                conversation.draft = ""
                                conversation.isDelete = true
                                conversation.type = SessionType.DISSOLVE.value
                            }
                            conversation.name= s.title
                            ConversationService.update(conversation)
                        }
                    }

                    ConversationService.updateTitle(s.sid, s.title)
                    //删除原来的关系
                    val members = sessionMemberDao.findAllBySid(s.sid)
                    if (members.isNotEmpty()) {
                        sessionMemberDao.deleteAll(members)
                    }
                    //重新插入
                    s.ids?.map { uid ->
                        SessionMember(s.sid, uid, s.admins?.contains(uid) ?: false)
                    }?.let(sessionMemberDao::saveOrUpdate)
                }
            }
        }
    }

    /**
     * 更新群组人员
     */
    fun updateSessionMember(sid: String, ids: MutableList<Long>) {
        msgDao.runOnTransaction {
            sessionMemberDao.findAllBySid(sid)?.let(sessionMemberDao::deleteAll)
            ids.forEach { sessionMemberDao.insert(SessionMember(sid, it)) }
        }
    }

    /**
     * 获取群组人员id
     */
    fun getSessionMemberIds(sid: String): List<Long> {
        val s = findById(sid)
        var uIds = if (s?.type == SESSION_DEPT.value) {
            IMClient.userManager.getUserByDepid(BigInteger(sid.replace("#", ""), 16).toLong()).map { it.id }
        } else {
            sessionMemberDao.getSessionMemberIds(sid)
        }
        var filterIds =mutableListOf<Long>()
        uIds.map {
            var user = IMClient.userManager.getUserById(it)
            if (user?.type != EditAction.DELETE.action) {
                filterIds.add(it)
            }
        }
        return  filterIds.toList()
    }

    fun getSessionMembers(sid: String, offset: Long, pageSize: Int): List<Member> {
        return sessionMemberDao.getSessionMembers(sid, pageSize, offset)
    }

    /**
     * 删除群组
     */
    fun delete(sid: String) {
        msgDao.runOnTransaction {
            sessionDao.delete(sid)
            //删除用户关联表
            sessionMemberDao.deleteBySid(sid)
            conversationDao.deleteById(sid)
            //删除会话消息
            msgDao.deleteBySid(sid)
        }
    }

    /**
     * 删除人员信息
     */
    fun deleteMember(sid: String, ids: MutableList<Long>) {
        msgDao.runOnTransaction {
            ids.toLongArray().forEachBlock(Constants.MAX_SQL_PARAM_SIZE) {
                sessionMemberDao.deleteMember(sid, it.toMutableList())
            }
        }
    }

    /**
     * 设置群组管理员
     */
    fun setAdmins(sid: String, adminIds: List<Long>) {
        msgDao.runOnTransaction {
            sessionMemberDao.resetAdmins(sid)
            sessionMemberDao.setAdmins(sid, adminIds)
        }
    }

    /**
     * 添加或者更新当前的成员
     */
    fun addMember(sid: String, ids: MutableList<Long>) {
        sessionMemberDao.saveOrUpdate(ids.map { com.liheit.im.core.bean.SessionMember(sid, it) })
    }

    /**
     * 获取群组管理员
     */
    fun getSessionAdmins(sid: String): List<Long> {
        return sessionMemberDao.findAllAdminBySid(sid)
    }

    /**
     * 搜索
     */
    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Session> {
        return sessionDao.search(keyword, pageSize, offset)
    }
}
