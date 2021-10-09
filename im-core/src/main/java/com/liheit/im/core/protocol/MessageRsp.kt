package com.liheit.im.core.protocol

import android.arch.persistence.room.TypeConverters
import android.os.Parcelable
import com.liheit.im.core.bean.MessageType
import com.liheit.im.core.bean.Subscription
import com.liheit.im.utils.json.MessageListConverter
import com.liheit.im.utils.ProtocolExclusionStrategy.ProtocolExclude
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue


/*{
    "fromid":900003,
    "t":1529572032,
    "utime":0,
    "mid":"{89991F9B-8DB8-467A-9B9A-1F196372A1AF}",
    "from":"tgaozairui",
    "toid":1000009,
    "flag":0,
    "sid":"{000DBBA3-0000-0000-0000000F4249}",
    "type":0,
    "name":"高再锐",
    "msgs":[
    {
        "mtype":0,
        "text":"束手束脚时间"
    }
    ]
}*/
/**
 * Created by daixun on 2018/6/21.
 *
 */
data class MessageRsp(
        var fromid: Int = 0,
        var t: Long = 0,
        var utime: Int = 0,
        var mid: String = "",
        var from: Int = 0,
        var toid: Int = 0,
        var flag: Int = 0,
        var sid: String,
        var type: Int,
        var name: String,
        var msgs: MutableList<MessageBody>? = null
)

data class MessageBody(
        var mtype: Int = 0,
        var text: String = "",
        var key: String = "",

        //图片的属性
        var token: String = "",
        var bytes: Long = 0,
        var sizew: Int = 0,
        var sizeh: Int = 0,
        var md5: String = "",
        @ProtocolExclude var localPath: String? = null,
        @ProtocolExclude var isupload: Int = 0,//上传标识，默认为 0，为下载
        @ProtocolExclude var status: Int = 0,//传输进度[0~100]，默认为 0

        var t: Int = 0, //音视频长度
        var id: Long = 0, //@用户的ID
        var name: String = "",//@名字 如果是图片或文件消息，则是图片或者文件名

        var address: String? = null,
        var buildingName: String? = null,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,

        var fromid: Long = 0,
        var mid: String = "",
        var mids: MutableList<String>? = null,
        var briefs: MutableList<String>? = null,//消息摘要 最多前三条，每条最长120字符
        var sid: String = "",//转发消息的源会话ID（主要用于追踪来源，发送者需要填，接收者不处理）
        var title: String = "",//合并消息的标题，格式为"群聊的聊天记录"、"xxx 与xxx的聊天记录"
        var total: Int = 0,
        var articles: MutableList<Article>? = null,//文章集合
        var subject: String = "",
        var logo: String = "",
        var subject2: String = "",
        var appurl: String = "",
        var mount_id: Long = 0,//     n   云盘文件目录 ID[mount_id]
        var link: String = "",// n     云盘链接[filelink]
        var localpath: String? = null,// y     【本地属性】本地文件位置
        var save2cloud: Boolean? = null,//     【本地属性】是否已经保存到了云盘
        var type: Int = 0, //类型
        var digest: String = "",//     摘要
        var pcurl: String? = null,//     桌面端打开 URL
        var author: String? = null,//     作者
        var createrid: Long = 0,//语音聊天房间创建者
        var roomid: Int = 0, //语音聊天房间id
        var audiotype: Int = 0, //语音聊天状态
        val toids: MutableList<Long>? = null,
        val addids: MutableList<Long>? = null,
        val context: String = ""
) {
    fun toRealType(): MsgBody {
        return when (mtype) {
//            MessageType.TEXT.value -> TextBody(text = text)
//            MessageType.EMOT.value -> EmojiBody(key = key, text = text)
            MessageType.REPLY_BEGIN.value -> RefsHeadBody(mid = mid, fromid = fromid, text = text)
            MessageType.REPLY_END.value -> RefsEndBody(id = id, text = text)
            MessageType.AT.value -> AtBody(id = id, name = name)
            MessageType.ANNEX.value -> AttachBody(
                    token = token,
                    bytes = bytes,
                    md5 = md5,
                    name = name
            )
            MessageType.IMAGE.value -> ImgBody(
                    token = token,
                    bytes = bytes,
                    md5 = md5,
                    name = name,
                    sizeh = sizeh,
                    sizew = sizew,
                    secrecy = 0
            )
            MessageType.VIDEO.value -> VideoBody(
                    token = token,
                    bytes = bytes,
                    md5 = md5,
                    name = name,
                    t = t
            )
            MessageType.VOICE.value -> AudioBody(
                    token = token,
                    bytes = bytes,
                    md5 = md5,
                    name = name,
                    t = t
            )
            MessageType.LOCATION.value -> LocBody(
                    address = address,
                    buildingName = buildingName,
                    latitude = latitude,
                    longitude = longitude
            )
            MessageType.FORWARD.value -> MergeForwardBody(
                    sid = sid,
                    title = title,
                    key = key,
                    token = token,
                    bytes = bytes,
                    md5 = md5,
                    name = name,
                    briefs = ArrayList<String>().also { a ->
                        briefs?.forEach { i -> a.add(i) }

                    })
            MessageType.NOTIFICATION.value ->
                NoticeBody(
                        type = type,
                        key = key,
                        subject = subject,
                        subject2 = subject2,
                        digest = digest,
                        logo = logo,
                        appurl = appurl,
                        pcurl = pcurl,
                        author = author,
                        taskStatus = 0,
                        isRead = 0,
                        context = context
                )
            MessageType.WEBAPP_CLOUD_FILE.value ->
                CloudFileBody(
                        token = token,
                        bytes = bytes,
                        md5 = md5,
                        name = name,
                        mount_id = mount_id,
                        link = link,
                        localpath = localpath,
                        save2cloud = save2cloud,
                        status = status
                )
            MessageType.VOICECHAT.value ->
                VoiceChatBody(
                        createrid = createrid,
                        roomid = roomid,
                        audiotype = audiotype,
                        toids = toids,
                        addids = addids
                )
//            MessageType.VIDEOCONFERENCE.value ->
//                VideoConferenceBody(
//                        createrid = createrid,
//                        protocoljoinurl = protocoljoinurl,
//                        protocolhoststarturl = protocolhoststarturl,
//                        hoststarturl = hoststarturl, joinurl = joinurl,
//                        confparties = confparties, duration = duration,
//                        confNumber = confNumber, toids = toids)
            else -> {
                throw RuntimeException("未实现的类型")
            }
        }
    }

}

interface MsgBody : Parcelable {
    val mtype: Int
}

/**
 * 文件消息
 */
interface FileBody : Parcelable{
    var name: String
    var token: String
    var bytes: Long
    var md5: String
    var localPath: String
}

/**
 * 未知消息类型
 */
@Parcelize
data class UnsupportBody(
        @IgnoredOnParcel val map: HashMap<String, @RawValue Any?>
) : MsgBody, MutableMap<String, Any?> by map {
    override val mtype: Int = (map.get("mtype") as? Number)?.toInt() ?: -1
}

/**
 * 无痕消息类型
 */
interface SecrecyBody : Parcelable{
    var secrecy: Int //0是普通消息 1是无痕消息
}

/**
 * 文本消息类型
 */
@Parcelize
data class TextBody(
        override var secrecy: Int,//0是普通消息 1是无痕消息
        var text: String = ""//消息内容
) : MsgBody, SecrecyBody {
    override val mtype: Int = MessageType.TEXT.value
}

/**
 * 表情消息类型
 */
@Parcelize
data class EmojiBody(
        override var secrecy: Int,//0是普通消息 1是无痕消息
        val key: String = "",//通过此获取表情图片
        val text: String = ""//表情的文本显示
) : MsgBody, SecrecyBody {
    override val mtype: Int = MessageType.EMOT.value
}

/**
 *@消息
 */
@Parcelize
data class AtBody(
        var id: Long = 0,//@人员id  0为全部
        var name: String = ""
) : MsgBody {
    override val mtype: Int = MessageType.AT.value
}

/**
 * 附件文件消息类型
 */
@Parcelize
data class AttachBody(
        override var token: String = "",//文件token
        override var bytes: Long = 0,//文件大小
        override var md5: String = "",//文件md5
        override var name: String = "",//文件名字
        override var localPath: String = ""//文件本地路径
) : MsgBody, FileBody {
    override val mtype: Int = MessageType.ANNEX.value
}

/**
 * 图片消息类型
 */
@Parcelize
data class ImgBody(
        override var secrecy: Int,//0是普通消息 1是无痕消息
        var sizew: Int = 0,//图片宽
        var sizeh: Int = 0,//图片高
        override var token: String = "",//图片token
        override var bytes: Long = 0,//图片大小
        override var md5: String = "",//图片md5
        override var name: String = "",//图片名字
        override var localPath: String = ""//本地路径
) : MsgBody, FileBody, SecrecyBody {
    override val mtype: Int = MessageType.IMAGE.value
}

/**
 * 位置消息类型
 */
@Parcelize
data class LocBody(
        var address: String? = "",
        var buildingName: String? = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
) : MsgBody {
    override val mtype: Int = MessageType.LOCATION.value
}

/**
 * 视频消息类型
 */
@Parcelize
data class VideoBody(
        var t: Int = 0,//视频时长
        var sizew: Int = 0,//视频宽
        var sizeh: Int = 0,//视频高
        var localThumbnail: String? = null,//视频缩略图本地位置
        override var token: String = "",//视频文件token
        override var bytes: Long = 0,//视频文件大小
        override var md5: String = "",//视频文件md5
        override var name: String = "",//视频文件名称
        override var localPath: String = ""//视频文件本地路径
) : MsgBody, FileBody {
    override val mtype: Int = MessageType.VIDEO.value
}

/**
 * 语音消息文件
 */
@Parcelize
data class AudioBody(
        var t: Int = 0,//语音时长
        var isPlay: Boolean = true,//是否已播放
        var showTextview: Boolean = false,//是否展示文本
        var voiceToText: String = "",//语音转文本
        override var token: String = "",//语音文件token
        override var bytes: Long = 0,//语音文件大小
        override var md5: String = "",//语音文件md5
        override var name: String = "",//语音文件名字
        override var localPath: String = ""//语音文件本地路径
) : MsgBody, FileBody {
    override val mtype: Int = MessageType.VOICE.value
}

@Parcelize
data class RefsHeadBody(
        var mid: String = "",
        var fromid: Long = 0,
        var text: String = ""
) : MsgBody {
    override val mtype: Int = MessageType.REPLY_BEGIN.value
}

@Parcelize
data class RefsEndBody(
        var id: Long = 0,
        var text: String = ""
) : MsgBody {
    override val mtype: Int = MessageType.REPLY_END.value
}

@Parcelize
data class NewReplyBody(
        var mid: String = "",//被回复消息id
        var mreplytype: Int,//被回复消息类型
        var fromid: Long = 0,//被回复消息发送人id
        var text: String? = "",//被回复消息内容
        @TypeConverters(MessageListConverter::class)
        var quotemsgs: @RawValue Any? = null
) : MsgBody {
    override val mtype: Int = MessageType.NEW_REPLY.value
}

@Parcelize
data class MergeForwardBody(
        var sid: String = "",
        var title: String = "",
        var key: String = "",
        var briefs: MutableList<String>? = null,
        var mids: MutableList<String>? = null,
        var items: @RawValue Any? = null,
        override var token: String = "",
        override var bytes: Long = 0,
        override var md5: String = "",
        override var name: String = "",
        override var localPath: String = ""
) : MsgBody, FileBody {
    override val mtype: Int = MessageType.FORWARD.value
}

@Parcelize
data class HybridMsgBody(
        var articles: ArrayList<Article>? = null//文章集合,
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_ARTICLES.value
}

@Parcelize
data class Article(
        var subject: String = "",//标题
        var thumb: String = "",//     图文消息图片
        var author: String = "",//作者
        var digest: String = "",//图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
        var appurl: String = "",//移动端打开地址
        var pcutl: String = "",//n     桌面端打开地址
        var sort: Int = 0//排序
) : Parcelable

@Parcelize
data class EmailBody(
        var id: String = "",//     string n     邮件的 ID
        var importance: Int = 0,// int     n   优先级(0:低 1:正常 2:高)
        var subject: String = "",// n     标题
        var digest: String = "",//     摘要
        var appurl: String = "",//     移动端打开地址
        var pcutl: String = "" //桌面端打开地址
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_EMAIL.value
}

@Parcelize
data class WorkFlowBody(
        var flowid: String = "",//原始 ID
        var type: Int = 0,//类型(0:无效 1:待办 2:已办)
        var name: String = "",//类型名称
        var subject: String = "",//标题
        var digest: String = "",//摘要
        var creator: String = "", //创建者 IM 账号
        var appurl: String = "",//移动端打开地址
        var pcurl: String = "" //  桌面端打开地址
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_FLOW.value
}

@Parcelize
data class WorkScheduleBody(
        var id: String = "",//日程 ID
        var subject: String = "",//标题
        var digest: String = "",//  摘要
        var start_time: Int = 0,//开始时间 UTC
        var end_time: Int = 0,//结束时间 UTC
        var duration: Int = 0,// 时长(分钟)
        var state: Int = 0,// 状态(0:无效 1:有效)
        var update_time: Int = 0,//最后修改时间 UTC
        var create_time: Int = 0,//创建时间 UTC
        var appurl: String = "",//移动端打开地址
        var pcurl: String = ""//桌面端打开地址,
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_SCHEDULE.value
}

@Parcelize
data class WorkTaskBody(
        var id: String = "", //任务 ID
        var importance: Int = 0,// 优先级(0:低 1:正常 2:高)
        var subject: String = "",// 标题
        var digest: String = "",//摘要
        var start_time: Long = 0, //开始时间 UTC
        var end_time: Long = 0,// 结束时间 UTC
        var reminder_time: Long = 0,// 提醒时间 UTC
        var actual_work: Int = 0, //实际时长(分钟)
        var total_work: Int = 0, //总时长(分钟)
        var percent: Int = 0,//完成百分比[0~100]
        var state: Int = 0,//状态(0:无效 1:有效)
        var update_time: Long = 0,//最后修改时间 UTC
        var create_time: Long = 0,//创建时间 UTC
        var appurl: String = "", //移动端打开地址
        var pcurl: String = ""//桌面端打开地址,
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_TASK.value
}

@Parcelize
data class CloudFileBody(
        // 云盘文件
        var token: String? = null,// 云盘文件的 token
        var bytes: Long = 0,//云盘文件的字节大小[filesize]
        var md5: String,// n     云盘文件的 md5 信息[filehash]
        var name: String,// n     云盘文件名字[filename]，如:工作报告.doc
        var mount_id: Long = 0,//     n   云盘文件目录 ID[mount_id]
        var link: String,// n     云盘链接[filelink]
        var localpath: String? = null,// y     【本地属性】本地文件位置
        var save2cloud: Boolean? = null,//     【本地属性】是否已经保存到了云盘
        var status: Int = 0// 【本地属性】传输进度[0~100]，默认为 0

) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_CLOUD_FILE.value
}

/**
 * 通知类型消息
 */
@Parcelize
data class NoticeBody(
        var type: Int, //类型
        var key: String? = null,//   原始标识符
        var subject: String,// n     标题
        var context: String,// n     内容
        var subject2: String? = null,// 二级标题
        var digest: String,//     摘要
        var logo: String? = null,//图标 URL
        var appurl: String? = null,//移动端打开 URL
        var pcurl: String? = null,//     桌面端打开 URL
        var author: String? = null,//     作者
        var principal: String? = null,//表示获取的规则
        var taskStatus: Int,//表示待办通知状态（0待办  1已办）
        var isRead: Int//表示待办通知状态（0已读 1未读）
) : MsgBody {
    override val mtype: Int = MessageType.NOTIFICATION.value
}

/**
 * 消息通知类型消息
 */
@Parcelize
data class MeetingNotification(
        var headline: @RawValue MeetingNotificationTitle? = null,//标题
        var body: @RawValue List<MeetingNotificationBody>? = null,//内容
        val appUrl: String,//手机客户端打开的url
        val pcUrl: String//pc打开的url
) : MsgBody {
    override val mtype: Int = MessageType.MEETING_NOTIFICATION.value
}

/**
 * 消息通知内容颜色显示
 */
data class MeetingNotificationTitle(
        val key: String,//主标题
        val value: String,//副标题
        val keyRGB: String,//主标题颜色
        val valueRGB: String//副标题颜色
)

data class MeetingNotificationBody(
        val key: String,
        val value: String
)

@Parcelize
data class UpgradeBody(
        val ver: String,
        val url: String? = null,
        val upgrade: Int,
        val info: String? = null
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_UPGRADE.value
}

@Parcelize
data class MettingBody(
        val t: Long,
        val title: String,
        val creater: String,
        val starturl: String,
        val pcjoinurl: String,
        val mobjoinurl: String
) : MsgBody {
    override val mtype: Int = MessageType.JOIN_MEETING.value
}

@Parcelize
data class AppRemindBody(
        val id: Long,
        val total: Long
) : MsgBody {
    override val mtype: Int = MessageType.WEBAPP_REMIND.value
}

/**
 * 自动回复类型消息
 */
@Parcelize
data class AutoReplyBody(
        val text: String
) : MsgBody {
    override val mtype: Int = MessageType.AUTO_REPLY.value
}

/**
 * 群通知类型消息
 */
@Parcelize
data class EditSessionBody(
        val flag: Long,//更新标识，参考 ModifySession*
        val type: Int,//会话类型(类型转换时)
        val title: String,//会话标题(修改标题时)
        val cid: Long,//创建者 ID(群转让时)
        val admins: List<Long>? = null,//管理员数组(如果有修改，必须带上所有的管理员;讨论组 无该功能)
        val adds: List<Long>? = null,//添加的成员(添加成员时，必须是不存在的成员)
        val dels: List<Long>? = null//删除的成员(删除成员时，必须是已经存在的成员)
) : MsgBody {
    override val mtype: Int = MessageType.SESSION_CHANGE.value
    @IgnoredOnParcel
    var extraMetaData: MutableMap<String, Any>? = null
}

/**
 * 撤回类型消息
 */
@Parcelize
data class RecallBody(
        val mid: String,//被撤回的消息 ID
        val t: Long//被撤回消息的发送时间
) : MsgBody {
    override val mtype: Int = MessageType.RECALL.value
}

/**
 * 语音视频通话类型消息
 */
@Parcelize
data class VoiceChatBody(
        var createrid: Long = 0,//创建者
        var inviterid: Long = 0,//邀请人id
        var roomid: Int = 0,
        var audiotype: Int = 0, //(0:发起；1：取消；2：拒绝；3：超时；4：结束；5中间加入)
        var trtctype: Int = 0,//0 语音通话  1视频通话
        val toids: MutableList<Long>? = null,//被邀请人ID
        val addids: MutableList<Long>? = null//中途添加的人
) : MsgBody {
    override val mtype: Int = MessageType.VOICECHAT.value
}

/**
 * 视频会议类型消息
 */
@Parcelize
data class VideoConferenceBody(
        var confid: String,//会议id
        var confname: String,//会议名称
        var confparties: Int,//会议方数
        var createrid: String = "",//创建者
        var duration: Int,//会议时长
        var hoststarturl: String,//主持人入会连接
        var joinurl: String,//参会人入会连接
        var protocolhoststarturl: String,//主持 人协议入会连接 用于 SDK
        var protocoljoinurl: String,//协议入会连接适用于 SDK 入会
        var confnumber: String,//视频会议号
        val toids: MutableList<Long>? = null,//被邀请人ID
        var confstarttime: Long? = 0L
) : MsgBody {
    override val mtype: Int = MessageType.VIDEOCONFERENCE.value
}

/**
 * 回执消息
 */
@Parcelize
data class ReceiptBody(
        val mid: String,//原要求回执的消息 ID
        val mode: Int = 0//0: 回执(其它保留)
) : MsgBody {
    override val mtype: Int = MessageType.RECEIPT.value
}

/**
 * 投票消息
 */
@Parcelize
data class VoteBody(
        val createuserid: Long,//投票创建者id
        val voteid: Long,//投票id
        val title: String,//投票标题
        val invalidtime: Long,//投票结束时间
        val options: MutableList<String>//投票选项
) : MsgBody {
    override val mtype: Int = MessageType.VOTE.value
}

/**
 * 接龙消息
 */
@Parcelize
data class SolitaireBody(
        var title: String,//接龙标题
        var example: String,//接龙例子
        val chainsId: String,//接龙id
        var itemList: MutableList<String>//接龙内容
) : MsgBody {
    override val mtype: Int = MessageType.SOLITAIRE.value
}

/**
 * 公众号消息(图文消息)
 */
@Parcelize
data class GraphicBody(
        var isRead: Boolean = true,//是否已读
        var graphicId: Long,//公众号消息id
        var title: String,//消息标题
        var context: String,//消息内容 html代码
        var subscription: @RawValue Subscription,//公众号信息
        var enclosure: @RawValue Any //附件内容
) : MsgBody {
    override val mtype: Int = MessageType.GRAPHIC.value
}

//公众号附件信息
@Parcelize
data class EnclosureData(
        var fileName: String,
        var url: String = "",
        var bytes: Long = 0,
        var token: String = ""
) : Parcelable {
}

enum class MsgType(val value: Int) {
    SINGLE_CHAT(0),
    GROUP_FIXED(30),
    GROUP_DEPARTMENT(31),
    GROUP_NORMAL(32),
    GROUP_TEMP(33),
    NOTICE_GLOABLE(50)
}

