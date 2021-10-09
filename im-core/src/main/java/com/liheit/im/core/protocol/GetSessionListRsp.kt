package com.liheit.im.core.protocol

import com.liheit.im.core.bean.Session

/**
 * Created by daixun on 2018/6/23.
 */

data class GetSessionListRsp(
        val utime:Long=0,
        var sessions: MutableList<Session>? = null
) : Rsp()




