package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by daixun on 2018/11/9.
 */
@Entity
data class UserMetaData(
        @PrimaryKey var key: String,
        @ColumnInfo var value: String? = null
)
