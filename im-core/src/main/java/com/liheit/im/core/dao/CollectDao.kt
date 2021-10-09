package com.liheit.im.core.dao

import android.arch.persistence.room.*
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.CollectMsg

@Dao
abstract class CollectDao {
    @Insert
    abstract fun insert(msg: CollectMsg)

    @Update
    abstract fun update(msg: CollectMsg)

    @Delete
    abstract fun delete(msg: CollectMsg)

    @Query("delete from collect_msg where id = :id")
    abstract fun deleteById(id: Long)

    @Query("delete From collect_msg")
    abstract fun deleteAll()

    @Query("select * from collect_msg where createTime < :t order by createTime desc limit :pageSize")
    abstract fun findCollectDataPage(t: Long, pageSize: Int): MutableList<CollectMsg>

    @Query("select * from collect_msg  where id = :id")
    abstract fun getCollectDataById(id: Long): CollectMsg?

    @Query("select * from collect_msg where createTime < :t and type==:type order by createTime desc limit :pageSize")
    abstract fun findCollectTypeDataPage(t: Long, type: Int, pageSize: Int): MutableList<CollectMsg>

    @Query("select * from collect_msg where createTime < :t and content||tag like :keyword escape '\\' order by createTime desc limit :pageSize")
    abstract fun searchCollectDataPage(t: Long, keyword: String, pageSize: Int): MutableList<CollectMsg>
}