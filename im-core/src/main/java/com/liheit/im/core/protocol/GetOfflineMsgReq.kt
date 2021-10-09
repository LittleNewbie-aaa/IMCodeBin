package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/21.
 * 获取离线消息请求
 */

data class GetOfflineMsgReq(
        var t: Long = 0//获取大于指定时间所产生的离线消息(后台限制只能获取 1 个月之内的)
)
