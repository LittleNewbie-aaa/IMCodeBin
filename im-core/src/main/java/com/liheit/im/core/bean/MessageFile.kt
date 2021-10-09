package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by daixun on 2018/8/3.
 */

@Entity
data class MessageFile(
    @PrimaryKey var mid: String = "",//文件消息id
    @ColumnInfo var sid: String = "",//会话id
    @ColumnInfo var isReceive: Boolean = false,
    @ColumnInfo var type: Int = 0,//文件类型
    @ColumnInfo var token: String = "",
    @ColumnInfo var bytes: Long = 0,//文件大小
    @ColumnInfo var sizew: Int = 0,
    @ColumnInfo var sizeh: Int = 0,
    @ColumnInfo var md5: String = "",
    @ColumnInfo var localPath: String? = null,//文件本地路径
    @ColumnInfo var isupload: Int = 0,//上传标识，默认为 0，为下载
    @ColumnInfo var status: Int = 0,//传输进度[0~100]，默认为 0
    @ColumnInfo var t: Long = 0, //消息时间
    @ColumnInfo var name: String = "",//图片或者文件名
    @Transient var isChecked: Boolean = false
)

