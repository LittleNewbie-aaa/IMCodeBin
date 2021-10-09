package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore

/**
 * 群组人员表
 */
@Entity(primaryKeys = ["sid", "uid"])
data class SessionMember(
        @ColumnInfo var sid: String = "",//群组id
        @ColumnInfo var uid: Long = 0,//人员id
        @ColumnInfo var isAdmin: Boolean = false//是否是当前群组的管理员
){
        @Ignore
        constructor() : this("")
}

data class Member(
        var sid: String,
        var isAdmin: Boolean = false,
        @Embedded
        var user: User
)