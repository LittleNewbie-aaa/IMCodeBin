@file:JvmName("ImCommonUtil")

package com.liheit.im.common.ext

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.ViewGroup
import android.widget.ImageView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liheit.im.common.emoji.EmojiconHandler
import com.liheit.im.common.glide.GlideApp
import com.liheit.im.core.Constants
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.*
import com.liheit.im.core.bean.ChatMessage.Companion.SEND_STATUS_SUCCESS
import com.liheit.im.core.bean.SessionType.*
import com.liheit.im.core.protocol.*
import com.liheit.im.core.protocol.session.ModifySessionReq
import com.liheit.im.utils.IDUtil
import com.liheit.im.utils.Log
import com.liheit.im.utils.TimeUtils
import com.liheit.im.utils.json.gson
import com.liheit.im.widget.TribeAvatar
import com.liheit.im_core.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

fun Conversation.getMemberIdTop4(): MutableList<Long> {
    val uIds = mutableListOf<Long>().apply {
        if (type == com.liheit.im.core.bean.SessionType.SESSION_P2P.value) {
            add(IDUtil.parseTargetId(IMClient.getCurrentUserId(), sid))
        } else {
            val list = IMClient.sessionManager.getSessionMembers(sid).toMutableList()
            if (list.size > 9) {
                addAll(list.subList(0, 9))
            } else {
                addAll(list)
            }
        }
    }
    return uIds
}

fun TribeAvatar.bindContent(ids: List<Long>) {
    this.removeAllViews()
    ids.forEach {
        var image = ImageView(this@bindContent.context)
        this@bindContent.addView(image, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
        image.setUserHeader(it)
    }
    this.requestLayout()
}

fun TribeAvatar.bindContent(logo: String?) {
    this.removeAllViews()
    var image = ImageView(this@bindContent.context)
    this@bindContent.addView(image, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT))
    image.setImageResource(R.drawable.default_head_m)
    logo?.let { GlideApp.with(image).load(logo).into(image) }
    this.requestLayout()
}

/**
 * 添加方法执行将user的查询和user信息放到一起
 */
fun TribeAvatar.bindContentWithContact(ids: List<Long>) {
    this.removeAllViews()
    ids.forEach { id ->
        var image = ImageView(this@bindContentWithContact.context)
        if(getAppName()==AppNameFlag.THE_HY_FLAG.value){
            image.scaleType = ImageView.ScaleType.FIT_XY
        }else{
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val headImg = if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
            R.drawable.hy_default_head
        } else {
            R.drawable.default_head_m
        }
        image.setImageResource(headImg)
        this@bindContentWithContact.addView(
                image, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        Observable.create<ImageView> { emt ->
            emt.onNext(image)
            emt.onComplete()
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeEx {
                    when (getAppName()) {
                        AppNameFlag.THE_LX_FLAG.value,
                        AppNameFlag.THE_XY_FLAG.value-> {
                            image.setUserDefaultHeader(id)
                        }
                        AppNameFlag.THE_SD_FLAG.value,
                        AppNameFlag.THE_HY_FLAG.value
                        -> {
                            image.setUserHeader(id, false, this@bindContentWithContact.context)
                        }
                    }
                }

    }
    this.requestLayout()
}

fun canRecall(isAdmin: Boolean, msg: ChatMessage): Boolean {
    if (msg.sendStatus != SEND_STATUS_SUCCESS) {
        return false
    }

    if (isAdmin && TimeUtils.getServerTime() - msg.t <= (24 * 60 * 60 * 1000)) {
        return true
    }
    if (msg.fromid == IMClient.getCurrentUserId() && TimeUtils.getServerTime() - msg.t <= (2 * 60_000)) {
        return true
    }

    return false
}

fun FileBody?.isDownloaded(): Boolean {
    if (this?.localPath.isNullOrBlank() || !File(this?.localPath).exists()) {
        return false
    }
    return true
}

fun ChatMessage.processExtraInfo() {
    val msg = this
    if (msg.bodyType == MessageType.SESSION_CHANGE.value) {
        (msg.getMessageBody() as EditSessionBody)?.also { notice ->
            notice.extraMetaData = mutableMapOf()
            fun getMembers(list: List<Long>): String {
                return IMClient.userManager.getUserByIds(list).map{
                    if(it.id==IMClient.getCurrentUserId()){
                        "“您“"
                    }else{
                        "“${it.name}“"
                    }
                }.joinToString("、")
            }
            when (notice.flag) {
                ModifySessionReq.ModifySessionType -> {
                    notice.extraMetaData?.put("author", getAuthorName(msg.fromid,false))
                }
                ModifySessionReq.ModifySessionTitle -> {
                    notice.extraMetaData?.put("author", getAuthorName(msg.fromid,false))
                }
                ModifySessionReq.ModifySessionCreaterID -> {
                    notice.extraMetaData?.put("author", getAuthorName(msg.fromid,false))
                    notice.extraMetaData?.put("owner", getAuthorName(notice.cid,true))
                }
                ModifySessionReq.ModifySessionAddAdmins -> {
                    val notify = getStringEx(R.string.chat_session_notific_set_admin, getAuthorName(msg.fromid,false), getMembers(notice.admins ?: listOf()))
                    notice.extraMetaData?.put("notify", notify)
                }

                ModifySessionReq.ModifySessionDelAdmins -> {
                    val notify = getStringEx(R.string.chat_session_notific_cancel_admin, getAuthorName(msg.fromid,false), getMembers(notice.admins ?: listOf()))
                    notice.extraMetaData?.put("notify", notify)
                }
                ModifySessionReq.ModifySessionAdd,
                ModifySessionReq.ModifySessionDel -> {
                    notice.extraMetaData?.put("author", getAuthorName(msg.fromid,false))
                    val members = mutableListOf<Long>()
                    notice.adds?.let(members::addAll)
                    notice.dels?.let(members::addAll)
                    members.let { IMClient.userManager.getUserByIds(it).map {
                        if(it.id==IMClient.getCurrentUserId()){
                            "“您“"
                        }else{
                            "“${it.name}“"
                        }
                    }.joinToString("、") }
                            ?.let { nameStr -> notice.extraMetaData?.put("members", nameStr) }
                }
                ModifySessionReq.ModifySessionAdd or ModifySessionReq.ModifySessionDel -> {
                    val author = getAuthorName(msg.fromid,false)
                    val addMembers = getMembers(notice.adds ?: listOf())
                    val delMembers = getMembers(notice.dels ?: listOf())
                    val notify = getStringEx(R.string.chat_session_notific_invite_and_remove, author, addMembers, delMembers)
                    notice.extraMetaData?.put("notify", notify)
                }
                ModifySessionReq.ModifySessionExit -> {
                    notice.extraMetaData?.put("author", msg.fromid?.let { getAuthorName(it,false) } ?: "")
                }
                ModifySessionReq.ModifySessionRemove -> {
                    notice.extraMetaData?.put("notify", "群已解散")
                }
                ModifySessionReq.ModifySessionTextNotice -> {
                    notice.extraMetaData?.put("author", msg.fromid?.let {
                        if (it == IMClient.getCurrentUserId()) {
                            "您"
                        } else {
                            "管理员"
                        } } ?: "")
                    notice.extraMetaData?.put("notify", "修改群公告")
                }
                else -> {
                    notice.extraMetaData?.put("notify", "未实现的通知")
                }
            }
        }
    }
}

private fun getAuthorName(uid: Long, isAddQuotes: Boolean): String {
    return if (uid == IMClient.getCurrentUserId()) {
        if(isAddQuotes){
            "“您“"
        }else{
            "您"
        }
    } else {
        "“${IMClient.userManager.getUserById(uid)?.name ?: ""}“"
    }
}

fun getEditSessionHint(localNotice: EditSessionBody): String {
//    Log.Companion.e("aaa localNotice=${gson.toJson(localNotice)}")
    val author = "${localNotice.extraMetaData?.get("author")}"
    val members = localNotice.extraMetaData?.get("members") ?: ""
    return when (localNotice.flag) {
        ModifySessionReq.ModifySessionTitle -> getStringEx(R.string.chat_session_notific_rename, author, "“${localNotice.title}“")
        ModifySessionReq.ModifySessionAdd -> getStringEx(R.string.chat_session_notific_invite, author, members)
        ModifySessionReq.ModifySessionDel -> getStringEx(R.string.chat_session_notific_remove, author, members)
        ModifySessionReq.ModifySessionType -> {
            val res = if (localNotice.type == SessionType.SESSION_FIX.value) R.string.chat_session_notific_upgrade_to_fix_session else R.string.chat_session_notific_upgrade_to_session
            getStringEx(res, author)
        }
        ModifySessionReq.ModifySessionCreaterID -> {
            getStringEx(R.string.chat_session_notific_transfor, author, localNotice.extraMetaData?.get("owner") ?: "")
        }
        ModifySessionReq.ModifySessionExit -> getStringEx(R.string.chat_session_notific_exit, author)
        ModifySessionReq.ModifySessionRemove -> getStringEx(R.string.chat_session_notific_dissolve, author)
        ModifySessionReq.ModifySessionTextNotice -> getStringEx(R.string.chat_session_notice_exit, author)
        else -> "${localNotice.extraMetaData?.get("notify")}"
    }
}

/**
 * 消息格式化，内部可能会查询sql
 */
fun ChatMessage?.msgFormat(forNotify: Boolean = false): String {
    if (this == null) return ""
    val msg = this
    if (msg.isRecall() || msg.isRecallMessage())
        return when {
            msg.type==SessionType.OFFICIAL_ACCOUNTS.value -> {
                "文章已撤回"
            }
            msg.recallBy == -1L -> {
                "${getAuthorName(msg.fromid,false)}撤回了一条消息"
            }
            else -> {
                "${getAuthorName(msg.recallBy,false)}撤回了一条消息"
            }
        }
    if (msg.getMessageBody() is TextBody || msg.getMessageBody() is EmojiBody || msg.getMessageBody() is ImgBody) {
        val body = msg.getMessageBody() as SecrecyBody
        if (body.secrecy == 1) {
            return "[无痕消息]"
        }
    }

    return msg.msgs?.map {
        return@map when (it) {
            is AtBody -> it.name + " "
            is TextBody -> it.text
            is EmojiBody -> if (forNotify) it.text else "[${it.key}]"
            is ImgBody -> "[图片]"
            is AudioBody -> "[语音]"
            is VideoBody -> "[视频]"
            is LocBody -> "[位置]"
            is AttachBody -> "[文件]"
//            is AttachBody -> "[图文消息]"
            is EmailBody -> "[邮件]"
            is AppRemindBody -> "[消息提醒]"
            is WorkFlowBody -> "[流程]"
            is WorkScheduleBody -> "[日程]"
            is WorkTaskBody -> "[任务]"
            is UpgradeBody -> "应用更新"
            is RefsHeadBody -> it.text
            is RefsEndBody -> it.text
            is MergeForwardBody -> "[聊天记录]"
            is NoticeBody -> it.subject + it.digest
            is MeetingNotification -> it.headline?.key + it.headline?.value
            is EditSessionBody -> "[通知]"
//            is AutoReplyBody -> "[自动回复]" + it.text
            is AutoReplyBody -> it.text
            is RecallBody -> "[通知]"
            is VoiceChatBody ->
//                if (it.trtctype == 0) {
                "[语音聊天]"
//                } else {
//                    "[视频聊天]"
//                }
            is VideoConferenceBody -> "[即时会议]"
            is VoteBody -> "[投票]"
            is SolitaireBody -> "[接龙]"
            is NewReplyBody -> ""
            is GraphicBody -> it.title
            else -> "不支持的消息"
        }
    }?.joinToString("") ?: ""
}

fun User.isSelf(): Boolean = this.id == IMClient.getCurrentUserId()

fun calculateImageViewSize(size: IntArray) {
    val w = size[0]
    val h = size[1]
    val maxWidth = dip2px(150f).toFloat()
    val maxHeight = dip2px(150f).toFloat()
    val minWidth = dip2px(40f).toFloat()
    val minHeight = dip2px(40f).toFloat()
    var scale = 1f

    if (w > h) {
        //如果宽度大于高度
        if (w > maxWidth)
            scale = maxWidth / w

        if (w < minWidth) {
            scale = minWidth / w
        }

        if (h * scale < minHeight) {
            scale = minHeight / h
        }
    } else {
        //如果宽度小于等于高度
        if (h > maxHeight)
            scale = maxHeight / h

        if (h < minHeight)
            scale = minHeight / h

        if (w * scale < minWidth) {
            scale = minWidth / w
        }
    }

    val imageWidth = Math.min(w * scale, maxWidth) //返回最小的
    val imageHeight = Math.min(h * scale, maxHeight)//返回最小的
    size[0] = imageWidth.toInt()
    size[1] = imageHeight.toInt()
}

fun Int.formatSessionType(): String {
    return when (this) {
        SESSION_FIX.value -> "固定群"
        SESSION.value -> "普通群"
        SESSION_DISC.value -> "讨论组"
        else -> "其他"
    }
}

@RequiresApi(Build.VERSION_CODES.DONUT)
fun Context.initEmotion() {
    val gson = Gson()
    val faces = mutableListOf<EmojiconHandler.QQFace>()
    try {
        resources.assets.open("Emoticon.json").reader().use {
            val gsonData = it.readText()
            gson.fromJson<List<Map<String, String>>>(
                    gsonData,
                    object : TypeToken<List<Map<String, String>>>() {}.type
            ).forEach { item ->
                /*{
                    "File": "face_02piezui.gif",
                    "Name": "[撇嘴]",
                    "Shortcut": "pz"
                }*/

                val resId = resources.getIdentifier(
                        item["File"]!!.replace(".gif", "", true),
                        "drawable",
                        applicationInfo.packageName
                )
                faces.add(EmojiconHandler.QQFace("[${item["Shortcut"]}]", resId, item["Name"]))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    EmojiconHandler.setup(faces)

    Constants.emojiDescs.clear()
    faces.forEach {
        Constants.emojiDescs.put(
                it.name.replace("[", "").replace("]", ""),
                it.desc.replace("[", "").replace("]", "")
        )
    }
}
