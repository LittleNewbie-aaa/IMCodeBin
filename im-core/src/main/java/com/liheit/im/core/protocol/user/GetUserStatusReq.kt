package com.liheit.im.core.protocol.user

import com.liheit.im.core.protocol.LogoutReq
import com.liheit.im.core.protocol.Rsp

/**
 * Created by daixun on 2018/8/26.
 */

class GetUserStatusReq(
        var t: Long = 0,//客户端最后更新用户状态的时间戳,1:为部门ID，其它为 UTC
        var ids: LongArray? = null,//待获用户状态的 ID，不为空时，就使用 ids 来获取;否则就是使用时间
        var sid: String? = null //不为空时，使用会话 ID 拉取成员状态，
)

class GetUserStatusRsp(
        var t: Long = 0,//返回请求中的值
        var all: Boolean = false,//false:为增量更新;true:为全量更新(全量更新无 PC 和手机都离 线人员)PC.Term|PC.Stat|MB.Term|MB.Stat
        var status: Map<Long, Long>? = null//员工的状态 map，格式: "id":status; 状态格式如下:  移动状态:bit[0,3]移动终端:bit[4,7] PC状态:bit[8,11] PC终端:bit[12,15]
) {
    companion object {
        var MOBILE_STATUS_MASK = 0b1111L
        var MOBILE_TERMINAL_MASK = 0b11110000L
        var PC_STATUS_MASK = 0b111100000000L
        var PC_TERMINAL_MASK = 0b1111000000000000L

        fun getStatus(status: Long?): String {
            if (status == null) {
                return "离线"
            }
            var pcStatus = status.and(GetUserStatusRsp.PC_STATUS_MASK).shr(8).toInt()
            if (pcStatus == LogoutReq.ONLINE || pcStatus == LogoutReq.LEAVE) {
                return "PC在线"
            }
            var mobileStatus = status.and(GetUserStatusRsp.MOBILE_STATUS_MASK).toInt()
            if (mobileStatus == LogoutReq.ONLINE || mobileStatus == LogoutReq.LEAVE) {
                return "手机在线"
            }
            return "离线"
        }

        // 0:离线  1:手机在线   2:PC在线
        fun getIMStatus(status: Long?): Int {
            if (status == null) {
                return 0
            }
            var pcStatus = status.and(GetUserStatusRsp.PC_STATUS_MASK).shr(8).toInt()
            if (pcStatus == LogoutReq.ONLINE || pcStatus == LogoutReq.LEAVE) {
                return 2
            }
            var mobileStatus = status.and(GetUserStatusRsp.MOBILE_STATUS_MASK).toInt()
            if (mobileStatus == LogoutReq.ONLINE || mobileStatus == LogoutReq.LEAVE) {
                return 1
            }
            return 0
        }
    }
}
