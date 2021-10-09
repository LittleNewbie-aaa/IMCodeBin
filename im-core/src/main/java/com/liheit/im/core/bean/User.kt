package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.github.promeg.pinyinhelper.Pinyin
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * 用户数据表
 */
@Parcelize
@Entity()
data class User(
    @PrimaryKey var id: Long = 0,
    @ColumnInfo var account: String = "",
    @ColumnInfo var email: String = "",
    @ColumnInfo var phone: String = "",
    @ColumnInfo var tel: String = "",
    @ColumnInfo var sign: String = "",
    @ColumnInfo var cname: String = "",
    @ColumnInfo var ename: String = "",
    @ColumnInfo var job: String = "",
    @ColumnInfo var infoSearch: String = "",
    @ColumnInfo @SerializedName("sex") var isMale: Boolean = false,
    @ColumnInfo var bday: Int = 0,
    @ColumnInfo var type: Int = 0,
    @ColumnInfo var t: Long = 0,
    @ColumnInfo var level: Int = 0,
    @ColumnInfo var address: String = "",
    @ColumnInfo var logo: Long = 0,   //用户logn的最后更新时间，如果为0则表示没有logo
    @ColumnInfo var visible: Boolean = true,
    @ColumnInfo var upinyin: String = "",
    @ColumnInfo var rank: Int = 0, //用于特殊情况下的用户排序
    @ColumnInfo var json_extend: String = "",
    @Transient var selected: Boolean = false,
    @Transient var disabled: Boolean = false,
    @Transient var myself: Boolean = false,
    @Transient var status: Long? = null,
    @Transient var keyword: String = "",
    @Transient var voiceChatStates: Int = 0,//用户语音聊天状态
    @Transient var volumes: Int = 0,//用户语音聊天音量
    @Transient var isMute: Boolean = false//用户语音聊天是否静音
) : Parcelable {

    val name: String
        get() {
            return cname
        }

    fun buildSearchInfo() {
        var info = StringBuilder()
        info.append("|")
        info.append(account)
        info.append("|,")

        if (!cname.isNullOrBlank()) {
            info.append("|")
            info.append(cname)
            info.append("|,")
        }

        if (!phone.isNullOrBlank()) {
            info.append("|")
            info.append(phone)
            info.append("|,")
        }

        if (!tel.isNullOrBlank()) {
            info.append("|")
            info.append(tel)
            info.append("|,")
        }

        //缩写拼音
        if (!cname.isNullOrBlank()) {
            info.append("|")
            val pinyin = Pinyin.toPinyin(cname, ",|")
            pinyin.split(",|").forEach {
                if (it.length > 0) {
                    info.append(it.subSequence(0, 1))
                }
            }
            info.append("|,")
        }
        if (!ename.isNullOrBlank()) {
            info.append("|")
            info.append(Pinyin.toPinyin(ename, ""))
            info.append("|,")
        }
        infoSearch = info.toString()
    }
}

@Entity(primaryKeys = ["id", "depId"])
data class UserDepartment(
    var id: Long = 0L,
    @ColumnInfo var t: Long = 0,
    @ColumnInfo var type: Int = 0,
    @ColumnInfo var sort: Int = 0,
    var depId: Long = 0L
){
    @Ignore
    constructor() : this(0)
}
