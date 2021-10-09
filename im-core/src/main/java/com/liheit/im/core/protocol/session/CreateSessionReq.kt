package com.liheit.im.core.protocol.session

import com.liheit.im.core.bean.Session

/**
 * Created by daixun on 2018/6/29.
 */

open class CreateSessionReq(
        var sid: String = "",// string n 会话 ID，GUID
        var type: Int = 0,// int n 会话类型
        var title: String = "",// string n 会话标题
        var cid: Long = 0,// int n 创建者 ID
        var ctime: Long = 0,// int n 创建时间
        var utime: Long = 0,// n 更新时间
        var admins: MutableList<Long>? = null,// []int y 管理员数组（创建者默认就是超级权限，不需要设置为管理员）
        var ids: MutableList<Long>? = null// []int n 会话成员 ID 列表(必须大于等于 3，必须包括创建者)
) {


    fun toSession(): Session {
        return Session(sid = sid, type = type, title = title, cid = cid, ctime = ctime, utime = utime, admins = admins, ids = ids)
    }
}