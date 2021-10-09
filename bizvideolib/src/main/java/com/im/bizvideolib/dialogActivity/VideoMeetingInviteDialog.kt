package com.im.bizvideolib.dialogActivity

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.dagger.baselib.utils.ToastUtils
import com.dagger.baselib.utils.VibratorUtil
import com.im.bizvideolib.BizVideoClient
import com.im.bizvideolib.R
import com.liheit.im.common.ext.setCircleUserHeader
import com.liheit.im.common.ext.setOnClickListenerEx
import com.liheit.im.common.ext.subscribeEx
import com.liheit.im.core.Cmd
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMClient.getCurrentUserId
import com.liheit.im.core.bean.Department
import com.liheit.im.core.manager.ChatManager
import com.liheit.im.core.protocol.MeetingCallNotice
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.fromJson
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.video_meeting_invite_dialog.*

/**
 * 弹出视频会议邀请Dialog
 */
class VideoMeetingInviteDialog : AppCompatActivity() {
    var fromid = 0L
    var confCreaterid = ""
    var protocoljoinurl: String? = null
    var confId: String? = null
    var sid: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_meeting_invite_dialog)

        IMClient.chatManager.addCmdMessageListener(cmdMsgListener)

        img_close.setOnClickListenerEx { finish() }
        //取消
        tv_cancel.setOnClickListenerEx { finish() }
        //确定
        tv_confirm.setOnClickListenerEx {
            RxPermissions(this@VideoMeetingInviteDialog)
                    .request(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    .subscribeEx({ it ->
                        if (it) {
                            openVideoMeeting()
                            finish()
                        } else {
                            ToastUtils.showToast("缺少权限")
                            finish()
                        }
                    }, {})
        }

        fromid = intent.getLongExtra("fromid", 0)
        protocoljoinurl = intent.getStringExtra("protocoljoinurl")
        confCreaterid = intent.getStringExtra("confCreaterid")
        confId = intent.getStringExtra("confId")
        sid = intent.getStringExtra("sid")

        initView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e("aaa onNewIntent")
        stopSounds()

        fromid = intent.getLongExtra("fromid", 0)
        protocoljoinurl = intent.getStringExtra("protocoljoinurl")
        confCreaterid = intent.getStringExtra("confCreaterid")
        confId = intent.getStringExtra("confId")
        sid = intent.getStringExtra("sid")

        initView()
    }

    fun initView() {

        val creater = IMClient.userManager.getUserById(fromid)
        tv_name.text = creater?.name
        IMClient.departmentManager.getUserDepartment(fromid).forEach {
            var deps = mutableListOf<Department>(it)
            var dName = deps.map { it.cname }.joinToString("/")
            tv_role.text = "$dName-${creater?.job}"
        }
        img_head.setCircleUserHeader(fromid)
        playSounds()

        handler.postDelayed(Runnable {
            finish()
        }, 30 * 1000)
    }

    //进入视频会议
    fun openVideoMeeting() {
        if (BizVideoClient.bizVideoService?.isAutoSuccess!!) {
            // 直接协议启会，解析URL
            val user = IMClient.userManager.getUserById(getCurrentUserId())
            protocoljoinurl?.let { url ->
                confId?.let {
                    BizVideoClient.joinMeetingUrl(this,
                            url, user!!.name, user.id.toString(), it, confCreaterid)
                    BizVideoClient.sendJoinLeaveMeetingRoom(sid!!, 1)
                    BizVideoClient.chatMessage = null
                }
            }
        }
    }

    private var cmdMsgListener = object : ChatManager.CmdMessageListener {
        override fun onMessageReceived(cmd: Int, data: String) {
            Log.e("aaa VideoMeetingInviteDialog data=$data")
            when (cmd) {
                //视频会议房间通知
                Cmd.ImpMeetingNotice -> {
                    val msg = data.fromJson<MeetingCallNotice>()
                    if (msg.sid == sid) {
                        if (msg.userid == getCurrentUserId() || msg.membernum == 0) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    val handler: Handler = Handler()
    var vibrator: VibratorUtil? = null
    private var audioManager: AudioManager? = null
    private var soundPool: SoundPool? = null
    private var outgoing: Int = 0
    private var streamID = -1
    private fun playSounds() {
        handler.postDelayed(Runnable {
            streamID = playMakeCallSounds()
        }, 300)
        soundPool = SoundPool(1, AudioManager.MODE_NORMAL, 0)
        outgoing = soundPool!!.load(this, R.raw.phonering, 1)

        vibrator = VibratorUtil(this@VideoMeetingInviteDialog)
        var pattern = longArrayOf(1000, 500, 1000, 500)
        vibrator?.startVibratorForRepeat(pattern, 0)
    }

    private fun stopSounds() {
        soundPool?.stop(outgoing)
        vibrator?.cancleVibrator()
        soundPool?.release()
    }

    /**
     * 播放来电铃声
     */
    private fun playMakeCallSounds(): Int {
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

    override fun onDestroy() {
        super.onDestroy()
        IMClient.chatManager.removeCmdMessageListener(cmdMsgListener)
        stopSounds()
    }
}
