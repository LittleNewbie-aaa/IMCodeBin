package com.im.trtc_call.hy_call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import com.blankj.utilcode.util.ToastUtils
import com.dagger.baselib.base.BaseAdapter
import com.dagger.baselib.base.LinearLayoutManagerWrapper
import com.dagger.baselib.base.MainBaseActivity
import com.dagger.baselib.utils.ActivityUtils
import com.gyf.barlibrary.ImmersionBar
import com.im.trtc_call.*
import com.im.trtc_call.hy_call.adapter.VoiceChatInviteUserBinder
import com.im.trtc_call.hy_call.adapter.VoiceChatShowUserBinder
import com.liheit.im.common.ext.*
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMClient.getCurrentUserId
import com.liheit.im.core.MessageBuilder
import com.liheit.im.core.bean.Department
import com.liheit.im.core.bean.SessionType
import com.liheit.im.core.bean.User
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tencent.trtc.TRTCCloudDef
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import kotlinx.android.synthetic.main.activity_hy_voice_chat.*
import java.util.*

/**
 * 语音通话聊天
 */
@RequiresApi(Build.VERSION_CODES.M)
class HY_VoiceChatActivity : MainBaseActivity(), TRTCReturnListener {

    lateinit var vm: TRTCChatVM
    private var userAdapter = BaseAdapter()
    private var inviteUserAdapter = BaseAdapter()

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setTopColor()
        setContentView(R.layout.activity_hy_voice_chat)
        vm = getViewModel(TRTCChatVM::class.java, mModelFactory)

        initListener()

        TRRCUtils.getInstanse().rListener = this

        getChatData()

        RxPermissions(this@HY_VoiceChatActivity)
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

        if (vm.chatType != SessionType.SESSION_P2P.value) {
            toolbar.addRightImageButton(R.drawable.ic_toobar_add_white, R.id.action_add).setOnClickListenerEx {
                //群聊添加用户
                val intent = Intent()
                intent.action = applicationContext!!.packageName + ".VoiceChatUserSelectActivity"
                intent.putExtra("sid", vm.sid)
                intent.putExtra("hasChooseIds", vm.chooseIds.toLongArray())
                startActivityForResult(intent, 0x0018)//ChatActivity.REQ_SELECT_VOICE_USER
            }
        }

        initTRRC()

        initRoleView()

        vm.getAllUser()
    }

    //获取语音聊天相关数据
    fun getChatData() {
        vm.sdkAppId = intent.getIntExtra(TRTCChatVM.SDKAPPID, 0)
        vm.userSig = intent.getStringExtra(TRTCChatVM.SUERSIG).toString()
        vm.sid = intent.getStringExtra(TRTCChatVM.SID).toString()
        vm.createrid = intent.getLongExtra(TRTCChatVM.CREATERID, 0)
        vm.inviterid = intent.getLongExtra(TRTCChatVM.INVITERID, 0)
        vm.roomId = intent.getIntExtra(TRTCChatVM.ROOMID, 0)
        vm.chatType = intent.getIntExtra(TRTCChatVM.CHAT_TYPE, 0)
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
        Log.e("aaa vm.joinIds=${gson.toJson(vm.joinIds)}")
    }

    private fun initTRRC() {
        //语音服务是否启动
        if (TRTCChatService.isStarted) {
            btn_mic.isActivated = TRTCChatService.isEnableMic
            btn_handsfree.isActivated = TRTCChatService.isUseHandsfree
            btn_mic.isSelected = TRTCChatService.isEnableMic
            btn_handsfree.isSelected = TRTCChatService.isUseHandsfree
        } else {
            var intent = Intent(this@HY_VoiceChatActivity, TRTCChatService::class.java)
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

    //初始化页面
    private fun initRoleView() {
        if (vm.joinType == 0) {//判断进入语音聊天角色（0为被邀请者 1为创建者 2为中途加入）
            if (vm.chatType == SessionType.SESSION_P2P.value) {
                initTiwnCallView()
            } else {
                initPeopleCallView()
            }
        } else if (vm.joinType == 1) {
            if (vm.chatType == SessionType.SESSION_P2P.value) {
                initTiwnView()
            } else {
                initPeopleView()
            }
        } else if (vm.joinType == 2) {
            if (vm.chatType == SessionType.SESSION_P2P.value) {
            } else {
                toolbar.visibility = View.VISIBLE
                rvUserList.visibility = View.VISIBLE
                llUser.visibility = View.GONE
                tv_time.visibility = View.VISIBLE
                img_back.visibility = View.GONE
                clChatIng.visibility = View.VISIBLE
                clCallIng.visibility = View.GONE
                llInviteUser.visibility = View.GONE
                initUserAdapter()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityUtils.isForeground(this@HY_VoiceChatActivity)) {
            if (TRTCChatService.isStarted) {
                TRRCUtils.getInstanse().oListener?.hintFloatingWindow()
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onPause() {
        super.onPause()
        if (!ActivityUtils.isForeground(this@HY_VoiceChatActivity)) {
            if (TRTCChatService.isStarted && showFloatingWindow) {
                if (!Settings.canDrawOverlays(this)) {
                    ToastUtils.showShort("请允许悬浮窗权限")
                    startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")), FLOATINGWINDOW)
                } else {
                    TRRCUtils.getInstanse().oListener?.showFloatingWindow()
                }
            }
        }
    }

    //初始化单人聊天页面
    private fun initTiwnView() {
        toolbar.visibility = View.GONE
        rvUserList.visibility = View.GONE
        llUser.visibility = View.VISIBLE
        img_back.visibility = View.VISIBLE
        tv_time.visibility = View.VISIBLE
        llInviteUser.visibility = View.GONE
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        if (!TRTCChatService.TRRCStarted) {
            vm.playSounds(this)
        }
        setUserData(getCurrentUserId(), false)
    }

    //初始化单人聊天呼叫页面
    private fun initTiwnCallView() {
        toolbar.visibility = View.GONE
        rvUserList.visibility = View.GONE
        llUser.visibility = View.VISIBLE
        img_back.visibility = View.VISIBLE
        llInviteUser.visibility = View.GONE
        if (TRTCChatService.TRRCStarted) {
            tv_time.visibility = View.VISIBLE
            clChatIng.visibility = View.VISIBLE
            clCallIng.visibility = View.GONE
        } else {
            tv_time.visibility = View.GONE
            clChatIng.visibility = View.GONE
            clCallIng.visibility = View.VISIBLE
            vm.playSounds(this)
        }

        setUserData(getCurrentUserId(), false)
    }

    private fun setUserData(uid: Long, showCreator: Boolean) {
        vm.result.onNext(this) { users ->
            users.forEach { user ->
                if (showCreator) {
                    if (user.id == uid) {
                        tvName.text = user.name
                        tvDuty.text = user.job
                        IMClient.departmentManager.getUserDepartment(user.id).forEach {
                            var deps = mutableListOf<Department>(it)
                            var name = deps.map { it.cname }.joinToString("/")
                            tvSection.text = name
                        }
                        imgHead.setCircleUserHeader(user.id)
                    }
                } else {
                    if (user.id != uid) {
                        tvName.text = user.name
                        tvDuty.text = user.job
                        IMClient.departmentManager.getUserDepartment(user.id).forEach {
                            var deps = mutableListOf<Department>(it)
                            var name = deps.map { it.cname }.joinToString("/")
                            tvSection.text = name
                        }
                        imgHead.setCircleUserHeader(user.id)
                    }
                }
            }
        }
    }

    //初始化群聊天页面
    private fun initPeopleView() {
        toolbar.visibility = View.VISIBLE
        rvUserList.visibility = View.VISIBLE
        llUser.visibility = View.GONE
        tv_time.visibility = View.VISIBLE
        img_back.visibility = View.GONE
        clChatIng.visibility = View.VISIBLE
        clCallIng.visibility = View.GONE
        llInviteUser.visibility = View.GONE
        if (!TRTCChatService.TRRCStarted) {
            vm.playSounds(this)
        }

        initUserAdapter()
    }

    private fun initUserAdapter() {
        userAdapter.register(User::class.java, VoiceChatShowUserBinder())

        rvUserList.adapter = userAdapter
        rvUserList.layoutManager = LinearLayoutManagerWrapper(this)
        rvUserList.addItemDecoration(HorizontalDividerItemDecoration.Builder(this)
                .size(1).color(getColorEx(R.color.voiceChatDivided)).build())

        vm.result.onNext(this) {
            userAdapter.mData.clear()
            userAdapter.mData.addAll(it)
            userAdapter.notifyDataSetChanged()
        }
    }

    //初始化群聊天呼叫页面
    private fun initPeopleCallView() {
        if (TRTCChatService.TRRCStarted) {
            toolbar.visibility = View.VISIBLE
            rvUserList.visibility = View.VISIBLE
            tv_time.visibility = View.VISIBLE
            img_back.visibility = View.GONE
            clChatIng.visibility = View.VISIBLE
            clCallIng.visibility = View.GONE
            llUser.visibility = View.GONE
            llInviteUser.visibility = View.GONE

            initUserAdapter()
        } else {
            toolbar.visibility = View.GONE
            rvUserList.visibility = View.GONE
            tv_time.visibility = View.GONE
            img_back.visibility = View.VISIBLE
            clChatIng.visibility = View.INVISIBLE
            clCallIng.visibility = View.VISIBLE
            llUser.visibility = View.VISIBLE
            llInviteUser.visibility = View.VISIBLE

            vm.playSounds(this)
            setUserData(vm.createrid, true)
            initInviteUserAdapter()
        }
    }

    private fun initInviteUserAdapter() {
        inviteUserAdapter.register(User::class.java, VoiceChatInviteUserBinder())

        rvInviteUser.adapter = inviteUserAdapter
        rvInviteUser.layoutManager = LinearLayoutManagerWrapper(this, RecyclerView.HORIZONTAL, false)

        vm.result.onNext(this) {
            inviteUserAdapter.mData.clear()
            inviteUserAdapter.mData.addAll(it)
            inviteUserAdapter.notifyDataSetChanged()
        }
    }

    private fun initListener() {
        toolbar.addLeftBackImageButton().setOnClickListenerEx {
            closeActivity()
        }

        img_back.setOnClickListenerEx {
            closeActivity()
        }

        btn_refuse.setOnClickListenerEx {
            //拒绝接听
            vm.getJoinUser(2)
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            finish()
        }

        btn_consent.setOnClickListenerEx {
            vm.getJoinUser(1)
            TRRCUtils.getInstanse().oListener?.enterTRTCRoom()
        }

        btn_mic.isActivated = true
        btn_handsfree.isActivated = true
        btn_mic.isSelected = true
        btn_handsfree.isSelected = true

        btn_close.setOnClickListenerEx {
            TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
            ToastUtils.showShort("已退出语音聊天")
            finish()
        }

        btn_mic.setOnClickListenerEx {
            val currentMode = !btn_mic.isSelected
            // 开关麦克风
            TRRCUtils.getInstanse().oListener?.enableMic(currentMode)
        }

        btn_handsfree.setOnClickListenerEx {
            val currentMode = !btn_handsfree.isSelected
            TRRCUtils.getInstanse().oListener?.enableHandfree(currentMode)
        }
    }

    //麦克风开关回调
    override fun enableMicReturn(enable: Boolean?) {
        if (enable != null) {
            btn_mic.isSelected = enable
            if (enable) {
                ToastUtils.showShort("您已取消静音")
                vm.userMute.remove(getCurrentUserId())
            } else {
                ToastUtils.showShort("您已静音")
                if (!vm.userMute.contains(getCurrentUserId())) {
                    vm.userMute.add(getCurrentUserId())
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

    //进入语音聊天房间成功
    override fun enterTRTCRoomSucceed() {
        vm.stopSounds()
        initRoleView()
    }

    //进入语音聊天房间失败
    override fun enterTRTCRoomError() {
        ToastUtils.showShort("开起语音聊天失败...")
        TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
        finish()
    }

    //未进入语音聊天关闭
    override fun onCloseVoiceChatActivity() {
        showFloatingWindow = false
        finish()
    }

    //退出语音聊天
    override fun onCloseVoiceChat() {
        ToastUtils.showShort("语音聊天已结束")
        TRRCUtils.getInstanse().oListener?.exitTRTCRoom()
        finish()
    }

    //监听谁在说话
    override fun userVolumeChange(userVolumes: ArrayList<TRTCCloudDef.TRTCVolumeInfo>) {
        vm.userVolumeChange(userVolumes.toMutableList())
    }

    //监听谁设置静音
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

    override fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean) {}

    //房间人员变化监听
    override fun onRoomUserChange(chooseIds: ArrayList<Long>, joinIds: ArrayList<Long>) {
        vm.chooseIds = chooseIds
        vm.joinIds = joinIds
        vm.getAllUser()
    }

    override fun addChooseUsers(addChooseUsers: MutableList<Long>) {
        addChooseUsers.forEach {
            if (!vm.chooseIds.contains(it)) {
                vm.chooseIds.add(it)
            }
        }
        vm.getAllUser()
    }

    override fun onRemoteUserLeaveRoom(userId: String) {}

    //超过30秒未加入移除人员
    override fun onRemoteUserLeaveRoom(userIds: ArrayList<Long>) {
        Log.e("aaa userIds=${userIds}")
        userIds.forEach {
            vm.chooseIds.remove(it)
            vm.joinIds.remove(it)
        }
        vm.getAllUser()
    }

    override fun refreshUserVideoView() {}

    override fun changeToVideoCall() {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            closeActivity()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("NewApi")
    fun closeActivity() {
        if (!TRTCChatService.isStarted) {
            finish()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            ToastUtils.showShort("请允许悬浮窗权限")
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")), 0)
        } else {
            TRRCUtils.getInstanse().oListener?.showFloatingWindow()
            finish()
        }
    }

    companion object {
        const val FLOATINGWINDOW = 0x0101

        const val SID = "sid"//聊天id
        const val CREATERID = "createrid"
        const val ROOMID = "roomid"//房间id
        const val SDKAPPID = "sdkappid"
        const val SUERSIG = "usersig"
        const val CHAT_TYPE = "chatType"//聊天类型
        const val JOIN_TYPE = "joinType"//人员角色（0：加入；1：创建）
        const val EXTRA_SELECTED_IDS = "chooseIds"//选择聊天房间人员
        const val JOIN_IDS = "joinIds"//已加入人员

        fun startAction(
                activity: AppCompatActivity, sid: String, createrid: Long,
                roomId: Int, usersig: String, sdkappid: Int, users: MutableList<Long>,
                joinIds: MutableList<Long>, chatType: Int, joinType: Int
        ) {
            var intent = Intent(activity, HY_VoiceChatActivity::class.java)
            intent.putExtra(SID, sid)
            intent.putExtra(CREATERID, createrid)
            intent.putExtra(ROOMID, roomId)
            intent.putExtra(SUERSIG, usersig)
            intent.putExtra(SDKAPPID, sdkappid)
            intent.putExtra(EXTRA_SELECTED_IDS, users.toLongArray())
            intent.putExtra(JOIN_IDS, joinIds.toLongArray())
            intent.putExtra(CHAT_TYPE, chatType)
            intent.putExtra(JOIN_TYPE, joinType)
            activity.startActivity(intent)
        }
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            0x0018 -> {
                data?.let {
                    var userIDs = it.getLongArrayExtra("userIds")?.toMutableList()
                    userIDs?.let { users ->
                        val voiceChatMsg = MessageBuilder.createVoiceChatMsg(
                                vm.sid, SessionType.SESSION.value, vm.roomId, 5,
                                vm.chooseIds, users, vm.createrid, 0,
                                IMClient.getCurrentUserId()
                        )
                        TRRCUtils.getInstanse().oListener?.addChooseUser(users, voiceChatMsg.t)
                        IMClient.chatManager.sendMessage(voiceChatMsg)
                        IMClient.chatManager.setVoiceRoomMember(vm.sid, vm.roomId, users)?.toObservable()?.subscribe()
                        vm.chooseIds.addAll(users)
                        vm.getAllUser()
                    }
                }
            }
        }
        if (requestCode == 0) {
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

    override fun onDestroy() {
        super.onDestroy()
        vm.stopSounds()
        TRRCUtils.getInstanse().rListener = null
    }

}