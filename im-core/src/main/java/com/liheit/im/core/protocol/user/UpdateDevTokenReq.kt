package com.liheit.im.core.protocol.user

import com.liheit.im.core.protocol.Rsp

/**
 * Created by daixun on 2018/11/30.
 */

data class UpdateDevTokenReq(var devtoken: String,
                             var devfactory: String)

class UpdateDevTokenRsp : Rsp()