package com.liheit.im.core.protocol.session

import com.liheit.im.core.protocol.Rsp

/**
 * Created by daixun on 2018/7/13.
 */

class CreateSessionRsp(
        var sid: String = "",
        var type: Int = 0,
        var title: String = "",
        var cid: Long = 0L,
        var ctime: Long = 0,
        var utime: Long = 0,
        var admins: List<Long>? = null,
        var ids: List<Long>? = null
) : Rsp()
