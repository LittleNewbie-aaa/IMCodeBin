package com.im.bizvideolib

import com.liheit.im.core.IMClient
import com.liheit.im.core.IMClient.getCurrentUserId
import com.liheit.im.utils.Log
import meeting.confcloud.cn.bizaudiosdk.ActionCode
import meeting.confcloud.cn.bizaudiosdk.ActionListener
import meeting.confcloud.cn.bizaudiosdk.BizMeetingFinishedListener

class BizVideoListener : ActionListener, BizMeetingFinishedListener {

    //视频会议认证初始化回调
    override fun onAction(i: Int, i1: Long) {
        when (i) {
            ActionCode.SDK_UNINITIALIZED ->
                Log.e("sdk未初始化，错误代码：$i$i1")
//                Toast.makeText(this, "sdk未初始化，错误代码：$i$i1", Toast.LENGTH_SHORT).show()
            ActionCode.NETWORK_NOT_CONNECTED, ActionCode.SERVER_NO_PARAMETER,
            ActionCode.SERVER_PARAMETER_ERROR, ActionCode.SERVER_STATUS_CODE_ERROR,
            ActionCode.BIZCONF_ERROR_INVALID_ARGUMENTS, ActionCode.BIZCONF_ERROR_ILLEGAL_APP_KEY_OR_SECRET,
            ActionCode.BIZCONF_ERROR_NETWORK_UNAVAILABLE, ActionCode.BIZCONF_ERROR_DEVICE_NOT_SUPPORTED,
            ActionCode.BIZCONF_ERROR_UNKNOWN, ActionCode.HTTP_STATUS_CODE,
            ActionCode.SDK_ERROR_CODE ->
                Log.e("认证失败，错误代码:$i$i1")
//                Toast.makeText(this, "认证失败，错误代码:$i$i1", Toast.LENGTH_SHORT).show()
            ActionCode.SDK_INIT_SUCCESS -> {
                Log.e("初始化成功:$i$i1")
//                Toast.makeText(this, "初始化成功:$i$i1", Toast.LENGTH_SHORT).show()
//                Log.e("aaa IMClient.getCurrentUserAccount()=${IMClient.getCurrentUserAccount()}")
                if (IMClient.getCurrentUserAccount() != "" && IMClient.getCurrentUserAccount() != null) {
                    BizVideoClient.initialize()
                }
            }
        }
        BizVideoClient.actionCode = i
    }

    //会议开始监听
    override fun inMeetingStatus() {

    }

    //会议结束监听
    override fun onMeetingFinished() {
        BizVideoClient.sid?.let { sid ->
            BizVideoClient.sendJoinLeaveMeetingRoom(sid, 0)
        }
        if (BizVideoClient.isHost && BizVideoClient.confCreaterid == getCurrentUserId().toString()) {
            BizVideoClient.confId?.let { BizVideoClient.releaseConf(it) }
            BizVideoClient.isHost = false
        }
        BizVideoClient.confId = null
        BizVideoClient.confCreaterid = ""
    }

}