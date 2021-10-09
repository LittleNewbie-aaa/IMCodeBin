package com.liheit.im.core.protocol

import com.liheit.im.core.protocol.annotation.LoginStatus


/**
 * Created by daixun on 2018/6/21.
 */

data class LogoutRsp(@LoginStatus var status: Int) : Rsp()



