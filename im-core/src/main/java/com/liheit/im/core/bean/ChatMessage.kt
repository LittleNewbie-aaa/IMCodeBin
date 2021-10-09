package com.liheit.im.core.bean

import android.arch.persistence.room.*
import android.media.MediaMetadataRetriever
import android.os.Parcelable
import android.support.annotation.IntDef
import com.liheit.im.common.ext.AppNameFlag
import com.liheit.im.common.ext.getAppName
import com.liheit.im.core.Constants
import com.liheit.im.core.IMClient
import com.liheit.im.core.MessageBuilder
import com.liheit.im.core.bean.ChatMessage.Companion.FLAG_READ
import com.liheit.im.core.bean.ChatMessage.Companion.FLAG_UNREAD
import com.liheit.im.core.protocol.*
import com.liheit.im.utils.*
import com.liheit.im.utils.json.MessageListConverter
import com.liheit.im.utils.json.gson
import kotlinx.android.parcel.Parcelize
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 *  聊天消息表
 */
@Parcelize
@Entity(indices = [Index("mid"), Index("sid"), Index("t")])
data class ChatMessage(
        @PrimaryKey var mid: String = "",// string n 消息 ID，标识一条消息的 GUID
        @ColumnInfo var t: Long = 0,//int n 消息发送时间 UTC（需要与服务器同步校准［用心跳校准］）
        @ColumnInfo var sid: String = "", // string n 会话 ID，标识一个会话的 GUID
        @ColumnInfo var type: Int = 0,// int n 会话类型，参考后面的会话类型定义描述
        @ColumnInfo var name: String = "",// string n 发送者名字，消息头显示
        @ColumnInfo var from: String = "",// string n 发送者账号
        @ColumnInfo var utime: Long = 0,//session 更新时间
        @ColumnInfo var fromid: Long = 0,// int n 发送者 ID
        @ColumnInfo var toid: Long = 0,// int y 单聊的接收者 ID；其它为无
        @ColumnInfo var flag: Int = 0,// int y 标识，用于功能扩展，参考 Msg***Flag
        @ColumnInfo var sendStatus: Int = SEND_STATUS_SENDING,//自定义属性，消息发送状态  0 发送中  1 发送成功  2 发送失败
        @ColumnInfo var content: String = "",//消息内容
        @ColumnInfo var bodyType: Int = 0,//消息内容类型
        @ColumnInfo var recallBy: Long = -1,
        @Transient var unReceiptCount: Int = 0, //未回执的数目
        @Transient var receiptCount: Int = 0,  //已回执的数目
        @Transient var isChecked: Boolean = false,//多选消息是是否已选中
        @Transient var recallUser: String = "",
        @TypeConverters(MessageListConverter::class)
        @ColumnInfo var msgs: ArrayList<MsgBody>? = null// Msg[] n 消息内容，具体结构参考 Msg
) : Parcelable {

    @Ignore
    var messageStatusCallback: IMClient.IMCallBack? = null

    fun toReq(): SendMsgReq {
        return SendMsgReq(
                t = t,
                mid = mid,
                sid = sid,
                type = type,
                from = from,
                name = name,
                fromid = fromid,
                toid = toid,
                flag = flag,
                msgs = msgs?.map {
                    if (it is FileBody) it.localPath = ""
                    return@map it
                }?.toMutableList()
        )
    }

    /**
     *  消息是否需要回执
     */
    fun isNeedReceipt(): Boolean {
        return (flag and FLAG_RECEIPT shr 1) == 1
    }

    /**
     *  消息是否已经回执
     */
    fun isReceipted(): Boolean {
        return (flag and FLAG_RECEIPTED shr 2) == 1
    }

    /**
     *  消息是否已全部回执
     */
    fun isAllReceipted(): Boolean {
        return (flag and FLAG_RECEIPT_ALL_READ shr 3) == 1
    }

    /**
     *  消息是否是撤回的消息
     */
    fun isRecall(): Boolean {
        return (flag and FLAG_RECALL shr 4) == 1
    }

    /**
     *  是否是撤回的消息
     */
    fun isRecallMessage(): Boolean {
        return (flag and FLAG_RECALLED shr 9) == 1
    }

    /**
     *  是否是发送给回执消息发送者的回执消息
     */
    fun isReceiptToMessage(): Boolean {
        return (flag and FLAG_RECEIPT_TO shr 8) == 1
    }

    /**
     *  添加标识
     */
    fun addFlag(f: Int) {
        flag = flag or f
    }

    //设置消息是否需要回执
    fun needReceipt(needReceipt: Boolean) {
        if (needReceipt) {
            this.flag = flag or FLAG_RECEIPT
        } else {
            this.flag = flag and (flag xor FLAG_RECEIPT)
        }
    }

    /**
     *  获取消息中的第一条消息body
     */
    fun getMessageBody(): MsgBody? {
        return msgs?.get(0)
    }

    /**
     *  消息预处理
     */
    fun preProcess() {
        content = getMsgShowText(this, true)
        bodyType = getMsgBodyType(this)
    }

    companion object {
        const val MASK_READ = 0x00000001  // Bit0: 0-未读；1-已读 掩码
        const val FLAG_READ = 0b1  // Bit0: 0-未读；1-已读
        const val FLAG_UNREAD = 0b0  // Bit0: 0-未
        const val FLAG_RECEIPT = 0x00000002 // Bit1: 0-不需要回执；1-需要回执
        const val FLAG_RECEIPTED = 0x00000004 // Bit2: 0-还未回执；1-已经回执(只有当为MsgReceiptFlag时才有用)
        const val FLAG_RECEIPT_ALL_READ = 0x00000008 // bit3: 0-没有全部已读；1-该回执消息全部已读
        const val FLAG_RECALL = 0x00000010// Bit4: 0-没有撤回的消息；1-已经撤回的消息
        const val FLAG_REPLY = 0x00000020 // Bit5: 0-不是消息引用；1-是消息引用
        const val FLAG_FORWARD = 0x00000040 // Bit6: 0-不是合并转发的消息；1-是合并转发的消息
        const val FLAG_AUTO_RESPONE = 0x00000080 // Bit7: 0-不是自动回复的消息；1-是自动回复的消息
        const val FLAG_RECEIPT_TO = 0x00000100 // Bit8: 1-是发的消息回执（回复给回执发起者），发送者设置
        const val FLAG_RECALLED = 0x00000200 // Bit9: 1-是撤回消息，发送者设置
        const val FLAG_SESSION = 0x00000400 // Bit10: 1-是会话更新消息，发送者设置

        const val SEND_STATUS_SENDING = 0//消息发送中
        const val SEND_STATUS_SUCCESS = 1//消息发送成功
        const val SEND_STATUS_ERROR = 2//消息发送失败

        /**
         *  获取视频或语音的长度
         */
        fun getTime(filePath: String?): Int {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(filePath)
            val time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    .toInt() / 1000
            return Math.max(time, 1)
        }

        //获取到视频的宽高信息并且存放到数组里
        fun getWidthAndHeight(filePath: String?): IntArray {
            var retr = MediaMetadataRetriever()
            retr.setDataSource(filePath)
            var width =
                    retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            var height =
                    retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            var route = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (route == "90" || route == "270") {
                return intArrayOf(height, width)
            }
            return intArrayOf(width, height)
        }

        /**
         * 创建文本类型消息
         */
        fun createTextMsg(
                sid: String,
                content: String,
                chatType: Int,
                refsMid: String? = null,
                isTracelessMsg: Int
        ): ChatMessage {
            if (content.isNullOrEmpty()) {
                throw RuntimeException("消息不能为空")
            }
            if (content.length > Constants.MsgMax) {
                throw RuntimeException("消息长度不能超过${Constants.MsgMax}")
            }

            val msg = createMessage(sid, chatType, refsMid)

            var msgs = msg.msgs!!
            Log.e("aaa content=${content} ${content.length}")
            val regex = "(?:@[\u4e00-\u9fa5_a-zA-Z0-9]{1,}\\s|\\[(.[^\\[]*)\\])"

            val matcher = Pattern.compile(regex).matcher(content)
            var processStart = 0
            while (matcher.find()) {
                if (matcher.start() != processStart) {
                    val item = content.substring(processStart, matcher.start())

                    msgs.add(TextBody(text = item, secrecy = isTracelessMsg))
                }

                val matchText = matcher.group()
                if (matchText.startsWith("@")) {
                    val pNumber = Pattern.compile("[0-9]*")
                    if (pNumber.matcher(matchText.substring(1).trim()).matches()) {
                        var id = matchText.substring(1).trim().toLong()
                        var user = IMClient.userManager.getUserById(id)
                        var userName = if (id == 0L) "@全体成员" else "@${user?.cname}"
                        msgs.add(AtBody(id = id, name = userName))
                    } else {
                        msgs.add(TextBody(text = matchText, secrecy = isTracelessMsg))
                    }
                } else {
                    var key = matcher.group(1)
                    var desc = Constants.emojiDescs.get(key)?.let { "[${it}]" } ?: ""
                    if (desc.isNullOrEmpty()) {
                        msgs.add(TextBody(text = content, secrecy = isTracelessMsg))
                    } else {
                        msgs.add(EmojiBody(key = key, text = desc, secrecy = isTracelessMsg))
                    }
                }

                processStart = matcher.end()
                Log.e("aaa processStart=$processStart  content.length=${content.length}")
            }
            if (processStart == 0 || processStart <= content.length - 1) {
                msgs.add(TextBody(text = content.substring(processStart), secrecy = isTracelessMsg))
            }
            msg.preProcess()
            return msg
        }

        /**
         *创建图片类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createImgMsg(sid: String, filePath: String, chatType: Int, isTracelessMsg: Int): ChatMessage {
            var file = File(filePath)
            return createMessage(sid, chatType, null)
                    .apply {
                        val widthAndHeight = BitmapUtil.getImageWidthHeight(filePath)
                        val body = ImgBody(localPath = filePath, name = file.name, secrecy = isTracelessMsg,
                                sizew = widthAndHeight[0], sizeh = widthAndHeight[1])
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建视频类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createVideoMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            var file = File(filePath)
            return createMessage(sid, chatType, null)
                    .apply {
                        val time = getTime(filePath)
                        val widthAndHeight = getWidthAndHeight(filePath)
                        val body = VideoBody(localPath = filePath, name = file.name, t = time,
                                sizew = widthAndHeight[0], sizeh = widthAndHeight[1])
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建群通知类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createLocalNoticeMsg(
                sid: String, chatType: Int, ct: Long,
                flag: Long, type: Int, title: String, cid: Long
        ): ChatMessage {
            return createMessage(sid, chatType, null, ct)
                    .apply {
                        val body = EditSessionBody(flag = flag, type = type, title = title, cid = cid)
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建语音类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createVoiceMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            var file = File(filePath)
            return createMessage(sid, chatType, null)
                    .apply {
                        val time = getTime(filePath)
                        val body = AudioBody(localPath = filePath, name = file.name, t = time)
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         * 创建文件类型的消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createFileMsg(sid: String, filePath: String, chatType: Int): ChatMessage {
            var file = File(filePath)
            return createMessage(sid, chatType, null)
                    .apply {
                        val body = AttachBody(localPath = filePath, name = file.name)
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建语音聊天类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createVoiceChatMsg(
                sid: String,
                chatType: Int,
                roomId: Int,
                audioType: Int,
                toIDs: MutableList<Long>,
                addids: MutableList<Long>? = null,
                createid: Long,
                trtctype: Int,
                inviterid: Long
        ): ChatMessage {
            return createMessage(sid, chatType, null)
                    .apply {
                        val body = VoiceChatBody(createrid = createid, inviterid = inviterid,
                                roomid = roomId, audiotype = audioType,
                                toids = toIDs, addids = addids, trtctype = trtctype)
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建视频会议类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createVideoConferenceMsg(
                sid: String, chatType: Int,
                createrid: String, protocoljoinurl: String,
                protocolhoststarturl: String, hoststarturl: String,
                joinurl: String, confparties: Int, duration: Int,
                confNumber: String, confid: String, confname: String,
                toIDs: MutableList<Long>, confstarttime: Long
        ): ChatMessage {
            return createMessage(sid, chatType, null)
                    .apply {
                        val body = VideoConferenceBody(
                                createrid = createrid,
                                protocoljoinurl = protocoljoinurl,
                                protocolhoststarturl = protocolhoststarturl,
                                hoststarturl = hoststarturl, joinurl = joinurl,
                                confparties = confparties, duration = duration,
                                confnumber = confNumber, toids = toIDs, confid = confid,
                                confname = confname, confstarttime = confstarttime
                        )
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         * 创建投票类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createVoteChatMsg(
                sid: String, chatType: Int, createuserid: Long, voteid: Long, title: String,
                invalidtime: Long, options: MutableList<String>
        ): ChatMessage {
            return createMessage(sid, chatType, null)
                    .apply {
                        val body = VoteBody(
                                createuserid = createuserid, voteid = voteid, title = title,
                                invalidtime = invalidtime, options = options
                        )
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建接龙类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createSolitaireChatMsg(
                sid: String, chatType: Int, title: String,//接龙标题
                example: String,//接龙例子
                chainsId: String,//接龙id
                itemList: MutableList<String>
        ): ChatMessage {
            return createMessage(sid, chatType, null)
                    .apply {
                        val body = SolitaireBody(
                                title = title, example = example,
                                chainsId = chainsId, itemList = itemList
                        )
                        this.bodyType = body.mtype
                        msgs?.add(body)
                    }
        }

        /**
         *创建位置类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createLocationMsg(
                sid: String,
                address: String,
                buildingName: String,
                latitude: Double,
                longitude: Double,
                chatType: Int
        ): ChatMessage {
            val locationBody = LocBody().apply {
                this.address = address
                this.buildingName = buildingName
                this.latitude = latitude
                this.longitude = longitude
            }

            return createMessage(sid, chatType).apply {
                msgs?.add(locationBody)
                this.bodyType = MessageType.LOCATION.value
            }
        }

        /**
         *创建转发消息
         */
        @Deprecated("replace by MessageBuilder")
        fun forwardMsg(msg: ChatMessage, sid: String, type: Int): ChatMessage {
            var senderId = IMClient.getCurrentUserId()
            var senderName = IMClient.getCurrentUser()?.cname ?: ""
            var senderAccount = IMClient.getCurrentUserAccount() ?: ""
            var msgFlag = 0
            var chatType = type
            var msgs = msg.msgs

            var toId = 0L
            if (chatType == com.liheit.im.core.bean.SessionType.SESSION_P2P.value) {
                toId = IDUtil.parseTargetId(IMClient.getCurrentUserId(), sid)
            }
            var m: ChatMessage
            if (msg.getMessageBody()?.mtype == MessageType.SOLITAIRE.value) {
                var body = msg.getMessageBody() as SolitaireBody
                var content = StringBuilder()
                if (!body.title.isNullOrEmpty()) {
                    content.append("${body.title}\n")
                }
                if (!body.example.isNullOrEmpty()) {
                    content.append("例 ${body.example}\n")
                }
                content.append(
                        body.itemList.mapIndexed { index, s ->
                            "${index + 1}.$s"
                        }.joinToString("\n")
                )
                m = MessageBuilder.createTextMsg(sid, content.toString(), chatType)
            } else if (msg.getMessageBody()?.mtype == MessageType.VOTE.value) {
                m = MessageBuilder.createTextMsg(sid, "[发起投票]", chatType)
            } else if (msg.getMessageBody()?.mtype == MessageType.NEW_REPLY.value) {
                m = MessageBuilder.createTextMsg(sid, showReplyText(msg.msgs), chatType)
            } else {
                m = msg.copy(mid = IDUtil.generatorMsgId(),
                        t = TimeUtils.getServerTime(), sid = sid, name = senderName,
                        from = senderAccount, fromid = senderId, toid = toId, flag = msgFlag,
                        sendStatus = SEND_STATUS_SENDING, type = chatType, msgs = msgs)
            }
            return m
        }

        /**
         *创建聊天记录类型消息
         */
        @Deprecated("replace by MessageBuilder")
        fun createMergeForwardMessage(conversation: Conversation, msgIds: MutableList<String>, msgtitle: String): ChatMessage {
            var chatType = conversation.type
            var titleStr = msgtitle
            val msg = createMessage(conversation.sid, chatType, null)

            var itemData = mutableListOf<ChatMessage>()
            var disposeMsgIds = mutableListOf<String>()
            msgIds.forEach {
                var m = IMClient.chatManager.getMessageById(it)
                if (m != null && !m.isRecall() && !m.isRecallMessage()) {
                    itemData.add(m)
                    disposeMsgIds.add(it)
                }
            }

            var brief = mutableListOf<String>()
            for (s in disposeMsgIds.iterator()) {
                var m = IMClient.chatManager.getMessageById(s)
                if (m != null) {
                    val content = "${m.name}:${MessageFormatUtils.simpleFormat(m, 1)}"
                    if (content.length <= 120) {
                        brief.add(content)
                    } else {
                        brief.add(content.substring(0, 120))
                    }
                    if (brief.size >= 3) {
                        break
                    }
                }
            }

            msg?.let { m ->
                val body = MergeForwardBody().apply {
                    sid = m.sid
                    briefs = brief
                    title = titleStr
                    mids = disposeMsgIds
                    items = itemData
                }
                m.msgs?.add(body)
            }
            return msg
        }

        private fun createMessage(
                sid: String,
                chatType: Int,
                refsMid: String? = null,
                ct: Long? = null
        ): ChatMessage {
            var senderId = IMClient.getCurrentUserId()
            var senderName = IMClient.getCurrentUser()?.cname ?: ""
            var senderAccount = IMClient.getCurrentUserAccount() ?: ""
            var msgFlag = 0

            var toId = if (chatType == SessionType.SESSION_P2P.value || chatType == SessionType.FILE_HELP.value) {
                IDUtil.parseTargetId(IMClient.getCurrentUserId(), sid)
            } else {
                0L
            }

            var msgs = arrayListOf<MsgBody>()
            if (refsMid != null) {
                var refsMsg = IMClient.chatManager.getMessageById(refsMid)
                if (getAppName() == AppNameFlag.THE_SD_FLAG.value) {
                    //之前回复消息数据格式（实地使用）
                    if (refsMsg != null) {
                        val head = RefsHeadBody().apply {
                            fromid = refsMsg.fromid
                            mid = refsMsg.mid
                            text = "「${refsMsg.name}:"
                        }
                        val head2 = RefsHeadBody().apply {
                            fromid = refsMsg.fromid
                            mid = refsMsg.mid
                            text = "」"
                        }

                        var end = RefsEndBody().apply {
                            id = refsMsg.fromid
                            text = "\n-----------------------------\n"
                        }
                        msgs.add(head)
                        refsMsg.msgs?.forEach { m ->
                            if (m is AtBody) {
                                msgs.add(TextBody(secrecy = 0, text = m.name))
                            } else {
                                msgs.add(m)
                            }
                        }
                        msgs.add(head2)
                        msgs.add(end)
                    }
                } else {
                    if (refsMsg != null) {
                        refsMsg.msgs?.let {
                            val replyBody = NewReplyBody(
                                    mid = refsMsg.mid,
                                    mreplytype = it[0].mtype,
                                    fromid = refsMsg.fromid,
                                    text = showReplyText(refsMsg.msgs),
                                    quotemsgs = refsMsg.msgs
                            )
                            Log.e("aaa replyBody=${gson.toJson(replyBody)}")
                            msgs.add(replyBody)
                        }
                    }
                }
            }
            var t: Long = ct ?: TimeUtils.getServerTime()
            return ChatMessage(
                    mid = IDUtil.generatorMsgId(),
                    t = t,
                    //TODO sessionId 需要先判断消息是发送给谁的，然后根据规则生成
                    sid = sid,
                    //TODO 这里也是
                    type = chatType,
                    name = senderName,
                    from = senderAccount,
                    fromid = senderId,
                    toid = toId,
                    flag = msgFlag,
                    sendStatus = SEND_STATUS_SENDING,
                    msgs = msgs
            )
        }

        /**
         *  获取消息内容类型
         */
        fun getMsgBodyType(msg: ChatMessage): Int {
            if (msg.msgs == null) return MessageType.TEXT.value
            val type = msg.msgs?.getOrNull(0)?.mtype
            return when (type) {
                null,
                MessageType.TEXT.value,
                MessageType.AT.value,
                MessageType.EMOT.value
                -> MessageType.TEXT.value
                else -> type
            }
        }

        /**
         *  获取消息文字内容
         */
        fun getMsgShowText(msg: ChatMessage, isSearch: Boolean? = false, isDisposeRecall: Boolean? = true): String {
            var msgList = msg.msgs?.toMutableList()
            if (msg.msgs!![0].mtype == MessageType.NEW_REPLY.value) {
                msgList?.removeAt(0)
            }
            return msgList?.map {
                if (isDisposeRecall == true && (msg.isRecall() || msg.isRecallMessage())) {
                    return ""
                }
                return@map when (it) {
                    is AtBody -> it.name
                    is TextBody -> {
                        val body = it as SecrecyBody
                        if (body.secrecy == 1 &&
                                (msg.getMessageBody()?.mtype == MessageType.TEXT.value ||
                                        msg.getMessageBody()?.mtype == MessageType.EMOT.value)
                        ) {
                            ""
                        } else {
                            it.text
                        }
                    }
                    is EmojiBody -> {
                        if (isSearch == true) {
                            ""
                        } else {
                            val body = it as SecrecyBody
                            if (body.secrecy == 1 &&
                                    (msg.getMessageBody()?.mtype == MessageType.TEXT.value ||
                                            msg.getMessageBody()?.mtype == MessageType.EMOT.value)
                            ) {
                                ""
                            } else {
                                "[${it.key}]"
                            }
                        }
                    }
                    is RefsHeadBody -> it.text
                    is RefsEndBody -> it.text
                    is AutoReplyBody -> it.text
                    is FileBody -> {
                        if (isSearch == true) {
                            if (msg.getMessageBody()?.mtype != MessageType.VOICE.value &&
                                    msg.getMessageBody()?.mtype != MessageType.FORWARD.value) {
                                it.name
                            } else {
                                ""
                            }
                        } else {
                            it.name
                        }
                    }
                    is GraphicBody -> {
                        if (isSearch == true) {
                            it.title
                            val document: Document = Jsoup.parse(it.context, "utf-8")
                            val text = document.getElementById("top").getElementsByTag("div")[1].text()
                            it.title + "\n" + text
                        } else {
                            it.title
                        }
                    }
                    else -> ""
                }
            }?.joinToString("") ?: ""
        }

        //获取被回复消息的展示文字
        fun showReplyText(msgList: MutableList<MsgBody>? = mutableListOf()): String {
//            var msgList = msg.msgs?.toMutableList()
//            if (msgList?.get(0)?.mtype == MessageType.NEW_REPLY.value) {
//                msgList?.removeAt(0)
//            }
            return msgList?.map {
                return@map when (it) {
                    is TextBody -> it.text
                    is EmojiBody -> "[${it.key}]"
                    is RefsHeadBody -> it.text
                    is RefsEndBody -> it.text
                    is AtBody -> it.name
                    is AutoReplyBody -> it.text
                    is ImgBody -> "[图片]"
                    is VideoBody -> "[视频]"
                    is AttachBody -> "[文件]${it.name}"
                    is LocBody -> "[位置]${it.buildingName}"
                    else -> ""
                }
            }?.joinToString("") ?: ""
        }
    }
}

@IntDef(
        value = [
            FLAG_READ,
            FLAG_UNREAD]
)
@Retention(AnnotationRetention.SOURCE)
annotation class MsgFlag

enum class MessageType(val value: Int) {
    //标准消息子类型
    UN_SUPPORT(-1),
    TEXT(0),//文本消息
    EMOT(1),//标准表情消息，IM内置
    EMOT_USER(2),//用户表情消息，以图片方式发送
    ANNEX(3),//附件消息
    IMAGE(4),//图片消息
    VOICE(5),//语音消息
    VIDEO(6),//视频消息
    LOCATION(7),//位置
    AT(8),//@membaer 消息
    FORWARD(9),//消息合并转发
    REPLY_BEGIN(10),//消息引用开始标识
    REPLY_END(11),//消息引用结束标识
    JOIN_MEETING(12), //会议入会提醒
    AUTO_REPLY(13),//自动回复消息（单聊会话）
    NEW_REPLY(14),//新的回复消息
    GRAPHIC(15),//公众号消息(图文消息)

    VOICECHAT(150),//语音聊天
    VIDEOCONFERENCE(151),//视频会议
    VOTE(152),//投票消息
    SOLITAIRE(153),//接龙消息

    //Webapp消息子类型
    NOTIFICATION(90),  // 通知
    MEETING_NOTIFICATION(91),  // 会议通知
    WEBAPP_UPGRADE(99),  // 升级提示
    WEBAPP_REMIND(100),//应用提醒
    WEBAPP_ARTICLES(101),//图文消息
    WEBAPP_EMAIL(102),//邮件
    WEBAPP_FLOW(103),//流程
    WEBAPP_SCHEDULE(104),//日程
    WEBAPP_TASK(105),//任务
    WEBAPP_CLOUD_FILE(106), // 云盘文件

    // 特殊功能消息子类型
    SESSION_CHANGE(1000), // 群内的各种消息提示，如添加成员 删除成员，退群等
    RECALL(1001), // 消息撤回
    RECEIPT(1002), // 消息回执（后台需要只转发给接收者）
}

fun Int.toMessageType(): MessageType {
    return MessageType.values().find { it.value == this } ?: MessageType.UN_SUPPORT
}