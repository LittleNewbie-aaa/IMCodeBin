package com.liheit.im.core.protocol

import com.liheit.im.core.bean.User

/**
 * Created by daixun on 2018/6/19.
 */

data class GetUserInfoRsp(
        var t: Long = 0, //返回请求中的值
        var type: Long, //
        var users: MutableList<User>?=null,
        var packageType: Int, //0为json返回 1为文件返回
        var fileUrl: String="" //文件数据地址
) : Rsp()
