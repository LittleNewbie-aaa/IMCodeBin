package com.im.trtc_call

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import com.dagger.baselib.utils.VibratorUtil
import com.liheit.im.common.ext.*
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMClient.getCurrentUserId
import com.liheit.im.core.bean.Department
import com.liheit.im.core.bean.User
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson
import com.tencent.trtc.TRTCCloudDef
import io.reactivex.Observable
import javax.inject.Inject

class TRTCChatVM @Inject constructor() : ViewModel() {
    var userList = MutableLiveData<MutableList<User>>()

    var sdkAppId: Int = 0
    var userSig: String = ""
    var sid: String = ""//当前会话sid
    var roomId: Int = 0//音视频房间id
    var createrid: Long = 0L//音视频聊天创建者id
    var inviterid: Long = 0L//邀请人id
    var chatType: Int = 0//会话类型(0单人 1多人)
    var joinType: Int = 0//人员角色（0：加入；1：创建）
    var TRTCType: Int = 0//音视频聊天类型（0：语音；1：视频）
    var callTime: Long = 0//音视频通话开始时间

    var users = mutableListOf<User>()//当前房间内人员列表
    var chooseIds = mutableListOf<Long>()//选择邀请聊天人员
    var joinIds = mutableListOf<Long>()//已加入聊天房间人员
    var userMute = mutableListOf<Long>()//房间内静音人员
    var userVideo = mutableListOf<String>()//房间内静音人员

    val result = Transformations.map(userList) {
        return@map it
    }!!

    //获取当前全部聊天人员
    fun getAllUser() {
        Observable.create<MutableList<User>> {
            var userList = chooseIds.union(joinIds)
            val users = IMClient.userManager.getUserByIds(userList.toMutableList()).toMutableList()
            users.forEach { user ->
                joinIds.forEach { id ->
                    if (id == user.id || user.id == createrid) {
                        user.voiceChatStates = 1
                    }
                }
            }
            it.onNext(users)
            it.onComplete()
        }.doOnNext {
            it.forEach { user ->
                user.isMute = false
                userMute.forEach { id ->
                    if (user.id == id) {
                        user.isMute = true
                    }
                }
            }
        }
                .scheduler()
                .subscribeEx {
                    users = it
                    userList.value = it
                }
    }

    //获取当前已加入聊天人员
    fun getJoinUser(status: Int) {
        IMClient.chatManager.joinVoiceCall(sid, roomId,
                getCurrentUserId(), status, inviterid)?.toObservable()
                ?.subscribeEx { msg ->
                    if (status == 1) {
                        Log.e("aaa getJoinUser${gson.toJson(msg)}")
                        joinIds.clear()
                        msg.ids?.let { joinIds.addAll(it) }
                        chooseIds.clear()
                        msg.allids?.let { chooseIds.addAll(it) }

                        msg.leaveids?.forEach {
                            chooseIds.remove(it)
                        }

                        msg.videoids?.let {
                            userVideo.clear()
                            userVideo.addAll(it)
                            TRRCUtils.getInstanse().rListener?.refreshUserVideoView()
                        }
                    }
                }
    }

    //设置静音说话
    fun userVolumeChange(userVolumes: MutableList<TRTCCloudDef.TRTCVolumeInfo>) {
        Observable.create<MutableList<User>> {
            users.forEach { user ->
                user.volumes = 0
                userVolumes.forEach { u ->
                    if (u.userId == null) {
                        u.userId = getCurrentUserId().toString()
                    }
                    if (user.id == u.userId.toLong()) {
                        user.volumes = u.volume
                    }
                }
            }
            it.onNext(users)
            it.onComplete()
        }.doOnNext {
            it.forEach { user ->
                user.isMute = false
                userMute.forEach { id ->
                    if (user.id == id) {
                        user.isMute = true
                    }
                }
            }
        }
                .scheduler()
                .subscribeEx {
                    userList.value = it
                }
    }

    //获取当前已加入聊天人员
    fun toOpenVideo(open: Boolean) {
        IMClient.chatManager.toOpenVideo(roomId, open).toObservable()
                ?.subscribeEx { mag -> }
    }

    fun setUserData(uid: Long, tvName: TextView, tvDuty: TextView, tvSection: TextView, imgHead: ImageView) {
        val user = IMClient.userManager.getUserById(uid)
        if (user != null) {
            tvName.text = user.name
            tvDuty.text = user.job
            IMClient.departmentManager.getUserDepartment(uid).forEach {
                var deps = mutableListOf<Department>(it)
                var name = deps.map { it.cname }.joinToString("/")
                tvSection.text = name
            }
            imgHead.setCircleUserHeader(user.id)
        }
    }

    private var audioManager: AudioManager? = null
    private var soundPool: SoundPool? = null
    private var outgoing: Int = 0
    private var streamID = -1
    private val handler: Handler = Handler()
    private var vibrator: VibratorUtil? = null
    fun playSounds(ac: Activity) {
        handler.postDelayed(Runnable {
            streamID = playMakeCallSounds()
        }, 300)
        soundPool = SoundPool(1, AudioManager.MODE_NORMAL, 0)
        outgoing = soundPool!!.load(ac.baseContext, R.raw.phonering, 1)

        vibrator = VibratorUtil(ac)
        var pattern = longArrayOf(1000, 500, 1000, 500)
        vibrator?.startVibratorForRepeat(pattern, 0)
    }

    /**
     * 播放来电铃声
     */
    fun playMakeCallSounds(): Int {
        return try {
            audioManager?.mode = AudioManager.MODE_RINGTONE
            audioManager?.isSpeakerphoneOn = true
            // play
            soundPool?.play(outgoing, // sound resource
                    0.3f, // left volume
                    0.3f, // right volume
                    1, // priority
                    -1, // loop，0 is no loop，-1 is loop forever
                    1f)!!
        } catch (e: Exception) {
            -1
        }
    }

    fun stopSounds() {
        soundPool?.stop(outgoing)
        vibrator?.cancleVibrator()
        soundPool?.release()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isMicrophoneMute = false
    }

    companion object {
        const val FLOATINGWINDOW = 0x0101

        const val SDKAPPID = "sdkappid"//腾讯实时音视频id
        const val SUERSIG = "usersig"//腾讯实时音视频签名
        const val SID = "sid"//会话id
        const val CHAT_TYPE = "chatType"//会话聊天类型
        const val CREATERID = "createrid"//通话创建者id
        const val INVITERID = "inviterid"//通话邀请者id
        const val ROOMID = "roomid"//通话房间id
        const val JOIN_TYPE = "joinType"//人员角色（0：加入；1：创建）
        const val EXTRA_SELECTED_IDS = "chooseIds"//选择聊天房间人员
        const val JOIN_IDS = "joinIds"//已加入人员
        const val TRTC_TYPE = "TRTCType"//通话类型
        const val VIDEO_IDS = "videoIds"//打开视频人员的id
        const val CALL_TIME = "callTime"//音视频通话开始时间
    }
}