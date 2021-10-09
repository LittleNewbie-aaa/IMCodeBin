package com.im.trtc_call.group

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.blankj.utilcode.util.ToastUtils
import com.dagger.baselib.base.MainBaseActivity
import com.dagger.baselib.utils.ActivityUtils
import com.gyf.barlibrary.ImmersionBar
import com.im.trtc_call.*
import com.im.trtc_call.videolayout.TRTCVideoLayout
import com.im.trtc_call.videolayout.TRTCVideoLayoutManager
import com.liheit.im.common.ext.*
import com.liheit.im.core.IMClient
import com.liheit.im.core.MessageBuilder
import com.liheit.im.core.bean.SessionType
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tencent.trtc.TRTCCloudDef
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_trtc_group_chat.*
import kotlinx.android.synthetic.main.activity_trtc_group_chat.btn_close
import kotlinx.android.synthetic.main.activity_trtc_group_chat.btn_consent
import kotlinx.android.synthetic.main.activity_trtc_group_chat.btn_handsfree
import kotlinx.android.synthetic.main.activity_trtc_group_chat.btn_mic
import kotlinx.android.synthetic.main.activity_trtc_group_chat.btn_refuse
import kotlinx.android.synthetic.main.activity_trtc_group_chat.clCallIng
import kotlinx.android.synthetic.main.activity_trtc_group_chat.clChatIng
import kotlinx.android.synthetic.main.activity_trtc_group_chat.img_back
import kotlinx.android.synthetic.main.activity_trtc_group_chat.tv_time
import kotlinx.android.synthetic.main.activity_trtc_solo_chat.*
import java.util.*

/**
 * 多人通话
 */
@SuppressLint("NewApi")
class TRTCGroupChatActivity : MainBaseActivity() {
    lateinit var vm: TRTCChatVM
    private var showFloatingWindow = true
    private lateinit var trtcLayoutManager: TRTCVideoLayoutManager

    //  设置顶部状态栏颜色
    private fun setTopColor() {
        ImmersionBar.with(this)
            .transparentStatusBar()
            .statusBarColor(R.color.activity_bar_color) //底部导航栏颜色，不写默认黑色
            .init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 应用运行时，保持不锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTopColor()
        setContentView(R.layout.activity_trtc_group_chat)
        vm = getViewModel(TRTCChatVM::class.java, mModelFactory)

        trtcLayoutManager = findViewById(R.id.trtcLayoutManager)

        trtcLayoutManager.setMode(TRTCVideoLayoutManager.MODE_GRID)

        initListener()

        TRRCUtils.getInstanse().rListener = TRTCReturn

        getChatData()

        RxPermissions(this@TRTCGroupChatActivity)
            .request(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .subscribeEx({ it ->
                if (it) {
                    initTRRC()
                } else {
                    com.dagger.baselib.utils.ToastUtils.showToast("缺少权限")
                    finish()
                }
            }, {
                com.dagger.baselib.utils.ToastUtils.showToast("缺少权限")
                finish()
            })

        if (TRTCChatService.TRRCStarted) {
            showCallIngView()
        } else {
            initRoleView()
        }
    }

    fun getChatData() {
        vm.sdkAppId = intent.getIntExtra(TRTCChatVM.SDKAPPID, 0)
        vm.userSig = intent.getStringExtra(TRTCChatVM.SUERSIG).toString()
        vm.sid = intent.getStringExtra(TRTCChatVM.SID).toString()
        vm.createrid = intent.getLongExtra(TRTCChatVM.CREATERID, 0)
        vm.inviterid = intent.getLongExtra(TRTCChatVM.INVITERID, 0)
        vm.roomId = intent.getIntExtra(TRTCChatVM.ROOMID, 0)
        vm.chatType = 1
        vm.joinType = intent.getIntExtra(TRTCChatVM.JOIN_TYPE, 0)
        vm.TRTCType = intent.getIntExtra(TRTCChatVM.TRTC_TYPE, 0)
        vm.callTime = intent.getLongExtra(TRTCChatVM.CALL_TIME, 0)

        intent.getLongArrayExtra(TRTCChatVM.EXTRA_SELECTED_IDS)?.let {
            vm.chooseIds.clear()
            vm.chooseIds.addAll(it.toList())
        }
        if (!vm.chooseIds.contains(IMClient.getCurrentUserId())) {
            vm.chooseIds.add(IMClient.getCurrentUserId())
        }
        Log.e("aaa vm.chooseIds=${gson.toJson(vm.chooseIds)}")

        if (TRTCChatService.isStarted) {
            vm.chooseIds.clear()
            vm.chooseIds.addAll(TRTCChatService.chooseIds)
        }
        intent.getLongArrayExtra(TRTCChatVM.JOIN_IDS)?.let {
            vm.joinIds.clear()
            vm.joinIds.addAll(it.toList())
        }
        if (!vm.joinIds.contains(vm.createrid) && !TRTCChatService.TRRCStarted) {
            vm.joinIds.add(vm.createrid)
        }

        intent.getStringArrayExtra(TRTCChatVM.VIDEO_IDS)?.let {
            vm.userVideo.clear()
            vm.userVideo.addAll(it)
        }
    }

    //初始化腾讯音视频服务
    private fun initTRRC() {
        //语音服务是否启动
        if (TRTCChatService.isStarted) {
            btn_mic.isSelected = TRTCChatService.isEnableMic
            btn_handsfree.isSelected = TRTCChatService.isUseHandsfree
            btn_open_camera.isSelected = TRTCChatService.isOpenCamera
        } else {
            var intent = Intent(this@TRTCGroupChatActivity, TRTCChatService::class.java)
            intent.putExtra(TRTCChatVM.SID, vm.sid)
            intent.putExtra(TRTCChatVM.CREATERID, vm.createrid)
            intent.putExtra(TRTCChatVM.INVITERID, vm.inviterid)
            intent.putExtra(TRTCChatVM.ROOMID, vm.roomId)
            intent.putExtra(TRTCChatVM.SDKAPPID, vm.sdkAppId)
            intent.putExtra(TRTCChatVM.SUERSIG, vm.userSig)
            intent.putExtra(TRTCChatVM.CHAT_TYPE, vm.chatType)
            intent.putExtra(TRTCChatVM.JOIN_TYPE, vm.joinType)
            intent.putExtra(TRTCChatVM.TRTC_TYPE, vm.TRTCType)
            intent.putExtra(TRTCChatVM.EXTRA_SELECTED_IDS, vm.chooseIds.toLongArray())
            intent.putExtra(TRTCChatVM.VIDEO_IDS, vm.userVideo.toTypedArray())
            intent.putExtra(TRTCChatVM.CALL_TIME, vm.callTime)
            startService(intent)

            if (vm.joinType != 0) {
                vm.getJoinUser(1)
            }
        }
    }

    private fun initRoleView() {
        when (vm.joinType) {//判断进入语音聊天角色（0为被邀请者 1为创建者 2为中途加入）
            0 -> initTiwnCallView()
            1 -> initTiwnView()
            2 -> showCallIngView()
        }
    }

    //初始化呼叫页面
    private fun initTiwnView() {
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        imgAdd.visibility = View.VISIBLE
        llOperationView.setVisible(false)
        vm.playSounds(this@TRTCGroupChatActivity)
        //展示自己的界面
        trtcLayoutManager.setMySelfUserId(IMClient.getCurrentUserId().toString())
        vm.chooseIds.distinct().forEach { id ->
            var videoLayout = addUserToManager(id)
            videoLayout?.setVideoAvailable(false)
        }
    }

    //初始化被呼叫页面
    private fun initTiwnCallView() {
        llOperationView.setVisible(false)
        clChatIng.visibility = View.GONE
        clCallIng.visibility = View.VISIBLE
        imgAdd.visibility = View.GONE
        vm.playSounds(this@TRTCGroupChatActivity)
        //展示自己的界面
        trtcLayoutManager.setMySelfUserId(IMClient.getCurrentUserId().toString())
        vm.chooseIds.distinct().forEach {
            var videoLayout = addUserToManager(it)
            videoLayout?.setVideoAvailable(false)
        }
    }

    //初始化通话中的页面
    private fun showCallIngView() {
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        imgAdd.visibility = View.VISIBLE
        llOperationView.setVisible(true)
        if (btn_open_camera.isSelected) {
            imgSwitchCamera.setVisible(true)
        } else {
            imgSwitchCamera.setVisible(false)
        }
        trtcLayoutManager.setMySelfUserId(IMClient.getCurrentUserId().toString())
        vm.chooseIds.distinct().forEach { id ->
            var videoLayout: TRTCVideoLayout? = trtcLayoutManager.findCloudViewView(id.toString())
            if (videoLayout == null) {
                videoLayout = addUserToManager(id)
            }
            videoLayout?.setVideoAvailable(false)
            if (id == IMClient.getCurrentUserId() && TRTCChatService.isOpenCamera) {
                videoLayout?.setVideoAvailable(true)
                showLocalPreview(videoLayout)
            }
        }
        refreshUserVideo()
    }

    //展示自己视频页面
    private fun showLocalPreview(videoLayout: TRTCVideoLayout?) {
        io.reactivex.Observable.create<Boolean> {
            it.onNext(true)
            it.onComplete()
        }.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .scheduler()
            .subscribeEx {
                TRRCUtils.getInstanse().oListener?.openCamera(TRTCChatService.isFrontCamera, videoLayout?.videoView)
            }
    }

    //获取用户显示view
    private fun addUserToManager(uid: Long): TRTCVideoLayout? {
        val user = IMClient.userManager.getUserById(uid)
        val layout: TRTCVideoLayout = trtcLayoutManager.allocCloudVideoView(uid.toString()) ?: return null
        layout.userNameTv.text = user?.name
        layout.headImg.setCircleUserHeader(uid)
        return layout
    }

    private fun initListener() {
        img_back.setOnClickListenerEx {
            closeActivity()
        }
        imgAdd.setOnClickListenerEx {
            //群聊发起语音通话
            val intent = Intent()
            intent.action = applicationContext!!.packageName + ".VoiceChatUserSelectActivity"
            intent.putExtra("sid", vm.sid)
            intent.putExtra("hasChooseIds", vm.chooseIds.toLongArray())
            startActivityForResult(intent, 0x0018)//ChatActivity.REQ_SELECT_VOICE_USER
        }

        btn_refuse.setOnClickListenerEx {
            //拒绝接听
//            vm.getJoinUser(2)
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        btn_consent.setOnClickListenerEx {
            //接听
            vm.getJoinUser(1)
            TRRCUtils.getInstanse().oListener?.enterTRTCRoom()
        }

//        btn_mic.isActivated = true
//        btn_handsfree.isActivated = true
        btn_mic.isSelected = true
        btn_handsfree.isSelected = true

        btn_close.setOnClickListenerEx {
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            ToastUtils.showShort("已退出语音聊天")
            finish()
        }

        //切换摄像头
        imgSwitchCamera.setOnClickListenerEx {
            val layout: TRTCVideoLayout =
                trtcLayoutManager.findCloudViewView(IMClient.getCurrentUserId().toString())
            TRRCUtils.getInstanse().oListener?.openCamera(
                !TRTCChatService.isFrontCamera,
                layout?.videoView
            )
        }

        //打开关闭摄像头
        btn_open_camera.setOnClickListenerEx {
            val layout: TRTCVideoLayout =
                trtcLayoutManager.findCloudViewView(IMClient.getCurrentUserId().toString())
            if (TRTCChatService.isOpenCamera) {
                layout.setVideoAvailable(false)
                TRRCUtils.getInstanse().oListener?.closeCamera()
                btn_open_camera.isSelected = false
//                vm.toOpenVideo(false)//关闭
                imgSwitchCamera.setVisible(false)
            } else {
                layout.setVideoAvailable(true)
                TRRCUtils.getInstanse().oListener?.openCamera(
                    TRTCChatService.isFrontCamera,
                    layout?.videoView
                )
                btn_open_camera.isSelected = true
//                vm.toOpenVideo(true)//开启
                imgSwitchCamera.setVisible(true)
            }
        }

        //开关麦克风
        btn_mic.setOnClickListenerEx {
            val currentMode = !btn_mic.isSelected
            TRRCUtils.getInstanse().oListener?.enableMic(currentMode)
        }
        //开关免提
        btn_handsfree.setOnClickListenerEx {
            val currentMode = !btn_handsfree.isSelected
            TRRCUtils.getInstanse().oListener?.enableHandfree(currentMode)
        }
    }

    private var TRTCReturn = object : TRTCReturnListener {
        override fun enterTRTCRoomSucceed() {
            vm.stopSounds()
            clChatIng.visibility = View.VISIBLE
            clCallIng.visibility = View.GONE
            imgAdd.visibility = View.VISIBLE
            llOperationView.setVisible(true)
        }

        override fun enterTRTCRoomError() {
            ToastUtils.showShort("开启语音聊天失败...")
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        //麦克风开关回调
        override fun enableMicReturn(enable: Boolean?) {
            if (enable != null) {
                btn_mic.isSelected = enable
                if (enable) {
                    ToastUtils.showShort("您已取消静音")
                    vm.userMute.remove(IMClient.getCurrentUserId())
                } else {
                    ToastUtils.showShort("您已静音")
                    if (!vm.userMute.contains(IMClient.getCurrentUserId())) {
                        vm.userMute.add(IMClient.getCurrentUserId())
                    }
                }
            }
        }

        override fun enableHandfreeReturn(isUseHandsfree: Boolean?) {
            if (isUseHandsfree != null) {
                btn_handsfree.isSelected = isUseHandsfree
                if (isUseHandsfree) {
                    ToastUtils.showShort("扬声器")
                } else {
                    ToastUtils.showShort("耳机")
                }
            }
        }

        override fun timeReturn(time: String?) {
            tv_time.text = time
        }

        override fun onRoomUserChange(chooseIds: ArrayList<Long>, joinIds: ArrayList<Long>) {
        }

        //退出语音聊天
        override fun onCloseVoiceChat() {
            ToastUtils.showShort("语音聊天已结束")
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        //未进入语音聊天关闭
        override fun onCloseVoiceChatActivity() {
            showFloatingWindow = false
            finish()
        }

        override fun onUserAudioAvailable(userId: String, available: Boolean) {
            var user = userId.toLong()
            if (!available) {
                if (!vm.userMute.contains(user)) {
                    vm.userMute.add(user)
                }
            } else {
                vm.userMute.remove(user)
            }
        }

        override fun onUserVideoAvailable(userId: String, isVideoAvailable: Boolean) {
            //有用户的视频开启了
            var layout: TRTCVideoLayout? = trtcLayoutManager.findCloudViewView(userId)
            if (layout == null) {
                layout = addUserToManager(userId.toLong())
            }
            layout?.setVideoAvailable(isVideoAvailable)
            if (isVideoAvailable) {
                TRRCUtils.getInstanse().oListener?.startRemoteView(userId, layout?.videoView)
            } else {
                TRRCUtils.getInstanse().oListener?.stopRemoteView(userId)
            }
            if (!vm.chooseIds.contains(userId.toLong())) {
                vm.chooseIds.add(userId.toLong())
            }
            if (!vm.joinIds.contains(userId.toLong())) {
                vm.joinIds.add(userId.toLong())
            }
        }

        override fun onRemoteUserLeaveRoom(userId: String) {
            vm.chooseIds.remove(userId.toLong())
            vm.joinIds.remove(userId.toLong())
            trtcLayoutManager.recyclerCloudViewView(userId)
        }

        override fun onRemoteUserLeaveRoom(userIds: ArrayList<Long>) {
            vm.chooseIds.removeAll(userIds.toList())
            userIds.forEach { id ->
                trtcLayoutManager.recyclerCloudViewView(id.toString())
            }
        }

        override fun refreshUserVideoView() {
//            refreshUserVideo()
        }

        override fun addChooseUsers(addChooseUsers: MutableList<Long>) {
            addChooseUsers.forEach {
                if (!vm.chooseIds.contains(it)) {
                    vm.chooseIds.add(it)
                    var layout: TRTCVideoLayout? = addUserToManager(it)
                    layout?.setVideoAvailable(false)
                }
            }
        }

        override fun changeToVideoCall() {
        }

        override fun userVolumeChange(userVolumes: ArrayList<TRTCCloudDef.TRTCVolumeInfo>?) {
        }
    }

    fun refreshUserVideo() {
        TRTCChatService.isOpenVideoIds.forEach {
            //有用户的视频开启了
            var layout: TRTCVideoLayout? = trtcLayoutManager.findCloudViewView(it)
            if (layout == null) {
                layout = addUserToManager(it.toLong())
            }
            layout?.setVideoAvailable(true)
            TRRCUtils.getInstanse().oListener?.startRemoteView(it, layout?.videoView)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityUtils.isForeground(this@TRTCGroupChatActivity)) {
            if (TRTCChatService.isStarted) {
                TRRCUtils.getInstanse().oListener?.hintFloatingWindow()
                if (TRTCChatService.isOpenCamera) {
                    var videoLayout: TRTCVideoLayout? = trtcLayoutManager.findCloudViewView(IMClient.getCurrentUserId().toString())
                    if (videoLayout == null) {
                        videoLayout = addUserToManager(IMClient.getCurrentUserId())
                    }
                    videoLayout?.setVideoAvailable(true)
                    showLocalPreview(videoLayout)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!ActivityUtils.isForeground(this@TRTCGroupChatActivity)) {
            if (TRTCChatService.isStarted && showFloatingWindow) {
                if (!Settings.canDrawOverlays(this)) {
                    ToastUtils.showShort("请允许悬浮窗权限")
                    startActivityForResult(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ), TRTCChatVM.FLOATINGWINDOW
                    )
                } else {
                    TRRCUtils.getInstanse().oListener?.showFloatingWindow()
                }
            }
        }
    }

    fun closeActivity() {
        if (!Settings.canDrawOverlays(this)) {
            ToastUtils.showShort("请允许悬浮窗权限")
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ), TRTCChatVM.FLOATINGWINDOW
            )
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.stopSounds()
        TRRCUtils.getInstanse().rListener = null
        TRRCUtils.getInstanse().oListener?.stopRemoteView(IMClient.getCurrentUserId().toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("aaa requestCode=$requestCode   resultCode=$resultCode")
        when (requestCode) {
            0x0018 -> {
                data?.let {
                    var userIDs = it.getLongArrayExtra("userIds")?.toMutableList()
                    userIDs?.let { users ->
                        Log.e("aaa users=${gson.toJson(users)}")
                        val voiceChatMsg = MessageBuilder.createVoiceChatMsg(
                            vm.sid, SessionType.SESSION.value, vm.roomId, 5,
                            vm.chooseIds, users, vm.createrid, 1,
                            IMClient.getCurrentUserId()
                        )
                        IMClient.chatManager.sendMessage(voiceChatMsg)
                        TRRCUtils.getInstanse().oListener?.addChooseUser(users, voiceChatMsg.t)
                        IMClient.chatManager.setVoiceRoomMember(vm.sid, vm.roomId, users)?.toObservable()?.subscribe()
                        users.forEach {
                            var layout: TRTCVideoLayout? =
                                trtcLayoutManager.findCloudViewView(it.toString())
                            layout?.setVideoAvailable(false)
                        }
                    }
                }
            }
            TRTCChatVM.FLOATINGWINDOW -> {
                if (!Settings.canDrawOverlays(this)) {
                    ToastUtils.showShort("授权失败,已退出语音聊天")
                    TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
                    finish()
                } else {
                    ToastUtils.showShort("授权成功")
                    TRRCUtils.getInstanse().oListener?.showFloatingWindow()
                    finish()
                }
            }
        }
    }
}