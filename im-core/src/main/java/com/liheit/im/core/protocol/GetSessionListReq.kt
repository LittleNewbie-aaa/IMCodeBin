package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/23.
 */

data class GetSessionListReq(
        var type: Long? = null,//群类型：0-所有群；SessionFix-固定群；SessionDept-部门固定群; SessionGroup-群; SessionDisc-讨论组；或者是更新时间戳 UTC
        var sids: MutableList<String>? = null
) {
    companion object {
        const val TYPE_ALL = 0
    }
}
