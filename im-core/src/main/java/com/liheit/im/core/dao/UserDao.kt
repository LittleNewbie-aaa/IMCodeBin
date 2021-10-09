package com.liheit.im.core.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.liheit.im.core.bean.User

/**
 * 用户表操作
 */
@Dao
interface UserDao {
    @Query("select * from  user where cname like :keyword or ename like :keyword")
    fun findUserLikeName(keyword: String): MutableList<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(users: List<User>)

    @Query("select * from user where id in (:ids) and type!=3 order by lower(upinyin) asc")
    fun findByIds(ids: List<Long>): MutableList<User>

    @Query("select * from user where phone and type!=3 like :phone")
    fun findByPhone(phone: String): MutableList<User>

    @Query("SELECT * FROM user where id = :id")
    fun findById(id: Long): User?

    @Query("select * from user where visible= 1 and type !=3 and level in (:permissions) and infoSearch like :keyword limit :pageSize offset :offset")
    fun searchUser(keyword: String, pageSize: Int, offset: Long,permissions:IntArray): MutableList<User>

    @Query("select u.* from user as u ,UserDepartment ud where u.visible= 1 and u.id = ud.id and u.type != 3 and ud.depId = :depId order by u.level desc")
    fun findUserByDepartmentId(depId: Long): MutableList<User>

    @Query("select u.* from user as u ,SessionMember sm where u.id=sm.uid and sm.sid=:sid and u.type!=3 order by isAdmin desc, lower(u.upinyin) asc limit :pageSize offset :offset")
    fun findBySession(sid: String, pageSize: Int, offset: Long): List<User>

    @Query("DELETE FROM User where type=:type")
    fun deleteByType(type: Int): Int

    @Query("UPDATE User set visible=:visible where id in (select id from UserDepartment where depId in (:depIds))")
    fun updateUserVisible(depIds: LongArray, visible: Boolean): Long

    @Query("UPDATE User set visible=:visible")
    fun updateAllUserVisible(visible: Boolean): Long

    @Query("UPDATE User set logo=:logo where id=:userId")
    fun updateLogo(logo: Long, userId: Long): Long

    @Query("select * from user where upinyin=''")
    fun findUserPinyinIsNoll(): List<User>

    @Query("UPDATE User set upinyin=:pinyin where id=:id")
    fun updateUserNamePinyin(id:Long,pinyin:String): Long
}