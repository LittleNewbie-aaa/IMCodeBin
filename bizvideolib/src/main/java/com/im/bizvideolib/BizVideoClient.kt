package com.im.bizvideolib

import android.content.Context
import com.dagger.baselib.utils.ToastUtils
import com.liheit.im.common.ext.subscribeEx
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMClient.context
import com.liheit.im.core.MessageBuilder
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.SessionType
import com.liheit.im.core.bean.User
import com.liheit.im.core.protocol.VideoConferenceBody
import com.liheit.im.core.service.MessageService
import com.liheit.im.utils.*
import com.liheit.im.utils.json.gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import meeting.confcloud.cn.bizaudiosdk.AudioServers
import meeting.confcloud.cn.bizaudiosdk.BizVideoService
import meeting.confcloud.cn.bizaudiosdk.bizconflistener.BizInviteOptions
import us.zoom.sdk.ZoomSDK
import java.lang.Exception

/**
 * 视频会议管理器
 */
object BizVideoClient {

    //视频会议相关参数
    val channelId = BuildConfig.channelId
    val autoKey = BuildConfig.autoKey
    val APIKey = BuildConfig.APIKey
    val siteSign = BuildConfig.siteSign

    var bizVideoService: BizVideoService? = null
    var confId: String? = null
    var confCreaterid: String = ""
    var isHost = false
    var chatMessage: ChatMessage? = null

    var actionCode: Int? = -1

    fun init(context: Context) {
        bizVideoService = BizVideoService.getInstance(context)
        // 全局监听注册。注册了这个监听，才会走其他的监听
        var listener = BizVideoListener()
        bizVideoService?.addActionListener(listener)
        bizVideoService?.addMeetingFinishedListener(listener)
        //SDK认证
        bizVideoService?.authSdk(channelId, autoKey)
//        显示邀请弹框中的所有分享图标
//        720p开关
//        ZoomSDK.getInstance().getMeetingSettingsHelper().enable720p(true);
//        // 自定义结束，离开会议按钮
//        BizVideoService.getInstance(getActivity()).addBizConfEndButtonClickListener(this, true);
//        // 驾驶者模式
//        AudioServers.getInstance(getActivity()).setDisableDriverMode(true);
//        // 显示邀请弹框中的所有分享图标
//        bizVideoService.setInviteOptions_DATA(BizInviteOptions.INVITE_ENABLE_ALL);
//        // 只显示邮件邀请
//        bizVideoService.setInviteOptions_DATA(BizInviteOptions.INVITE_VIA_EMAIL);

        // 显示邀请弹框中的分享图标
        bizVideoService?.setInviteOptions_DATA(BizInviteOptions.INVITE_ENABLE_ALL)
    }

    fun isAutoSuccess():Boolean {
        return try {
            bizVideoService?.isAutoSuccess!!
        }catch (e:Exception){
            false
        }
    }

    /**
     * 主持人开启会议
     * userId: 入会人员id
     * Username:用户名称
     * meetingNo:会议号码
     * uId:参会人的身份标识(非必填)
     * cuid: cuid(非必填项目)
     * confId: 会议id
     * confCreaterId: 会议创建者id
     */

    fun startMeeting(userId: String, userName: String, token: String,
                     meetingNo: String, cuid: String, zak: String,
                     confId: String, confCreaterId: String): Int? {
        if (bizVideoService?.isAutoSuccess!!) {
            this.confId = confId
            this.confCreaterid = confCreaterId
            bizVideoService?.startMeeting(userId, userName, token, meetingNo, cuid, zak)
            isHost = true
        }
        return actionCode
    }

    /**
     * 主持人协议链接启会
     * protocolUrl：入会链接
     * confId: 会议id
     * confCreaterId: 会议创建者id
     */
    fun startMeetingUrl(context: Context, protocolUrl: String,
                        confId: String, confCreaterId: String): Int? {
        if (bizVideoService?.isAutoSuccess!!) {
            this.confId = confId
            this.confCreaterid = confCreaterId
            bizVideoService?.startMeetingUrl(context, protocolUrl)
            isHost = true
        }
        return actionCode
    }

    /**
     * 加入有密码会议
     * Username:用户名称
     * meetingNo:会议号码
     * password:会议密码
     * userId:参会人的身份标识(非必填)
     * cuid: cuid(非必填项目)
     * confId: 会议id
     * confCreaterId: 会议创建者id
     */
    fun joinMeetingPwd(userName: String, meetingNo: String, password: String,
                       userId: String, cuid: String, confId: String,
                       confCreaterId: String): Int? {
        if (bizVideoService?.isAutoSuccess!!) {
            this.confId = confId
            this.confCreaterid = confCreaterId
            bizVideoService?.joinMeeting(userName, meetingNo, password, userId, cuid)
        }
        return actionCode
    }

    /**
     * 加入无密码会议
     * Username:用户名称
     * meetingNo:会议号码
     * uId:参会人的身份标识(非必填)
     * cuid: cuid(非必填项目)
     * confId: 会议id
     * confCreaterId: 会议创建者id
     */
    fun joinMeetingNoPwd(userName: String, meetingNo: String,
                         userId: String, cuid: String, confId: String,
                         confCreaterId: String): Int? {
        if (bizVideoService?.isAutoSuccess!!) {
            this.confId = confId
            this.confCreaterid = confCreaterId
            bizVideoService?.joinMeeting(userName, meetingNo, userId, cuid)
        }
        return actionCode
    }

    /**
     * 协议链接加入会议
     * protocolUrl：入会协议链接
     * userName：入会人员名字
     * userId：入会人员id
     * confId: 会议id
     * confCreaterId: 会议创建者id
     */
    fun joinMeetingUrl(context: Context, protocolUrl: String,
                       userName: String, userId: String,
                       confId: String, confCreaterId: String): Int? {
        if (bizVideoService?.isAutoSuccess!!) {
            this.confId = confId
            this.confCreaterid = confCreaterId
            bizVideoService?.joinMeetingUrl(context, "${protocolUrl}&uname=${userName}&uid=${userId}")
        }
        return actionCode
//        Log.e("aaa${protocolUrl}&uname=${userName}&uid=${userId}")
    }

    //进入会议是否自动打开摄像头（false为打开摄像头  true为关闭摄像头）
    fun setMeetingSettingCloseCamera(flag: Boolean) {
        bizVideoService?.isMeetingSettingCloseCamera = flag
        SharedPreferencesDataTypeUtils.saveBoolean(
                context, "${IMClient.getCurrentUserAccount()}CloseCamera", flag)
//        Log.e("aaa CloseCamera=${getMeetingSettingCloseCamera(context)}")
    }

    //获取进入会议是否自动打开摄像头
    fun getMeetingSettingCloseCamera(context: Context): Boolean {
        return bizVideoService!!.isMeetingSettingCloseCamera
    }

    // 自动连接语音vopip（false为关闭连接  true为打开连接）
    fun setAutoConnectVoIP(context: Context, flag: Boolean) {
        AudioServers.getInstance(context).setAutoConnectVoIP(flag)
        SharedPreferencesDataTypeUtils.saveBoolean(
                IMClient.context, "${IMClient.getCurrentUserAccount()}VoIP", flag)
//        Log.e("aaa VoIP=${getAutoConnectVoIP()}")
    }

    //获取进入会议是否自动连接语音vopip
    fun getAutoConnectVoIP(): Boolean {
        return ZoomSDK.getInstance().meetingSettingsHelper.isAutoConnectVoIPWhenJoinMeetingEnabled
    }

    //进入会议是否打开麦克风（false为打开麦克风  true为关闭麦克风）
    fun setMuteMyMicrophoneWhenJoinMeeting(context: Context, flag: Boolean) {
        AudioServers.getInstance(context).isMuteMyMicrophoneWhenJoinMeeting = flag
        SharedPreferencesDataTypeUtils.saveBoolean(
                IMClient.context, "${IMClient.getCurrentUserAccount()}CloseMicrophone", flag)
//        Log.e("aaa CloseMicrophone=${getMuteMyMicrophoneWhenJoinMeeting(IMClient.context)}")
    }

    //获取进入会议是否打开麦克风设置
    fun getMuteMyMicrophoneWhenJoinMeeting(context: Context): Boolean {
        return AudioServers.getInstance(context).isMuteMyMicrophoneWhenJoinMeeting
    }

    //发送入会邀请消息
    fun sendVideoMeetingMsg(confId: String, confCreaterId: String, userlist: List<User>) {
        BizConfVideoClient.getConfDetailByConfId(confId, confCreaterId)
                .map {
                    android.util.Log.e("aaa", gson.toJson(it))
                    it.data as ConfReservationItem
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeEx({ data ->
                    userlist?.forEach { user ->
                        val toIDs = mutableListOf<Long>()
                        toIDs.add(user.id)
                        val convId = IDUtil.createSingleChatId(IMClient.getCurrentUserId(), user.id)
                        val type = SessionType.SESSION_P2P.value
                        val voiceChatMsg = MessageBuilder.createVideoConference(
                                convId, type, confCreaterId, data.protocolJoinUrl,
                                data.protocolHostStartUrl, data.hostStartUrl, data.joinUrl,
                                data.confParties, data.duration, data.confNumber,
                                data!!.confId.toString(), data!!.confName.toString(),
                                toIDs, DateUtil.getBizVideoStartTime(data.startTime, null))
                        Log.e("aaa${gson.toJson(voiceChatMsg)}")
                        IMClient.chatManager.sendMessage(voiceChatMsg)
                    }
                    ToastUtils.showToast("发送成功")
                }, {})
    }

    var sid: String? = null//加入会议时的回话id

    //发送加入或退出会议消息
    fun sendJoinLeaveMeetingRoom(sid: String, status: Int) {
        IMClient.chatManager.joinLeaveMeetingRoomCall(sid, status)
                .toObservable()
                .subscribeEx {}
        if (status == 1) {
            this.sid = sid
        } else {
            this.sid = null
        }
    }

    //主持人结束会议时释放资源
    fun releaseConf(confId: String) {
        BizConfVideoClient.cancelConf(confId, IMClient.getCurrentUserId().toString())
                .subscribeEx({
                    Log.e("aaa releaseConf ${gson.toJson(it)}")
                    if (it.status == 100) {
                        chatMessage?.let { msg ->
                            Log.e("aaa ${gson.toJson(chatMessage)}")
                            (msg.getMessageBody() as VideoConferenceBody).confstarttime = 1577808000000
                            MessageService.update(msg)
                            IMClient.chatManager.onMessageChanged(mutableListOf(msg))
                            MessageService.setMsgRead(msg.sid, TimeUtils.getServerTime())
                            this.chatMessage = null
                        }
                    } else {
//                        ToastUtils.showToast("释放资源失败")
                    }
                }, { })
    }

    fun initialize() {
        var closeCamera = SharedPreferencesDataTypeUtils.getBoolean(
                context, "${IMClient.getCurrentUserAccount()}CloseCamera", true)  //是否自动打开摄像头
//        Log.e("aaa closeCamera=${closeCamera}")
        setMeetingSettingCloseCamera(closeCamera)

        var voIP = SharedPreferencesDataTypeUtils.getBoolean(
                context, "${IMClient.getCurrentUserAccount()}VoIP", true) //是否自动连接语音vopip
//        Log.e("aaa voIP=$voIP")
        setAutoConnectVoIP(context, voIP)

        var closeMicrophone = SharedPreferencesDataTypeUtils.getBoolean(
                context, "${IMClient.getCurrentUserAccount()}CloseMicrophone", true)  //是否自动是否打开麦克风设置
//        Log.e("aaa closeMicrophone=$closeMicrophone")
        setMuteMyMicrophoneWhenJoinMeeting(context, closeMicrophone)
    }
}