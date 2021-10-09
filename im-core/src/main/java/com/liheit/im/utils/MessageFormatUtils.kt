package com.liheit.im.utils

import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.protocol.*

/**
 * Created by daixun on 2018/6/29.
 */

class MessageFormatUtils {

    companion object {
        @JvmStatic
        fun simpleFormat(msg: ChatMessage, type: Int? = 0): String {
            if (msg.isRecall())
                return "${msg.from}撤回了一条消息"
            return msg.msgs?.map {
                return@map when (it) {
                    is AtBody -> it.name + " "
                    is TextBody -> it.text
                    is EmojiBody -> if(type==0){
                        "[${it.key}]"
                    }else{
                        it.text
                    }
                    is ImgBody -> "[图片]"
                    is AudioBody -> "[语音]"
                    is VideoBody -> "[视频]"
                    is LocBody -> "[位置]"
                    is AttachBody -> "[文件]"
                    is AttachBody -> "[图文消息]"
                    is EmailBody -> "[邮件]"
                    is AppRemindBody -> "[消息提醒]"
                    is WorkFlowBody -> "[流程]"
                    is WorkScheduleBody -> "[日程]"
                    is WorkTaskBody -> "[任务]"
                    is UpgradeBody -> "应用更新"
                    is RefsHeadBody -> it.text
                    is RefsEndBody -> it.text
                    is MergeForwardBody -> "[聊天记录]"
                    is NoticeBody -> it.subject
                    is EditSessionBody -> "[通知]"
                    is AutoReplyBody -> it.text
                    is VoiceChatBody ->
                        if (it.trtctype == 0) {
                            "[语音聊天]"
                        } else {
                            "[视频聊天]"
                        }
                    is VideoConferenceBody -> "[即时会议]"
                    is VoteBody -> "[投票]"
                    is SolitaireBody -> "[接龙]"
                    is NewReplyBody -> ""
                    else -> "不支持的消息"
                }
            }?.joinToString("") ?: ""
        }
    }
}
