package com.im.trtc_call.solo

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
import com.liheit.im.common.ext.*
import com.liheit.im.core.IMClient
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tencent.trtc.TRTCCloudDef
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_trtc_solo_chat.*
import java.util.*


/**
 * 单人通话聊天
 */
@SuppressLint("NewApi")
class TRTCSoloChatActivity : MainBaseActivity() {
    lateinit var vm: TRTCChatVM

    private var showFloatingWindow = true

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
        setContentView(R.layout.activity_trtc_solo_chat)
        vm = getViewModel(TRTCChatVM::class.java, mModelFactory)

        initListener()

        TRRCUtils.getInstanse().rListener = TRTCReturn

        getChatData()

        RxPermissions(this@TRTCSoloChatActivity)
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
            if (vm.TRTCType == 1) {
                Observable.create<Boolean> {
                    it.onNext(true)
                    it.onComplete()
                }.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .scheduler()
                    .subscribeEx {
                        showLocalPreview()
                        showRemoteVideoFlow(TRTCChatService.remoteIsOpenCamera)
                    }
            }
        } else {
            initRoleView()
        }
    }

    //初始化腾讯音视频服务
    private fun initTRRC() {
        //语音服务是否启动
        if (TRTCChatService.isStarted) {
//            btn_mic.isActivated = TRTCChatService.isEnableMic
//            btn_handsfree.isActivated = TRTCChatService.isUseHandsfree
            btn_mic.isSelected = TRTCChatService.isEnableMic
            btn_handsfree.isSelected = TRTCChatService.isUseHandsfree
        } else {
            var intent = Intent(this@TRTCSoloChatActivity, TRTCChatService::class.java)
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
            intent.putExtra(TRTCChatVM.CALL_TIME, vm.callTime)
            startService(intent)

            if (vm.joinType != 0) {
                vm.getJoinUser(1)
            }
        }
    }

    private fun getChatData() {
        vm.sdkAppId = intent.getIntExtra(TRTCChatVM.SDKAPPID, 0)
        vm.userSig = intent.getStringExtra(TRTCChatVM.SUERSIG).toString()
        vm.sid = intent.getStringExtra(TRTCChatVM.SID).toString()
        vm.createrid = intent.getLongExtra(TRTCChatVM.CREATERID, 0)
        vm.inviterid = intent.getLongExtra(TRTCChatVM.INVITERID, 0)
        vm.roomId = intent.getIntExtra(TRTCChatVM.ROOMID, 0)
        vm.chatType = 0
        vm.joinType = intent.getIntExtra(TRTCChatVM.JOIN_TYPE, 0)
        vm.TRTCType = intent.getIntExtra(TRTCChatVM.TRTC_TYPE, 0)
        vm.callTime = intent.getLongExtra(TRTCChatVM.CALL_TIME, 0)

        intent.getLongArrayExtra(TRTCChatVM.EXTRA_SELECTED_IDS)?.let {
            vm.chooseIds.clear()
            vm.chooseIds.addAll(it.toList())
        }
        Log.e("aaa vm.chooseIds=${gson.toJson(vm.chooseIds)}")
        intent.getLongArrayExtra(TRTCChatVM.JOIN_IDS)?.let {
            vm.joinIds.clear()
            vm.joinIds.addAll(it.toList())
        }
        if (!vm.joinIds.contains(vm.createrid) && !TRTCChatService.TRRCStarted) {
            vm.joinIds.add(vm.createrid)
        }
    }

    private fun initRoleView() {
        when (vm.joinType) {//判断进入语音聊天角色（0为被邀请者 1为创建者 2为中途加入）
            0 -> initTiwnCallView()
            1 -> initTiwnView()
        }
    }

    private fun initTRTCTypeView() {
        Log.e("aaa vm.TRTCType=${vm.TRTCType}")
        llLeftView.setVisible(true)
        llRightView.setVisible(true)
        btnSwitchType.setVisible(vm.TRTCType == 1)
        tvSwitchType.setVisible(vm.TRTCType == 1)
        btn_mic.setVisible(vm.TRTCType == 0)
        tv_mic.setVisible(vm.TRTCType == 0)
        btnSwitchCamera.setVisible(vm.TRTCType == 1)
        tvSwitchCamera.setVisible(vm.TRTCType == 1)
        btn_handsfree.setVisible(vm.TRTCType == 0)
        tv_handsfree.setVisible(vm.TRTCType == 0)
    }

    //初始化呼叫页面
    private fun initTiwnView() {
        llUser.visibility = View.VISIBLE
        img_back.visibility = View.VISIBLE
        tv_time.visibility = View.VISIBLE
        tv_time.text = "正在等待对方接受邀请"
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        llLeftView.visibility = View.INVISIBLE
        llRightView.visibility = View.INVISIBLE
        vm.playSounds(this@TRTCSoloChatActivity)
        val users = vm.chooseIds.filter { it != IMClient.getCurrentUserId() }
        vm.setUserData(users[0], tvName, tvDuty, tvSection, imgHead)

        if (vm.TRTCType == 1) {
            Observable.create<Boolean> {
                it.onNext(true)
                it.onComplete()
            }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .scheduler()
                .subscribeEx {
                    showLocalPreview()
                }
        }
    }

    //初始化被呼叫页面
    private fun initTiwnCallView() {
        llUser.visibility = View.VISIBLE
        img_back.visibility = View.VISIBLE
        tv_time.visibility = View.VISIBLE
        tv_time.visibility = View.GONE
        clChatIng.visibility = View.GONE
        clCallIng.visibility = View.VISIBLE
        vm.playSounds(this@TRTCSoloChatActivity)
        vm.setUserData(vm.createrid, tvName, tvDuty, tvSection, imgHead)

        if (vm.TRTCType == 1) {
            Observable.create<Boolean> {
                it.onNext(true)
                it.onComplete()
            }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .scheduler()
                .subscribeEx {
                    showLocalPreview()
                }
        }
    }

    //展示自己视频页面
    private fun showLocalPreview() {
        trtcLayoutManager.setMySelfUserId(IMClient.getCurrentUserId().toString())
        var videoLayout: TRTCVideoLayout? =
            trtcLayoutManager.findCloudViewView(IMClient.getCurrentUserId().toString())
        if (videoLayout == null) {
            videoLayout = addUserToManager(IMClient.getCurrentUserId())
        }
        videoLayout?.setVideoAvailable(true)
        TRRCUtils.getInstanse().oListener?.openCamera(
            TRTCChatService.isFrontCamera, videoLayout?.videoView
        )
    }

    //拉取远端视频流
    private fun showRemoteVideoFlow(isVideoAvailable: Boolean) {
        val users = vm.chooseIds.filter { it != IMClient.getCurrentUserId() }[0]
        var layout: TRTCVideoLayout? = trtcLayoutManager.findCloudViewView(users.toString())
        if (layout == null) {
            layout = addUserToManager(users)
        }
        layout?.setVideoAvailable(isVideoAvailable)
        if (isVideoAvailable) {
            TRRCUtils.getInstanse().oListener?.startRemoteView(users.toString(), layout?.videoView)
        } else {
            TRRCUtils.getInstanse().oListener?.stopRemoteView(users.toString())
        }
    }

    //初始化通话中显示页面
    private fun showCallIngView() {
        img_back.visibility = View.VISIBLE
        tv_time.visibility = View.VISIBLE
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        initTRTCTypeView()
        if (vm.TRTCType == 0) {
            trtcLayoutManager.visibility = View.GONE
            llUser.visibility = View.VISIBLE
            val users = vm.chooseIds.filter { it != IMClient.getCurrentUserId() }
            vm.setUserData(users[0], tvName, tvDuty, tvSection, imgHead)
        } else {
            Log.e("aaa IMClient.getCurrentUserId()=${IMClient.getCurrentUserId()}")
            trtcLayoutManager.visibility = View.VISIBLE
            llUser.visibility = View.GONE
        }
    }

    //获取用户显示view
    private fun addUserToManager(uid: Long): TRTCVideoLayout? {
        val user = IMClient.userManager.getUserById(uid)
        val layout: TRTCVideoLayout =
            trtcLayoutManager.allocCloudVideoView(uid.toString()) ?: return null
        layout.userNameTv.text = user?.name
        layout.headImg.setCircleUserHeader(uid)
        return layout
    }

    private fun initListener() {
        img_back.setOnClickListenerEx {
            closeActivity()
        }

        btn_refuse.setOnClickListenerEx {
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        btn_consent.setOnClickListenerEx {
            vm.getJoinUser(1)
            TRRCUtils.getInstanse().oListener?.enterTRTCRoom()
        }

        btn_mic.isSelected = true
        btn_handsfree.isSelected = true

        btn_close.setOnClickListenerEx {
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            ToastUtils.showShort("已退出语音聊天")
            finish()
        }

        //切换到语音通话
        btnSwitchType.setOnClickListenerEx {
            TRRCUtils.getInstanse().oListener?.switchVideoCall()
        }

        //切换摄像头
        btnSwitchCamera.setOnClickListenerEx {
            val layout: TRTCVideoLayout =
                trtcLayoutManager.findCloudViewView(IMClient.getCurrentUserId().toString())
            TRRCUtils.getInstanse().oListener?.openCamera(
                !TRTCChatService.isFrontCamera,
                layout?.videoView
            )
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
            showCallIngView()
        }

        override fun enterTRTCRoomError() {
            ToastUtils.showShort("开启语音聊天失败...")
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        override fun onRoomUserChange(chooseIds: ArrayList<Long>, joinIds: ArrayList<Long>) {
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

        override fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean) {
            //有用户的视频开启了
            Observable.create<Boolean> {
                it.onNext(true)
                it.onComplete()
            }.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .scheduler()
                .subscribeEx {
                    showRemoteVideoFlow(isVideoAvailable)
                }
        }

        override fun onRemoteUserLeaveRoom(userId: String?) {
        }

        override fun onRemoteUserLeaveRoom(userIds: ArrayList<Long>?) {
        }

        override fun refreshUserVideoView() {
        }

        override fun addChooseUsers(addChooseUsers: MutableList<Long>?) {
        }

        //切换到音频通话处理UI
        override fun changeToVideoCall() {
            trtcLayoutManager.visibility = View.GONE
            trtcLayoutManager.removeAllViews()
            llUser.visibility = View.VISIBLE
            vm.TRTCType = 0
            val users = vm.chooseIds.filter { it != IMClient.getCurrentUserId() }
            vm.setUserData(users[0], tvName, tvDuty, tvSection, imgHead)
            initTRTCTypeView()
        }

        override fun userVolumeChange(userVolumes: ArrayList<TRTCCloudDef.TRTCVolumeInfo>?) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityUtils.isForeground(this@TRTCSoloChatActivity)) {
            if (TRTCChatService.isStarted) {
                TRRCUtils.getInstanse().oListener?.hintFloatingWindow()
                if (vm.TRTCType == 1 && TRTCChatService.TRRCStarted) {
                    Observable.create<Boolean> {
                        it.onNext(true)
                        it.onComplete()
                    }.subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .scheduler()
                        .subscribeEx {
                            showLocalPreview()
                            showRemoteVideoFlow(TRTCChatService.remoteIsOpenCamera)
                        }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!ActivityUtils.isForeground(this@TRTCSoloChatActivity)) {
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
        trtcLayoutManager.removeAllViews()
        if (TRTCChatService.TRRCStarted) {
            TRRCUtils.getInstanse().oListener?.stopRemoteView(
                IMClient.getCurrentUserId().toString()
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TRTCChatVM.FLOATINGWINDOW) {
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
        super.onActivityResult(requestCode, resultCode, data)
    }
}