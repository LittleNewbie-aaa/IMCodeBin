package com.liheit.im.core.protocol.contact

/**
 * 通讯录更新通知应
 * Created by daixun on 2018/7/19.
 */

class ContactNotice(
        var utime: Long = 0,//用户信息更新时间(客户端需要用它与上一次的比较，来确定是否要更新)
        var dtime: Long = 0,//部门最后更新时间(同上)
        var dutime: Long = 0//部门用户最后更新时间(同上)
)
