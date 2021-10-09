package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/14.
 */
data class LoginReq(
        var account: String,//当前用户账号
        var token: String,//当前用户token
        var mac: String,
        var devtoken: String//离线推送设备token
) : Req()



