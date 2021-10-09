package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.Subscription

/**
 * 公众号表操作
 */
@Dao
interface SubscriptionDao {
    @Insert
    fun insert(s: Subscription)

    @Update
    fun update(s: Subscription)

    @Delete
    fun delete(s: Subscription)

    @Query("select * from Subscription  where id = :id")
    fun getSubscriptionById(id: Long): Subscription?

    @Query("select * from Subscription  where sid = :sid")
    fun getSubscriptionBySid(sid: String): Subscription?

    @Query("select * from Subscription where createTime < :t order by createTime desc limit :pageSize")
    fun findSubscriptionDataPage(t: Long, pageSize: Int): MutableList<Subscription>

    @Query("select * from Subscription where createTime < :t and name like :keyword escape '\\' order by createTime desc limit :pageSize")
    fun searchSubscriptionDataPage(t: Long, keyword: String, pageSize: Int): MutableList<Subscription>

    @Query("delete from Subscription where sid = :sid")
    fun deleteBySid(sid: String)

    @Query("DELETE FROM Subscription")
    fun deleteAll()
}