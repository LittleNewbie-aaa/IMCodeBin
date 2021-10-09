package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/19.
 */

data class GetUserInfoReq(
        var t: Long = 0, //获取详情类型: 0-All Users;  1-UserIDs;  2-DeptIDs; others as UTC time
        var ids: LongArray? = null //待获取详情的 ID(t 为 1、2 时才有用)
)
