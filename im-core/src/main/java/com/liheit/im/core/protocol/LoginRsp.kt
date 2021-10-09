package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/18.
 */

data class LoginRsp(
        var utime: Long = 0,//用户信息更新时间(客户端需要用它与上一次的比较，来确定是否要更新)
        var dtime: Long = 0,//部门最后更新时间(同上)
        var dutime: Long = 0,//部门用户最后更新时间(同上)
        val powers: Powers? =null
) : Rsp()

data class Powers(
        val info: List<Int>? = null, //查看用户详情权限(info):如果有指定时，只能查看指定level用户的详情，否则无限制
        val phone: List<Int>? = null,//查看用户电话权限(phone):如果有指定时，只能查看指定level用户电话信息，否则无限制
        val session: List<Int>? = null,//发起单聊会话权限(session):如果有指定时，只能与指定level用户发起单聊，否则无限制
        val vosip: List<Int>? = null//发起视频会议权限(vosip):如果有指定时，只能与指定level用户发起视频会议，否则无限制
)