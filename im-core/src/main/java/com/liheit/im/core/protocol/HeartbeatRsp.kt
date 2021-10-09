package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/23.
 */

data class HeartbeatRsp(
        var now: Long,//服务器当前时间，用于校验客户端是当前时间
        var t: Long //原样返回客户上传的时间
):Rsp()
