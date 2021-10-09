package com.liheit.im.core.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.liheit.im.core.bean.Config

/**
 * Created by daixun on 2018/10/27.
 */

@Dao
interface ConfigDao {
    @Query("select * from config where key = :key and account = :account")
    fun findByKeyAndAccount(key: String, account: String): Config

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(config: Config)
}
