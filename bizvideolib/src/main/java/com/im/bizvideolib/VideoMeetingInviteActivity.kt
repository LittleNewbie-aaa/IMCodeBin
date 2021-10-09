package com.im.bizvideolib

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.dagger.baselib.base.MainBaseActivity
import com.im.bizvideolib.dialogActivity.SendVideoMeetingMsgInviteDialog
import com.liheit.im.common.ext.subscribeEx
import com.liheit.im.core.Constants
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.User
import meeting.confcloud.cn.bizaudiosdk.BizVideoService
import us.zoom.androidlib.util.AndroidAppUtil

/**
 * 视频会议邀请
 */
class VideoMeetingInviteActivity : MainBaseActivity() {

//    var confNumber: String? = null
    var ids = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_video_meeting_invite_msg_dialog)

        val uri = intent.data
        val biz = BizVideoService.getInstance(this)

        // 标题
        val subject = intent.getStringExtra(AndroidAppUtil.EXTRA_SUBJECT)
        // 内容总结
        val text = intent.getStringExtra(AndroidAppUtil.EXTRA_TEXT)
        // 时间
        val time = intent.getStringExtra(AndroidAppUtil.EXTRA_TIME)
        // 重复
        val repeat = intent.getStringExtra(AndroidAppUtil.EXTRA_IS_REPEAT)
        // date
        val date = intent.getStringExtra(AndroidAppUtil.EXTRA_DATE)
        // 密码
        val psw = intent.getStringExtra(AndroidAppUtil.EXTRA_MEETING_PSW)
        // 原始密码
        val rawpsw = intent.getStringExtra(AndroidAppUtil.EXTRA_MEETING_RAW_PSW)
//        confNumber = biz.meetingNumber
//        val mData = StringBuffer()
//        mData.append("MeetingNumber：").append(biz.meetingNumber + "\n")
//                .append("MeetingID：").append(biz.meetingID + "\n")
//                .append("MeetingTopic：").append(biz.meetingTopic + "\n")
//                .append("joinMeetingURL：").append(biz.getjoinMeetingURL() + "\n")
//                .append("MeetingStartTime：").append("无" + "\n")
//                .append("RAW_PSW：").append(rawpsw + "\n")
//                .append("PSW：").append(psw + "\n")
//                .append("TIME：").append(time + "\n")
//                .append("EXTRA_DATE：").append(date + "\n")
//                .append("EXTRA_IS_REPEAT：").append(repeat + "\n")
//                .append("MeetingUserIdList=").append("${gson.toJson(biz.inMeetingUserIdList)}")

//        Log.Companion.e("aaa${mData.toString()}")

        ids.add(Constants.FILE_HELP_ID)

        getMeetingRoomData()
    }

    //获取视频会议房间信息
    private fun getMeetingRoomData() {
        val intent = Intent()
        intent.action = application.packageName + ".AddressBook"
        intent.putExtra("pass_addressbook", 1005)
        if (BizVideoClient.sid != null) {
            IMClient.chatManager.getMeetingRoomInfo(BizVideoClient.sid!!).toObservable()
                    .subscribeEx { info ->
                        ids.addAll(info.ids)
                        intent.putExtra("ids", ids.toLongArray())
                        startActivityForResult(intent, 0x00102)
                    }
        } else {
            intent.putExtra("ids", ids.toLongArray())
            startActivityForResult(intent, 0x00102)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            0x00102 -> {
                if (resultCode == Activity.RESULT_OK) {
                    //弹出确认选择框然后确认后转发
                    val userlist = data?.getParcelableArrayExtra("chooseIds")!!.toList() as List<User>
                    val content = userlist.map { it.cname }.filterIndexed { index, s -> index <= 30 }.joinToString("、")
                    SendVideoMeetingMsgInviteDialog.startAction(this,
                            userlist.toMutableList() as ArrayList<User>,
                            content, "")
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
        finish()
    }
}
