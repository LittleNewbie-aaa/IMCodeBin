package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import android.os.Parcelable
import com.liheit.im.core.IMClient
import com.liheit.im.core.ResourceManager
import com.liheit.im.core.protocol.*
import com.liheit.im.utils.json.MessageListConverter
import kotlinx.android.parcel.Parcelize
import java.util.ArrayList

/**
 * 收藏消息表
 */
@Parcelize
@Entity(tableName = "collect_msg")
data class CollectMsg(
        @ColumnInfo var describe: String? = "",//string 收藏描述
        @PrimaryKey var id: Long,// string 消息 ID
        @ColumnInfo var createTime: Long = 0,//int 消息收藏时间
        @ColumnInfo var type: Int = 0,// int 消息类型，参考后面的会话类型定义描述
        @ColumnInfo var userId: Long = 0,// 收藏人员 ID
        @ColumnInfo var content: String? = "",//消息内容
        @TypeConverters(MessageListConverter::class)
        @ColumnInfo var msgs: ArrayList<MsgBody>? = null,// Msg[] n 消息内容，具体结构参考 Msg
        @ColumnInfo var tag: String? = ""//收藏标签
) : Parcelable {
    companion object {
        //创建收藏消息
        fun createCollectMsg(describe: String, cid: Long, createTime: Long, msg: ChatMessage): CollectMsg {
            var cMsg = CollectMsg(describe = describe, id = cid, createTime = createTime,
                    type = msg.msgs!![0].mtype, userId = IMClient.getCurrentUserId(),
                    content = ChatMessage.getMsgShowText(msg), msgs = msg.msgs)
            return cMsg
        }

        //获取消息文本
        fun getMsgContent(msgs: ArrayList<MsgBody>): String {
            return msgs.map {
                return@map when (it) {
                    is TextBody -> {
                        val body = it as SecrecyBody
                        if (body.secrecy == 1 && (it.mtype == MessageType.TEXT.value || it.mtype == MessageType.EMOT.value)) {
                            ""
                        } else {
                            it.text
                        }
                    }
                    is RefsHeadBody -> it.text
                    is RefsEndBody -> it.text
                    is AtBody -> it.name
                    is FileBody -> {
                        if (it.mtype != MessageType.VOICE.value &&
                                it.mtype != MessageType.FORWARD.value) {
                            it.name?:""
                        } else {
                            ""
                        }
                    }
                    else -> ""
                }
            }.joinToString("") ?: ""
        }

        fun getMsgShowText(msgs: ArrayList<MsgBody>?): String {
            if (msgs != null) {
                return msgs.map {
                    return@map when (it) {
                        is TextBody -> {
                            val body = it as SecrecyBody
                            if (body.secrecy == 1 && (it.mtype == MessageType.TEXT.value || it.mtype == MessageType.EMOT.value)) {
                                ""
                            } else {
                                it.text
                            }
                        }
                        is EmojiBody -> {
                            val body = it as SecrecyBody
                            if (body.secrecy == 1 && (it.mtype == MessageType.TEXT.value || it.mtype == MessageType.EMOT.value)) {
                                ""
                            } else {
                                "[${it.key}]"
                            }
                        }
                        is RefsHeadBody -> it.text
                        is RefsEndBody -> it.text
                        is AtBody -> it.name
                        is FileBody -> it.name
                        else -> ""
                    }
                }.joinToString("") ?: ""
            }
            return ""
        }

        fun toDownloadParam(msg: CollectMsg): ResourceManager.DownloadParam {
            var msgBody = msg.msgs!![0]
            if (msgBody is FileBody) {
                return ResourceManager.DownloadParam()
                        .apply {
                            token = msgBody.token
                            md5 = msgBody.md5
                            name = msgBody.name
                            bytes = msgBody.bytes
                            type = msgBody.mtype
                            thumbnail = when (msgBody.mtype) {
                                MessageType.IMAGE.value,
                                MessageType.VIDEO.value
                                -> true
                                else -> false
                            }
                        }
            }
            return ResourceManager.DownloadParam()
        }
    }
}

data class CollectResult(
        var deleteList: MutableList<CollectJson>,
        var addList: MutableList<CollectJson>
)

data class CollectJson(
        var describe: String = "",// 发送者 ID
        var id: Long = 0,// string 消息 ID
        var createTime: Long = 0,//int 消息收藏时间
        var type: Int = 0,// int 消息类型，参考后面的会话类型定义描述
        var userId: Long = 0,// 收藏人员 ID
        var content: String? = "",//消息内容
        var msgs: String? = "",// Msg[] n 消息内容，具体结构参考 Msg
        var tag: String? = ""
)
