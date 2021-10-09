package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.Department
import com.liheit.im.core.bean.UserDepartment

/**
 * 部门表操作
 */
@Dao
interface DepartmentDao {

    @Insert
    fun insert(dep: Department)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOrUpdate(dep: Department)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveBatch(dep: List<Department>)

    @Update
    fun update(dep: Department)

    @Query("select * from Department where visible= 1 and pid = :pid order by sort asc")
    fun findByPid(pid: Long): MutableList<Department>

    @Query("select * from Department where id = :id")
    fun findById(id: Long): Department?

    @Query("select * from Department where id in (:ids)")
    fun findByIds(ids: LongArray): MutableList<Department>

    @Query("DELETE FROM Department where type=:type")
    fun deleteByType(type: Int): Int

    @Query("select d.* from Department d,UserDepartment ud where d.id =ud.depId and ud.id = :uid")
    fun getUserDepartment(uid: Long): List<Department>

    @Query("select d.* from Department d  where visible= 1 and d.cname like :keyword or d.ename like :keyword limit :pageSize offset :offset")
    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Department>

    @Query("UPDATE Department set visible = :visible where id in (:ids)")
    fun updateVisible(ids: LongArray, visible: Boolean): Long

    @Query("UPDATE Department set visible = :visible")
    fun updateAllVisible(visible: Boolean): Long

    @Query("DELETE FROM Department")
    fun deleteAll()
}

/**
 * 部门用户表操作
 */
@Dao
interface UserDepartmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dep: UserDepartment)

    @Update
    fun update(dep: UserDepartment)

    @Query("select id from UserDepartment where depId = :pid")
    fun findByPid(pid: Long): MutableList<Long>

    @Query("DELETE FROM UserDepartment where type=:type")
    fun deleteByType(type: Int): Int

    @Query("DELETE FROM UserDepartment")
    fun deleteAll(): Int

    @Query("select count(*) from UserDepartment where type != 3")
    fun count(): Int
}
