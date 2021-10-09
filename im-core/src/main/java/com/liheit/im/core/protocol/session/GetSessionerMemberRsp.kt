package com.liheit.im.core.protocol.session

import com.liheit.im.core.protocol.Rsp

/**
 * Created by daixun on 2018/7/13.
 */

class GetSessionerMemberRsp(
        var sid: String = "",
        var ids: MutableList<Long>? = null
) : Rsp()
