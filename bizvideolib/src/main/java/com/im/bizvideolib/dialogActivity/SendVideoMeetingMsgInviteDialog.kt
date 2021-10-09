package com.im.bizvideolib.dialogActivity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.im.bizvideolib.BizVideoClient
import com.im.bizvideolib.R
import com.liheit.im.common.ext.setOnClickListenerEx
import com.liheit.im.core.bean.User
import com.liheit.im.utils.Log
import com.liheit.im.utils.json.gson
import kotlinx.android.synthetic.main.send_video_meeting_invite_msg_dialog.*

/**
 * 弹出发送视频会议邀请确认Dialog
 */
class SendVideoMeetingMsgInviteDialog : AppCompatActivity() {

    var userlist: MutableList<User>? = null
    var content: String? = null
    var confNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_video_meeting_invite_msg_dialog)
        //取消
        tv_cancel.setOnClickListenerEx {
            finish()
        }
        //确定
        tv_confirm.setOnClickListenerEx {
            sendVideoMeetingMsg()
        }

        initView()
    }

    fun initView() {
        confNumber = intent.getStringExtra("confNumber")
        userlist = intent.getParcelableArrayListExtra("userlist")
        content = intent.getStringExtra("content")

        Log.e("aaa${gson.toJson(userlist)}//$content")

        tv_content.text = "确认邀请“$content”参加会议吗？"
    }

    //发送邀请消息
    fun sendVideoMeetingMsg() {
        BizVideoClient.confId?.let {
            userlist?.toMutableList()?.let { users ->
                BizVideoClient.sendVideoMeetingMsg(it, BizVideoClient.confCreaterid, users)
            }
        }
        finish()
    }

    companion object {
        fun startAction(act: AppCompatActivity, userlist: ArrayList<User>,
                        content: String, confNumber: String) {
            var intent = Intent(act, SendVideoMeetingMsgInviteDialog::class.java)
            intent.putParcelableArrayListExtra("userlist", userlist)
            intent.putExtra("content", content)
            intent.putExtra("confNumber", confNumber)
            act.startActivity(intent)
        }
    }

}
