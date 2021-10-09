package com.liheit.im.core.protocol

import com.liheit.im.core.bean.Department

/**
 * Created by daixun on 2018/6/19.
 */

data class GetDeptUserListRsp(
        var t: Long = 0,
        var type: Int = 0,
        var dusers: MutableList<Department>? = null,
        var packageType: Int, //0为json返回 1为文件返回
        var fileUrl: String="" //文件数据地址
) : Rsp()
