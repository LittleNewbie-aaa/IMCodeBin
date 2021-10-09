package com.liheit.im.core.protocol.session

import com.liheit.im.utils.TimeUtils

/**
 * Created by daixun on 2018/7/11.
 */

class ModifySessionReq(
        var flag: Long = 0, //更新标识，参考ModifySession*
        var uid: Long = 0,//修改者ID
        var cid: Long?=null,//群组创建者id
        var sid: String = "",//群组id
        var type: Int?=null,//群组类型
        var title: String?=null,//群组标题
        var notice:String?="",//群公告信息gson数据
        var utime:Long=TimeUtils.getServerTime(),
        var admins: MutableList<Long>? = null,//管理员数组（如果有修改，必须带上所有的管理员；讨论组无该功能）
        var adds: MutableList<Long>? = null,//添加的成员（必须是不存在的成员）
        var dels: MutableList<Long>? = null //删除的成员（必须是已经存在的成员）
){
    companion object {
        const val ModifySessionType		    =   0x00000001L	// 修改会话类型
        const val ModifySessionTitle	    =	0x00000002L	// 修改会话标题(只有群主，管理员可以修改)
        const val ModifySessionCreaterID    =	0x00000004L	// 修改群创建者(群转让，接收者必须是群成员)；讨论组无转让功能
        const val ModifySessionAddAdmins	=	0x00000008L	// 修改群管理员(只有群主能修改)；讨论组无管理员
        const val ModifySessionAdd		    =   0x00000010L	// 添加群成员(只有群主、管理员可以添加)
        const val ModifySessionDel		    =   0x00000020L	// 删除群成员(不能删除自己；群主不能退出；只有群主、管理员才能删除成员)
        const val ModifySessionExit		    =   0x00000040L	// 是自己退群（群主不能群群，只能解散群）
        const val ModifySessionDelAdmins    =   0x00000080L // 删除的管理员；讨论组无管理员
        const val ModifySessionRemove	    =   0x80000000L	// 删除群(解散群,只有群主才能解散群；值同db.DeleteFlag)
        const val ModifySessionTextNotice   =   0x00000100L // 修改群公告
    }
}
