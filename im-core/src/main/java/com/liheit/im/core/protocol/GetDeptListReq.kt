package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/15.
 */

data class GetDeptListReq(
    var t: Long = 0
)


data class GetDeptShowReq(val t: Long)

data class GetNewDeptShowReq(val userId: Long)

data class GetDeptShowRsp(
    val t: Long = 0,
    val default: Int = 0,//默认值，客户端接收到响应时: 当为ShowNoUpdate，不更新 否则，首先需要将全部更新成默认值
    val shows: MutableMap<Long, Int>? = null) {
    enum class UpdateFlag(val value: Int) {
        NO_UPDATE(-1),
        HIDE(0),
        SHOW_DEPT(1),
        SHOW_DEPT_USER(2)
    }
}
/*
部门显示状态 map，格式: "id":status 状态格式如下:
 无更新:ShowNoUpdate(-1)
 隐藏部门:ShowHide(0)
 显示部门:ShowDept(1)
 显示部门及员工:ShowDeptUser(2)*/
