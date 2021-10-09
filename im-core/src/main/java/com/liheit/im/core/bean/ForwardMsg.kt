package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

/**
 * Created by daixun on 2018/8/14.
 */

@Entity
class ForwardMsg(
    @PrimaryKey var token: String = "",
    @ColumnInfo var content: String = ""
){
    @Ignore
    constructor() : this("")
}

