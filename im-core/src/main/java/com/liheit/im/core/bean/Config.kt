package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

/**
 * Created by daixun on 2018/6/22.
 */

@Entity
data class Config(
    @PrimaryKey var key: String = "",
    @ColumnInfo var account: String = "",
    @ColumnInfo var value: String = ""
){
    @Ignore
    constructor() : this("")
}
