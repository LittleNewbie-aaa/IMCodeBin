package com.liheit.im.utils.json

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.liheit.im.core.bean.MessageType
import com.liheit.im.core.protocol.*
import com.liheit.im.utils.Log
import java.lang.Exception
import java.lang.reflect.Type

/**
 * 聊天消息预处理
 */
class MsgBodyDeserializer : JsonDeserializer<MsgBody> {
    val gson = GsonBuilder().create()

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): MsgBody? {

        val jsonObj = json.asJsonObject
        var type = -99
        try {
            type = jsonObj.get("mtype").asInt
            return when (type) {
                MessageType.TEXT.value -> gson.fromJson(json, object : TypeToken<TextBody>() {}.type)
                MessageType.EMOT.value -> gson.fromJson(json, object : TypeToken<EmojiBody>() {}.type)
                MessageType.EMOT_USER.value -> gson.fromJson(json, object : TypeToken<EmojiBody>() {}.type)
                MessageType.ANNEX.value -> gson.fromJson(json, object : TypeToken<AttachBody>() {}.type)
                MessageType.IMAGE.value -> gson.fromJson(json, object : TypeToken<ImgBody>() {}.type)
                MessageType.VOICE.value -> gson.fromJson(json, object : TypeToken<AudioBody>() {}.type)
                MessageType.VIDEO.value -> gson.fromJson(json, object : TypeToken<VideoBody>() {}.type)
                MessageType.LOCATION.value -> gson.fromJson(json, object : TypeToken<LocBody>() {}.type)
                MessageType.AT.value -> gson.fromJson(json, object : TypeToken<AtBody>() {}.type)
                MessageType.FORWARD.value -> gson.fromJson(json, object : TypeToken<MergeForwardBody>() {}.type)
                MessageType.REPLY_BEGIN.value -> gson.fromJson(json, object : TypeToken<RefsHeadBody>() {}.type)
                MessageType.REPLY_END.value -> gson.fromJson(json, object : TypeToken<RefsEndBody>() {}.type)
                MessageType.JOIN_MEETING.value -> gson.fromJson(json, object : TypeToken<MettingBody>() {}.type)
                MessageType.AUTO_REPLY.value -> gson.fromJson(json, object : TypeToken<AutoReplyBody>() {}.type)
                MessageType.NOTIFICATION.value -> gson.fromJson(json, object : TypeToken<NoticeBody>() {}.type)
                MessageType.WEBAPP_UPGRADE.value -> gson.fromJson(json, object : TypeToken<UpgradeBody>() {}.type)
                MessageType.WEBAPP_REMIND.value -> gson.fromJson(json, object : TypeToken<AppRemindBody>() {}.type)
                MessageType.WEBAPP_ARTICLES.value -> gson.fromJson(json, object : TypeToken<HybridMsgBody>() {}.type)
                MessageType.WEBAPP_EMAIL.value -> gson.fromJson(json, object : TypeToken<EmailBody>() {}.type)
                MessageType.WEBAPP_FLOW.value -> gson.fromJson(json, object : TypeToken<WorkFlowBody>() {}.type)
                MessageType.WEBAPP_SCHEDULE.value -> gson.fromJson(json, object : TypeToken<WorkScheduleBody>() {}.type)
                MessageType.WEBAPP_TASK.value -> gson.fromJson(json, object : TypeToken<WorkTaskBody>() {}.type)
                MessageType.WEBAPP_CLOUD_FILE.value -> gson.fromJson(json, object : TypeToken<CloudFileBody>() {}.type)
                MessageType.SESSION_CHANGE.value -> gson.fromJson(json, object : TypeToken<EditSessionBody>() {}.type)
                MessageType.RECALL.value -> gson.fromJson(json, object : TypeToken<RecallBody>() {}.type)
                MessageType.RECEIPT.value -> gson.fromJson(json, object : TypeToken<ReceiptBody>() {}.type)
                MessageType.VOICECHAT.value -> gson.fromJson(json, object : TypeToken<VoiceChatBody>() {}.type)
                MessageType.VIDEOCONFERENCE.value -> gson.fromJson(json, object : TypeToken<VideoConferenceBody>() {}.type)
                MessageType.MEETING_NOTIFICATION.value -> gson.fromJson(json, object : TypeToken<MeetingNotification>() {}.type)
                MessageType.VOTE.value -> gson.fromJson(json, object : TypeToken<VoteBody>() {}.type)
                MessageType.SOLITAIRE.value -> gson.fromJson(json, object : TypeToken<SolitaireBody>() {}.type)
                MessageType.NEW_REPLY.value -> gson.fromJson(json, object : TypeToken<NewReplyBody>() {}.type)
                MessageType.GRAPHIC.value -> gson.fromJson(json, object : TypeToken<GraphicBody>() {}.type)
                //else ->  UN_SUPPORT
                else -> UnsupportBody(gson.fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type))
            }
        } catch (e:Exception){
            Log.Companion.e("aaa type=${type} 错误")
            return UnsupportBody(gson.fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type))
        }
    }
}