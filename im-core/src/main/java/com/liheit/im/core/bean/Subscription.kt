package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 公众号信息表
 */
@Parcelize
@Entity
data class Subscription(
        @PrimaryKey var id: Long,//公众号id
        @ColumnInfo var createTime: Long = 0,//公众号创建时间
        @ColumnInfo var updateTime: Long = 0,//公众号修改时间
        @ColumnInfo var sid: String,//公众号sid
        @ColumnInfo var logo: String? = "",//公众号头像
        @ColumnInfo var name: String? = "",//公众号名字
        @ColumnInfo var status: Long//公众号状态（ 1:启用 -1:禁用）
) : Parcelable {}