package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore

/**
 * Created by daixun on 2018/7/13.
 */
@Entity(primaryKeys = ["mId", "uid"])
data class ReceiptStatus(
    var mId: String = "",
    var uid: Long = 0,
    @ColumnInfo var isReceipted: Boolean = false
){
    @Ignore
    constructor() : this("")
}
