package com.liheit.im.core.protocol.session

/**
 * Created by daixun on 2018/7/11.
 */

class ModifySessionNotice(
        var uid: Long = 0,//修改者ID
        var flag: Long = 0, //更新标识，参考ModifySession*
        var sid:String="",
        var type:Int=0,
        var title:String,
        var cid:Long=0,
        var notice:String,
        var utime:Long,
        var admins:MutableList<Long>?=null,//管理员数组（如果有修改，必须带上所有的管理员；讨论组无该功能）
        var adds:MutableList<Long>?=null,//添加的成员（必须是不存在的成员）
        var dels:MutableList<Long>?=null //删除的成员（必须是已经存在的成员）
)
