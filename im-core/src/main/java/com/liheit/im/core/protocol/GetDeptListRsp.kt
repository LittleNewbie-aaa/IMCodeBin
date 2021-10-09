package com.liheit.im.core.protocol

import com.liheit.im.core.bean.Department

/**
 * 服务器返回需要更新的部门数据
 */
data class GetDeptListRsp(
        var t: Long = 0,//最后更新时间
        var type: Int = 0,//更新类型
        var depts: MutableList<Department>? = null,//部门信息
        var packageType: Int, //0为json返回 1为文件返回
        var fileUrl: String="" //文件数据地址
) : Rsp()

/**
 * 更新类型
 */
enum class UpdateType(val type: Int) {
        FULL(100),//全量更新
        INC(101),
        UPDATE_ID(102)
}