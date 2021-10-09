package com.liheit.im.core.bean

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.liheit.im.core.Cmd
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.SessionType.*
import com.liheit.im.core.protocol.*
import com.liheit.im.core.service.MessageService
import com.liheit.im.utils.*
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize

/**
 * 会话消息列表
 */
@Parcelize
@Entity
class Conversation(
        @PrimaryKey var sid: String = "",//会话id
        @ColumnInfo var name: String = "",//会话名字
        @ColumnInfo var type: Int = 0,//会话类型 （详情看SessionType类）
        @ColumnInfo var lastMessageId: String = "",//当前会话最后那条消息的id
        @ColumnInfo var lastMsgDate: Long = 0,//当前会话最后那条消息的时间
//        @Column var unreadCount: Int = 0,
//        @Column var lastMsgDate: Long = 0,
//        @Column var lastMsgContent: String = "",
        @ColumnInfo var isDelete: Boolean = false,//会话是否删除
        @Transient var isTop: Boolean = false,//会话是否置顶
        @Transient var isNotification: Boolean = true,//会话是否提示
        @Transient var isOfter: Boolean = false,//是否是常用会话
        @ColumnInfo var draft: String? = null//会话草稿消息
) : Parcelable {

    fun getConversationId(): String {
        return when (type) {
            FILE_HELP.value,
            SESSION_P2P.value
            -> IDUtil.parseTargetId(IMClient.getCurrentUserId(), sid).toString()
            SESSION_FIX.value,
            SESSION_DISC.value,
            SESSION_DEPT.value,
            SYSTEM_NOTICE.value,
            SYSTEM_NOTICEUSERS.value,
            WEB_APP_NOTICE.value,
            MEETING_NOTICE.value,
            DISSOLVE.value,
            SESSION.value,
            OFFICIAL_ACCOUNTS.value
            -> sid
            else -> {
                sid
                Log.e("${type}>>>")
                TODO("未实现")
            }
        }

    }

    fun markAllMessagesAsRead() {
        val setUpUnRead = SharedPreferencesUtil.getInstance(IMClient.context).getSP("${sid}UnRead").toIntOrNull() ?: 0
        if (setUpUnRead > 0) {
            MessageService.setUnReadMessageCount(sid, "0")
        }
        //获取所有未读消息
        val msgList = MessageService.getUnReadMessagesBySid(sid)
        if (msgList != null && msgList.size > 0) {
            syncMsg(msgList)
            //TODO  消息已读需要发送同步命令
            MessageService.setRead(msgList)
        } else if (setUpUnRead > 0) {
            IMClient.chatManager.triggerConversationListener(sid)
        }
    }

    @SuppressLint("CheckResult")
    private fun syncMsg(msgList: MutableList<ChatMessage>) {
        val syncInfo = msgList.groupBy { it.sid }
        val sid = if (syncInfo.size > 1) null else msgList.get(0).sid
        val syncMsgReq = SyncMsgReq(msgList.maxBy { it.t }!!.t, sid)
        IMClient.sendObservable(Cmd.ImpSyncMsgReq, syncMsgReq)
                .map { it.data!!.fromJson<SyncMsgRsp>() }
                .check()
                .subscribeOn(Schedulers.io())
                .doOnSuccess { MessageService.setRead(msgList) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    IMClient.chatManager.triggerConversationListener(sid?:"")
                }, {
                    it.printStackTrace()
                })
    }

    //把一条消息置为已读
    fun markMessageAsRead(msgId: String) {
        val msg = MessageService.findById(msgId)
        Log.e("aaa markMessageAsRead=${gson.toJson(msg)}")
        msg?.let { syncMsg(mutableListOf(msg)) }
    }

    fun getMessage(startMsgId: String?, pageSize: Int, isUp: Boolean = true): MutableList<ChatMessage> {
        return MessageService.findMessage(sid, startMsgId, pageSize, isUp)
    }

    fun getUnReadMsgCount(): Long {
        return MessageService.getUnReadMessageCount(sid)
    }

    fun getLastMassage(): ChatMessage? {
        return MessageService.getLastMessage(sid)
    }

    fun getLastTypeMessage(bodyType: Int): ChatMessage? {
        return MessageService.getLastTypeMessage(sid, bodyType)
    }

    fun setMassageUnRead() {
        MessageService.setUnReadMessageCount(sid, "1")
        IMClient.chatManager.triggerConversationListener(sid)
    }

    fun removeMessage(mid: String) {
        MessageService.delete(mid)
    }
}
