package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.Member
import com.liheit.im.core.bean.Session
import com.liheit.im.core.bean.SessionMember
import com.liheit.im.core.bean.User

/**
 * 群组表操作
 */
@Dao
interface SessionDao {

    @Query("DELETE FROM session")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdate(s: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdate(s: List<Session>)

    @Query("SELECT * FROM session WHERE sid = :sid")
    fun getSessionById(sid: String): Session?

    @Query("SELECT * FROM session WHERE sid in (:sids)")
    fun getSessionsByIds(sids: MutableList<String>): MutableList<Session>

    @Delete
    fun delete(s: Session)

    @Query("DELETE FROM session WHERE sid = :sid")
    fun delete(sid: String): Int

    /*fun saveSession(sessions: MutableList<Session>) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            sessions.forEach { s ->
                s.save()
                s.ids?.forEach {
                    SessionMember(s.sid, it).save()
                }
            }
        }
    }

    fun saveSessionAndMember(sessions: Session) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            sessions.save()
            sessions.ids?.forEach {
                SessionMember(sessions.sid, it).save()
            }
        }
    }

    fun updateSessionMember(sid: String, ids: MutableList<Long>) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            SQLite.delete().from(SessionMember::class.java)
                    .where(SessionMember_Table.sid.eq(sid))
                    .executeUpdateDelete()
            ids.forEach {
                SessionMember(sid, it).save()
            }
        }
    }

    fun addMember(sid: String, ids: MutableList<Long>) {
        FlowManager.getDatabase(MsgDatabase::class.java).executeTransaction {
            ids.forEach {
                SessionMember(sid, it).save()
            }
        }
    }*/

     /*{
        return SQLite.select().from(SessionMember::class.java)
                .where(SessionMember_Table.sid.eq(sid))
                .queryList().map { it.uid }
    }*/

    @Query("SELECT * FROM Session")
    fun findAll(): MutableList<Session>

    @Query("SELECT * FROM Session where type=:type")
    fun findAllType(type:Int): MutableList<Session>

    @Query("select * from Session where title like :keyword escape '\\' limit :pageSize offset :offset")
    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Session>

    /*fun getSessionMember(sid: String): MutableList<SessionMember> {
        return SQLite.select().from(SessionMember::class.java)
                .where(SessionMember_Table.sid.eq(sid))
                .queryList()
    }

    fun getSessionAdmins(sid: String): List<Long> {
        return SQLite.select().from(SessionMember::class.java)
                .where(SessionMember_Table.sid.eq(sid))
                .and(SessionMember_Table.isAdmin.eq(true))
                .queryList().map { it.uid }
    }



    fun deleteMember(sid: String, dels: MutableList<Long>) {
        SQLite.delete().from(SessionMember::class.java)
                .where(SessionMember_Table.sid.eq(sid))
                .and(SessionMember_Table.uid.`in`(dels))
                .executeUpdateDelete()
    }*/
}

@Dao
interface SessionMemberDao {

    @Insert
    fun insert(smem: SessionMember)

    @Insert
    fun insertAll(smem: List<SessionMember>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdate(smem: List<SessionMember>)

    @Delete
    fun delete(smeme: SessionMember)

    @Delete
    fun deleteAll(smeme: List<SessionMember>)

    @Update
    fun update(smeme: SessionMember)

    @Query("UPDATE SessionMember set isAdmin =0 where sid=:sid")
    fun resetAdmins(sid: String)

    @Query("UPDATE SessionMember set isAdmin =1 where sid=:sid and uid in (:uids)")
    fun setAdmins(sid: String, uids: List<Long>)

    @Query("SELECT * FROM SessionMember WHERE sid = :sid")
    fun findAllBySid(sid: String): MutableList<SessionMember>

    @Query("DELETE FROM SessionMember WHERE sid = :sid")
    fun deleteBySid(sid: String)

//    @Query("SELECT * FROM SessionMember WHERE sid = :sid and uid in (:ids)")
//    fun findBySidAndUserIds(sid: String, ids: List<Long>): MutableList<SessionMember>

    @Query("DELETE FROM SessionMember WHERE sid = :sid and uid in (:ids)")
    fun deleteMember(sid: String, ids: List<Long>)

    @Query("SELECT uid FROM SessionMember WHERE sid = :sid and isAdmin=1")
    fun findAllAdminBySid(sid:String): List<Long>

    @Query("SELECT uid FROM SessionMember WHERE sid = :sid order by isAdmin, uid")
    fun getSessionMemberIds(sid: String): List<Long>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT u.*, sm.*, sm.uid as id FROM User u, SessionMember sm WHERE sm.sid = :sid and u.id = sm.uid and u.type!=3 order by isAdmin desc, lower(u.upinyin) asc limit :offset,:pageSize")
    fun getSessionMembers(sid: String, pageSize: Int, offset: Long): List<Member>

    @Query("DELETE FROM SessionMember")
    fun deleteAll()

}
