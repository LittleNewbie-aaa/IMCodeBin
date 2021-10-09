package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 群组数据表
 */
@Entity
data class Session(
    @PrimaryKey var sid: String = "",//群组id
    @ColumnInfo @SerializedName("type") var type: Int = 0,//群组类型 详情查看SessionType类
    @ColumnInfo var title: String = "",//群名称
    @ColumnInfo var cid: Long = 0,//群组创建者id
    @ColumnInfo var ctime: Long = 0,
    @ColumnInfo var utime: Long = 0,
    @Ignore var admins: MutableList<Long>? = null,//群组管理员id集合
    @Ignore var ids: MutableList<Long>? = null,//群组成员id集合
    @ColumnInfo var Flag: Int = 0,
    @Transient var keyword: String = "",
    @ColumnInfo var notice: String = ""
)


data class SessionNotice(
        var cid: Long = 0,//会话id
        var ctime: Long = 0,//群公告设置时间
        var content: String = ""//群公告内容
)

enum class SessionType(val value: Int) {
    REMOVE(-1),              // 删除

    SESSION_P2P(0),          // 点对点会话
    FILE_HELP(1),//文件传输助手

    // 多人会话
    DISSOLVE(-2),            // 解散状态
    SESSION_FIX(30),         // 固定群会话（后台创建的？）
    SESSION_DEPT(31),        // 部门固定群（自动创建：不能修改，必须自己在部门中）
    SESSION(32),             // 普通群
    SESSION_DISC(33),        // 临时组会话（客户端创建的临时讨论组？）

    // 公告(广播)
    BULLETIN(50),            // 公告
    SCHOOL_BULLETIN(50),     // 全校公告
    CLASS_BULLETIN(51),      // 班级公告
    GRADE_BULLETIN(52),      // 年级公告
    BULLETIN_TO(53),         // 定向公告（选择公告的接收者）？

    // 通知
    SYSTEM_NOTICE(100),      // 系统通知？
    SYSTEM_NOTICEUSERS(101), // 系统通知(接收者列表)
    WEB_APP_NOTICE(110),     // 轻应用通知
    MEETING_NOTICE(111),     // 消息通知

    SYSYTEM_MOSTOFTEN(120),  //设置常用群标识 仅在聊天列表使用
    SYSYTEM_FIXSESSION(121),  //设置固定群组群标识 仅在聊天列表使用

    // 公众号
    OFFICIAL_ACCOUNTS(130)      // 公众号
}
