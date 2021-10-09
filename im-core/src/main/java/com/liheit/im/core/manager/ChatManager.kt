package com.liheit.im.core.manager

import android.annotation.SuppressLint
import android.os.Environment
import android.support.v4.util.LruCache
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.liheit.im.common.ext.AppNameFlag
import com.liheit.im.common.ext.getAppName
import com.liheit.im.common.ext.subscribeEx
import com.liheit.im.core.*
import com.liheit.im.core.IMClient.getCurrentUserId
import com.liheit.im.core.bean.*
import com.liheit.im.core.bean.ChatMessage.Companion.SEND_STATUS_SUCCESS
import com.liheit.im.core.http.PkurgClient
import com.liheit.im.core.protocol.*
import com.liheit.im.core.protocol.message.GetMessageReq
import com.liheit.im.core.protocol.message.GetMessageRsp
import com.liheit.im.core.protocol.session.ModifySessionReq
import com.liheit.im.core.service.ConversationService
import com.liheit.im.core.service.MessageService
import com.liheit.im.core.service.SubscriptionService
import com.liheit.im.core.service.save
import com.liheit.im.utils.*
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*
import java.util.Collections.synchronizedList

/**
 * 聊天消息管理器
 */
@SuppressLint("CheckResult")
class ChatManager(private var im: IMClient) : MsgHandler {

    val MAX_MSG_CACHE_SIZE = 10000
    private var mCache = LruCache<String, ChatMessage>(MAX_MSG_CACHE_SIZE)

    //公众号消息（h5内容高度缓存）
    lateinit var graphicMsgContentHeights: MutableMap<String, Int>
    lateinit var fileMsgUpProgress: MutableMap<String, Int>

    init {
        val heights = SharedPreferencesUtil.getInstance(IMClient.context).getSP("GraphicMsgContentHeights")
        graphicMsgContentHeights = if (heights.isNullOrEmpty()) {
            mutableMapOf()
        } else {
            heights.fromJson<MutableMap<String, Int>>()
        }
        val upProgress = SharedPreferencesUtil.getInstance(IMClient.context).getSP("FileMsgUpProgress")
        fileMsgUpProgress = if (upProgress.isNullOrEmpty()) {
            mutableMapOf()
        } else {
            upProgress.fromJson<MutableMap<String, Int>>()
        }

    }

    //添加公众号消息 h5内容高度缓存
    fun addGraphicMsgContentHeight(mid: String, height: Int) {
        graphicMsgContentHeights[mid] = height
        SharedPreferencesUtil.getInstance(IMClient.context).putSP("GraphicMsgContentHeights", gson.toJson(graphicMsgContentHeights))
    }

    fun removeGraphicMsgContentHeight() {
        graphicMsgContentHeights = mutableMapOf()
        SharedPreferencesUtil.getInstance(IMClient.context).putSP("GraphicMsgContentHeights", gson.toJson(graphicMsgContentHeights))
    }

    //添加文件上传进度
    fun addFileMsgUpProgress(mid: String, progress: Int) {
        fileMsgUpProgress[mid] = progress
        SharedPreferencesUtil.getInstance(IMClient.context).putSP("FileMsgUpProgress", gson.toJson(fileMsgUpProgress))
    }

    override fun getHandlerType(): List<Int> {
        return listOf(
                Cmd.ImpMsgNotice,
                Cmd.ImpSyncMsgRsp,
                Cmd.ImpSyncMsgNotice,
                Cmd.ImpVoiceCallNotice,
                Cmd.ImpMsgDeletenotice,
                Cmd.ImpVideoSwitchVoice,
                Cmd.ImpMeetingNotice
        )
    }

    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        when (cmd) {
            //通知客户端新消息已读(多端状态同步)
            Cmd.ImpSyncMsgNotice -> {
                val resp = data.fromJson<SyncMsgNotice>()
                Log.e("aaa ImpSyncMsgNotice${gson.toJson(resp)}")
                resp.sid?.let { MessageService.setMsgRead(it, resp.t) }
                IMClient.sendACK(Cmd.ImpSyncMsgNoticeAck, resp)
                triggerConversationListener(resp.sid ?: "")
                //TODO Need notice unread countChange?
            }
            //消息通知
            Cmd.ImpMsgNotice -> {
                //接收到消息提醒后发送消息
                Log.e("aaa ImpMsgNotice=${data}")
                val msg = data.fromJson<ChatMessage>()
                when {
                    //处理回执消息
                    msg.isReceiptToMessage() -> {
                        handleReceiptMessage(msg)
                    }
                    //处理撤回消息
                    msg.isRecallMessage() -> {
                        handlerRecallMessage(msg)
                    }
                    else -> {
                        //如果是公众号消息保存公众号信息
                        if (msg.type == SessionType.OFFICIAL_ACCOUNTS.value) {
                            handleSubscriptionInfo(msg)
                        }
                        msg.msgs?.forEach {
                            if (it is FileBody) it.localPath = "" //收到文件类型消息本地地址设置为空
                            if (it is NoticeBody) it.isRead = 1 //收到通知类型消息设置为未读
                            if (it is AudioBody) {
                                if (msg.fromid != IMClient.getCurrentUserId()) {
                                    it.isPlay = false
                                }
                            }
                            if (it is GraphicBody) {
                                writeHtml(it.graphicId.toString(), it.context)
                            }
                            if (it is EditSessionBody) {
                                when (it.flag) {
                                    ModifySessionReq.ModifySessionType,
                                    ModifySessionReq.ModifySessionTitle,
                                    ModifySessionReq.ModifySessionCreaterID,
                                    ModifySessionReq.ModifySessionAddAdmins,
                                    ModifySessionReq.ModifySessionDelAdmins,
                                    ModifySessionReq.ModifySessionAdd,
                                    ModifySessionReq.ModifySessionDel,
                                    ModifySessionReq.ModifySessionExit,
                                    ModifySessionReq.ModifySessionRemove
//                                        , ModifySessionReq.ModifySessionTextNotice
                                    -> {
                                        msg.addFlag(ChatMessage.FLAG_READ)
                                    }
                                }
                            }
                        }
                        msg.sendStatus = SEND_STATUS_SUCCESS
                        if (msg.isFromMySelf()) {
                            msg.addFlag(ChatMessage.FLAG_READ)//设置消息未读
                        }
                        if (msg.bodyType != MessageType.VOICECHAT.value || (msg.getMessageBody() as? VoiceChatBody)?.audiotype != 5) {
                            synchronized(msgListeners) {
                                /**
                                 * 处理回执消息
                                 */
                                MessageService.insertMessageAndReceiptStatus(msg)
                                IMClient.runOnMainThread {
                                    msgListeners.forEach { it.onMessageReceived(mutableListOf(msg)) }
                                }
                                updateConversation(msg)
                            }
                        }
                    }
                }
                sendMsgNoticeAck(msg)
            }
            //语音聊天房间通知
            Cmd.ImpVoiceCallNotice,
                //单人视频通话切换成语音聊天通知
            Cmd.ImpVideoSwitchVoice,
                //视频会议通话房间通知
            Cmd.ImpMeetingNotice -> {
                synchronized(msgCmdListeners) {
                    IMClient.runOnMainThread {
                        msgCmdListeners.forEach {
                            it.onMessageReceived(cmd, data)
                        }
                    }
                }
            }
            //阅后即焚消息删除通知
            Cmd.ImpMsgDeletenotice -> {
                Log.e("aaa ImpMsgDeletenotice=${data}")
                val resp = data.fromJson<DeleteSecrecyMsgNotice>()
                val msg = getMessageById(resp.mid)
                MessageService.delete(resp.mid)
                IMClient.runOnMainThread {
                    msgListeners.forEach { it.onMessageDelete(msg?.sid, resp.mid) }
                }
                msg?.let { triggerConversationListener(it.sid) }
            }
        }
    }

    /**
     *  保存更新公众号信息
     */
    private fun handleSubscriptionInfo(msg: ChatMessage) {
        msg.getMessageBody()?.let {
            if (it.mtype == MessageType.GRAPHIC.value) {
                val body = it as GraphicBody
                SubscriptionService.save(body.subscription)
            }
        }
    }

    /**
     *  处理回执信息
     */
    private fun handleReceiptMessage(msg: ChatMessage, notifyChange: Boolean = true) {
        val receiptBody = msg.getMessageBody() as ReceiptBody
        var mid = receiptBody.mid
        val message = MessageService.findById(mid) ?: return

        if (message.type == SessionType.SESSION_P2P.value || msg.fromid == getCurrentUserId()) {
            MessageService.messageAddFlag(mid, ChatMessage.FLAG_RECEIPTED)
            message.flag = message.flag.or(ChatMessage.FLAG_RECEIPTED)
        } else {
            //多人会话，要先修改回执人的状态为已回执，然后统计是否全部人员已经回执
            MessageService.setUserReceipted(mid, msg.fromid)//修改已经回执的人状态
            var isAllReceipted = MessageService.messageIsAllReceipted(mid)
            if (isAllReceipted) {
                var newFlag = ChatMessage.FLAG_RECEIPT_ALL_READ and ChatMessage.FLAG_RECEIPT
                MessageService.messageAddFlag(mid, newFlag)
                message.flag = message.flag or newFlag
            }
            //设置回执的人数和未回执的人数
            message.unReceiptCount = MessageService.getReceiptedCount(mid, false).toInt()
            message.receiptCount = MessageService.getReceiptedCount(mid, true).toInt()
        }
        if (notifyChange)
            message.let {
                onMessageChanged(mutableListOf(it))
            }
    }

    /**
     * 处理撤回消息
     */
    private fun handlerRecallMessage(msg: ChatMessage) {
        (msg.getMessageBody() as? RecallBody)?.let { recallBody ->
            val m = MessageService.findById(recallBody.mid)
            if (m != null) {
                if (msg.type == SessionType.SYSTEM_NOTICE.value ||
                        msg.type == SessionType.MEETING_NOTICE.value) {
                    MessageService.delete(recallBody.mid)
                } else {
                    onRecallMessage(m, msg.fromid)
                }
            } else {
                msg.sendStatus = SEND_STATUS_SUCCESS
                if (msg.type != SessionType.SYSTEM_NOTICE.value &&
                        msg.type != SessionType.MEETING_NOTICE.value) {
                    msg.recallBy = msg.fromid
                    MessageService.save(msg)
                    MessageService.setRead(mutableListOf(msg))
                }
            }
        }
    }

    //处理被撤回的那条消息
    private fun onRecallMessage(m: ChatMessage, recallBy: Long) {
        m.recallBy = recallBy
        m.content = ""
        m.addFlag(ChatMessage.FLAG_RECALL)
//        m.addFlag(ChatMessage.FLAG_READ)
        Log.e("aaa onRecallMessage save ${gson.toJson(m)}")
        MessageService.save(m)
        synchronized(msgListeners) {
            im.runOnMainThread {
                msgListeners.forEach { it.onMessageRecall(m) }
            }
        }
        onMessageChanged(mutableListOf(m))
    }

    /**
     * 信息变动，调用回调接口，反馈消息
     */
    fun onMessageChange(msg: ChatMessage) {
        synchronized(msgListeners) {
            IMClient.runOnMainThread {
                msgListeners.forEach { it.onMessageReceived(mutableListOf(msg)) }
            }
        }
    }

    /**
     * 信息变动，调用回调接口，反馈消息
     */
    fun onFileMessageUpProgress(msg: ChatMessage, progress: Int) {
        IMClient.runOnMainThread {
            msgListeners.forEach { it.onFileMessageUpProgress(msg, progress) }
        }
    }

    /**
     * 接收自己在其他页面发送的消息
     */
    fun onOneselfSendMessage(msg: ChatMessage) {
        synchronized(msgListeners) {
            IMClient.runOnMainThread {
                msgListeners.forEach { it.onOneselfSendMessage(mutableListOf(msg)) }
            }
        }
    }

    /**
     * 向服务器发送同步消息通知
     */
    private fun syncMsg(msg: List<ChatMessage>?) {
        if (msg == null) return
        if (msg.isEmpty()) {
            return
        }

        var t = msg.maxBy { it.t }!!.t
        val syncCmd = SyncMsgReq(t)
        IMClient.sendObservable(Cmd.ImpSyncMsgReq, syncCmd)
                .map { it.data!!.fromJson<SyncMsgRsp>() }
                .check()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    it.printStackTrace()
                })
    }

    /**
     * 消息变化通知
     */
    fun onMessageChanged(msgs: MutableList<ChatMessage>) {
        IMClient.runOnMainThread {
            msgListeners.forEach { it.onMessageChanged(msgs) }
            triggerConversationListener(msgs[0].sid)
        }
    }

    /**
     * 更新会话通知
     */
    private fun updateConversation(msg: ChatMessage?) {
        msg?.let {
            ConversationService.updateConversation(msg)
            triggerConversationListener(msg.sid)
        }
    }

    /**
     * 更新会话列表通知
     */
    internal fun triggerConversationListener(sid: String) {
        IMClient.runOnMainThread {
            conversationListeners?.forEach { it.onConversationChange(sid) }
        }
    }

    /**
     * 获取到离线消息
     */
    fun getOfflineMsg(): Single<List<ChatMessage>> {
        return IMClient.sendObservable(Cmd.ImpGetOfflineMsgReq, GetOfflineMsgReq(getLastMsgTime()))
                .flatMapObservable(::parseOfflinePackate)
                .doOnNext { messages ->
                    Log.e("aaa getOfflineMsg${gson.toJson(messages)}")
                    synchronized(messages) {
                        messages?.forEach { msg ->
                            if (msg.isFromMySelf()) {
                                //是自己发的消息设置已读
                                msg.addFlag(ChatMessage.FLAG_READ)
                            }
                            msg.msgs?.forEach {
                                if (it is FileBody) {
                                    //是文件消息设置本地路径为空
                                    it.localPath = ""
                                }
                                if (it is NoticeBody) {
                                    //是待办通知消息设置已读
                                    it.isRead = 1
                                }
                                if (it is AudioBody) {
                                    //是语音消息设置未读标记
                                    if (msg.fromid != getCurrentUserId()) {
                                        it.isPlay = false
                                    }
                                }
                                if (it is GraphicBody) {
                                    writeHtml(it.graphicId.toString(), it.context)
                                }
                                if (it is EditSessionBody) {
                                    //群信息修改消息设置已读
                                    when (it.flag) {
                                        ModifySessionReq.ModifySessionType,
                                        ModifySessionReq.ModifySessionTitle,
                                        ModifySessionReq.ModifySessionCreaterID,
                                        ModifySessionReq.ModifySessionAddAdmins,
                                        ModifySessionReq.ModifySessionDelAdmins,
                                        ModifySessionReq.ModifySessionAdd,
                                        ModifySessionReq.ModifySessionDel,
                                        ModifySessionReq.ModifySessionExit,
                                        ModifySessionReq.ModifySessionRemove
//                                        , ModifySessionReq.ModifySessionTextNotice
                                        -> {
                                            msg.addFlag(ChatMessage.FLAG_READ)
                                        }
                                    }
                                }
                            }
                            msg.sendStatus = SEND_STATUS_SUCCESS
                        }
                        //TODO 保存所有普通消息
                        MessageService.insertBatch(messages.filter {
                            !it.isRecallMessage() && !it.isRecall() && !it.isReceiptToMessage()
                                    && (it.getMessageBody() as? VoiceChatBody)?.audiotype != 5
                        })
                        //TODO 处理所有撤回message
                        messages.filter { it.isRecallMessage() }
                                .forEach { handlerRecallMessage(it) }
                        //TODO 处理回执消息
                        messages.filter { it.isReceiptToMessage() }
                                .forEach { handleReceiptMessage(it, false) }
                        //TODO 处理其他消息
                        //可以对消息先分组
                        messages?.filter {
//                        !it.isRecallMessage() && !it.isReceiptToMessage() &&
                            (it.getMessageBody() as? VoiceChatBody)?.audiotype != 5
                        }?.groupBy { it.sid }?.forEach {
                            it.value.forEach {
                                if (it.type == SessionType.OFFICIAL_ACCOUNTS.value) {
                                    handleSubscriptionInfo(it)
                                }
                            }
                            it.value.maxBy { it.t }?.let {
                                ConversationService.updateConversation(it)
                            }
                        }.let {
                            triggerConversationListener("")
                        }

                        if (messages?.isNotEmpty() == true) {
                            synchronized(msgListeners) {
                                IMClient.runOnMainThread {
                                    msgListeners.forEach { it.onMessageReceived(messages, true) }
                                }
                            }
                        }
                        syncMsg(messages)
                    }
                }.subscribeOn(ImSchedulers.dispatcher()).singleOrError()
    }

    //获取同步已读消息
    fun getChatReadState(): Completable {
        return IMClient.sendObservable(Cmd.ImpGetReadStateReq, "")
                .map {
                    Log.e("aaa getChatReadState${gson.toJson(it)}")
                    it.data!!.fromJson<SyncReadStateRsp>()
                }
                .check()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable { resp ->
                    resp.readstates?.forEach { msg ->
                        msg.sid?.let {
                            MessageService.setMsgRead(it, msg.t)
                        }
                    }
                    triggerConversationListener("")
                    return@flatMapCompletable Completable.complete()
                }

    }

    var cacheDir: File

    init {
        cacheDir = IMClient.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    /**
     * 获取最后一条消息时间
     */
    private fun getLastMsgTime(): Long {
        return MessageService.getLastMsgTime()
    }

    /**
     * 离线消息预处理返回解析后数据
     */
    private fun parseOfflinePackate(resp: CommondResp): Observable<List<ChatMessage>> {
        return when (resp.packageType) {
            PackageType.PACKAGE_TYPE_ZIP,
            PackageType.PACKAGE_TYPE_GSON
            -> {
                Log.e("aaa parseOfflinePackate")
                return Observable.just(
                        resp.data?.fromJson<GetOfflineMsgRsp>()?.messages
                                ?: listOf()
                )
            }
            PackageType.PACKAGE_TYPE_URL -> {
                val file =
                        File(File(cacheDir.absolutePath, "offline"), UUID.randomUUID().toString())
                return urlRespToFile(resp.data!!, file)
                        .flatMapObservable { f ->
                            return@flatMapObservable Observable.create<ChatMessage> { emitter ->
                                var reader = JsonReader(FileReader(f))
                                reader.beginObject()
                                var userCount = 0
                                var begin = System.currentTimeMillis()
                                var lastUpdateTime = 0L
                                while (reader.hasNext()) {
                                    val name = reader.nextName()
                                    Log.d("name ${name}")
                                    when (name) {
                                        "result" -> println("result:" + reader.nextInt())
                                        "t" -> {
                                            lastUpdateTime = reader.nextLong()
                                            println("t:$lastUpdateTime")
                                        }
                                        "asc" -> {
                                            reader.nextBoolean()
                                        }
                                        "messages" -> {
                                            reader.beginArray()
                                            while (reader.hasNext()) {
                                                val chat = gson.fromJson<ChatMessage>(
                                                        reader, ChatMessage::class.java
                                                )
                                                userCount++
                                                emitter.onNext(chat)
                                            }
                                            reader.endArray()
                                            emitter.onComplete()
                                            Log.i("lastUserUpdateTime:${lastUpdateTime}")
                                        }
                                    }
                                }
                                Log.e("耗时：${System.currentTimeMillis() - begin} 用户总数 ${userCount}")
                                reader.endObject()
                            }
                                    .buffer(10000)
                                    .doFinally {
                                        f.delete()
                                        file.delete()
                                    }
                        }
            }
            else -> Observable.error(RuntimeException("协议错误"))
        }
    }

    //发送消息的处理 包括视频图片音频的 等上传和消息的回调
    fun sendMessage(msg: ChatMessage) {
        msg.sendStatus = ChatMessage.SEND_STATUS_SENDING
        Single.just(msg)
                .map {
                    var type = msg.msgs?.last()?.mtype
                    //合并多条消息生成压缩文件
                    genForwardJsonFile(type, msg)
                    //处理文件消息（图片、视频设置宽高 语音消息设置时长  文件信息）
                    genUploadParam(type, msg)
                    //复制一份消息把本地设置为已读
                    val copy = msg.copy()
                    //自己发送的消息，状态都是已读
                    copy.flag = msg.flag.or(ChatMessage.FLAG_READ)
                    //添加回执消息
                    if (msg.bodyType != MessageType.VOICECHAT.value || (msg.getMessageBody() as? VoiceChatBody)?.audiotype != 5) {
                        MessageService.insertMessageAndReceiptStatus(copy)
                        updateConversation(copy)
                    }
                    return@map it
                }.flatMap {
                    //华远发送消息添加敏感词检测并上传服务器
                    if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
                        if (!it.content.isNullOrEmpty()) {
                            var text = SensitiveWordsUtils.findText(it.content)
                            if (!text.isNullOrEmpty()) {
                                var code = it.sid
                                var type = 1 //0-单聊 1-群聊
                                if (it.type == SessionType.SESSION_P2P.value ||
                                        it.type == SessionType.FILE_HELP.value
                                ) {
                                    type = 0
                                    code = if (it.type == SessionType.FILE_HELP.value) {
                                        Constants.FILE_HELP_ID.toString()
                                    } else {
                                        it.toid.toString()
                                    }
                                }
                                PkurgClient.saveViolate(type, code, ChatMessage.getMsgShowText(it), text.toString())
                                        .subscribeOn(Schedulers.io())
                                        .subscribe({
                                            Log.e("saveViolate=$it")
                                        }, {
                                            Log.e("aaa saveViolate onError=$it")
                                        })
                            }
                        }
                    }
                    Single.just(it)
                }.flatMap { m ->
                    //upload file is need
                    var type = m.getMessageBody()?.mtype
                    //上传文件  图片 视频信息调用
                    if ((type == MessageType.IMAGE.value || type == MessageType.VOICE.value ||
                                    type == MessageType.VIDEO.value || type == MessageType.FORWARD.value) &&
                            (m.getMessageBody() as FileBody).localPath != null
                    ) {
                        Observable.fromIterable(m.msgs)
                                .flatMap {
                                    //初始化文件信息
                                    var param = UploadParam()
                                    if (it is FileBody) {
                                        //如果是属于filebaody类型的
                                        param.filePath = it.localPath
                                        param.token = it.md5
                                        param.fileName = it.md5
                                        param.fileSize = it.bytes
                                    }
                                    param.type = it.mtype
                                    param.account = IMClient.getCurrentUserAccount() ?: ""
                                    param.userCode = null
                                    IMClient.resourceManager.uploadFileSynchronous(msg.mid, param)
                                            .doOnSubscribe {}
                                            .doOnComplete {
                                                //发送图片等信息成功后回调
                                                m.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
                                            }
                                            .subscribeOn(ImSchedulers.fileUpload())
                                            .observeOn(ImSchedulers.ui())
                                            .doOnNext {
                                                msg.messageStatusCallback?.onProgress(it.second)
                                            }
                                            .lastElement().toObservable()
                                }.map { m }.singleOrError()
                    } else {
                        Single.just(m)
                    }
                }.flatMap { m ->
                    var isChangedMessage = true
                    var type = m.getMessageBody()?.mtype
                    //上传文件  图片 视频信息调用
                    if ((type == MessageType.ANNEX.value) && (m.getMessageBody() as FileBody).localPath != null) {
                        isChangedMessage = false
                        uploadFile(m)
                    }

                    //向IM发送消息
                    IMClient.sendObservable(Cmd.ImpSendMsgReq, m.toReq())
                            .map { it.data!!.fromJson<SendMsgRsp>() }
                            .check()
                            .doOnSuccess { resp ->
                                if (isChangedMessage) {
                                    var m = MessageService.findById(resp.mid)
                                    m?.let {
                                        it.flag = msg.flag.or(ChatMessage.FLAG_READ)
                                        it.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
                                        it.unReceiptCount = MessageService.getReceiptedCount(it.mid, false).toInt()
                                        it.receiptCount = MessageService.getReceiptedCount(it.mid, true).toInt()
                                        MessageService.update(it)  //消息入库
//                                    updateConversation(m)  //通知更新会话
                                        onMessageChanged(mutableListOf(m))  //通知更新消息
                                    }
                                }
                            }
                }
                .doOnError {
                    //消息发送失败
                    it.printStackTrace()
                    msg.flag = msg.flag.or(ChatMessage.FLAG_READ)
                    msg.sendStatus = ChatMessage.SEND_STATUS_ERROR
                    MessageService.update(msg)
                    onMessageChanged(mutableListOf(msg))
                }
                .subscribeOn(ImSchedulers.send())
                .observeOn(ImSchedulers.ui())
                .subscribe({
                    //发送普通信息成功后的回调
                    msg.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
                    msg.messageStatusCallback?.onSuccess()
                }, {
                    //发送失败处理
                    it.printStackTrace()
                    var e = it.toIMException()
                    msg.messageStatusCallback?.onError(e.code, e.message + "")
                })
    }

    //上传文件
    private fun uploadFile(msg: ChatMessage) {
        //初始化文件信息
        var param = UploadParam()
        val copy = msg.copy()
        if (msg.getMessageBody() is FileBody) {
            val it = msg.getMessageBody() as FileBody
            //如果是属于filebaody类型的
            param.filePath = it.localPath
            param.token = it.md5
            param.fileName = it.md5
            param.fileSize = it.bytes
        }
        param.type = msg.getMessageBody()?.mtype!!
        param.account = IMClient.getCurrentUserAccount() ?: ""
        param.userCode = null
        IMClient.resourceManager.uploadFile(msg.mid, param)
                .subscribeOn(ImSchedulers.fileUpload())
                .observeOn(ImSchedulers.ui())
                .doOnNext {
                    addFileMsgUpProgress(msg.mid, it.second)
                    onFileMessageUpProgress(copy, it.second)
                    msg.messageStatusCallback?.onProgress(it.second)
                }.doOnComplete {
                    //发送图片等信息成功后回调
                    var m = MessageService.findById(msg.mid)
                    m?.let {
                        Log.e("aaa m=${gson.toJson(it)}")
                        it.flag = msg.flag.or(ChatMessage.FLAG_READ)
                        it.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
                        it.unReceiptCount = MessageService.getReceiptedCount(it.mid, false).toInt()
                        it.receiptCount = MessageService.getReceiptedCount(it.mid, true).toInt()
                        MessageService.update(it)  //消息入库
//                        updateConversation(m)  //通知更新会话
                        onMessageChanged(mutableListOf(it))  //通知更新消息
                    }
                    Log.e("aaa 发送文件 成功后回调")
                }
                .doOnError {
                    Log.e("aaa 发送文件失败 ${it.message}")
                }
                .subscribe()
    }

    internal fun resetAllSendingMessage() {
        MessageService.setStatus(ChatMessage.SEND_STATUS_SENDING, ChatMessage.SEND_STATUS_ERROR)
    }

    /**
     * 处理文件消息（图片、视频设置宽高 语音消息设置时长  文件信息）
     */
    private fun genUploadParam(type: Int?, msg: ChatMessage) {
        msg.msgs?.forEach { msgBody ->
            when (msgBody) {
                is FileBody -> {
                    var filePath = msgBody.localPath
                    if (filePath != null) {
                        var file = File(filePath)
                        var fileMd5: String? = null
                        try {
                            fileMd5 = MD5Util.md5Hex(FileInputStream(file))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        msgBody.md5 = fileMd5!!
                        msgBody.token = fileMd5
                        msgBody.name = file.name
                        msgBody.bytes = file.length()
                    }
                }
            }
        }
    }

    /**
     * 合并消息生成压缩文件
     */
    private fun genForwardJsonFile(type: Int?, msg: ChatMessage) {
        if (type == MessageType.FORWARD.value) {
            msg.msgs?.forEach { body ->
                if (body is MergeForwardBody && body.mids != null && body.mids!!.size > 1) {
                    var msgs = body.mids?.map { IMClient.chatManager.getMessageById(it)?.toReq() }
                    var jsonZip = getForwardFile("${msg.mid}.zip")
                    var password = UUID.randomUUID().toString().replace("-", "")
                    var fileName = "${msg.mid}.json"
                    ZipUtils.zip(gson.toJson(msgs), fileName, jsonZip, password)
                    body.localPath = jsonZip.absolutePath
                    body.key = password
                    body.name = fileName
                }
            }
        }
    }

    fun getMergeMessageBody(msg: ChatMessage): Observable<MutableList<ChatMessage>> {
        return Observable.just(msg)
                .flatMap { m ->
                    val body = m.getMessageBody()
                    if (body is MergeForwardBody && !body.localPath.isNullOrEmpty() && File(body.localPath).exists()) {
                        return@flatMap Observable.just(m)
                    } else {
                        return@flatMap downloadRes(msg)
                    }
                }
                .map { m ->
                    val body = m.msgs?.getOrNull(0)
                    if (body is MergeForwardBody) {
                        var zipPath = body?.localPath
                        var password = body?.key
                        val json = ZipUtils.readZip(zipPath, password)
                        val msgs = gson.fromJson<MutableList<ChatMessage>>(
                                json, object : TypeToken<MutableList<ChatMessage>>() {}.type
                        )
                        msgs.forEach {
                            it.preProcess()
                            it.sendStatus = SEND_STATUS_SUCCESS
                        }
                        MessageService.setRead(msgs)
                        return@map msgs
                    }
                    return@map mutableListOf<ChatMessage>()
                }
    }

    fun downloadAttachment(mid: String): Observable<ChatMessage> {
        return Observable.just(mid)
                .map { getMessageById(it) }
                .flatMap { m ->
                    val body = m.msgs?.getOrNull(0)
                    if (body is FileBody) {
                        var zipPath = body.localPath
                        if (!zipPath.isNullOrEmpty() && File(zipPath).exists()) {
                            return@flatMap Observable.just(m)
                        } else {
                            return@flatMap downloadRes(m)
                        }
                    } else {
                        return@flatMap Observable.just(m)
                    }
                }
    }

    fun downloadRes(msg: ChatMessage, thumb: Boolean = false): Observable<ChatMessage> {
        if (msg.msgs == null || msg.msgs!!.size == 0) {
            return Observable.just(msg)
        }
        var mtype = msg.getMessageBody()?.mtype
        return when (mtype) {
            MessageType.VOICE.value,
            MessageType.IMAGE.value,
            MessageType.FORWARD.value,
            MessageType.ANNEX.value,
            MessageType.VIDEO.value
            -> {
                val filePath = getFilePath(msg.getMessageBody()!!)
                var m: ChatMessage? = null
                IMClient.resourceManager.downloadResource(
                        filePath, msg.toDownloadParam().apply { this.thumbnail = thumb })
                        .doOnNext { Log.v("progress:${it}") }
                        .doOnComplete {
                            msg.msgs?.get(0).let {
                                if (it is FileBody) {
                                    it.localPath = filePath
                                }
                            }
                            m = MessageService.findById(msg.mid)
                            Log.e("aaa downloadRes=${gson.toJson(m)}")
                            if (m?.isRecall() == false && m?.isRecallMessage() == false) {
                                Log.e("aaa 不是撤回的消息")
                                m?.msgs?.get(0)?.let {
                                    if (it is FileBody) {
                                        it.localPath = filePath
                                    }
                                    m?.save()
                                }
                            }
                        }
                        .lastOrError().map { m ?: msg }.toObservable()
            }
            else -> {
                Observable.just(msg)
            }
        }
    }

    /**
     * 向服务器发送接收消息成功通知
     */
    private fun sendMsgNoticeAck(msg: ChatMessage?) {
        msg?.let {
            IMClient.sendACK(
                    Cmd.ImpMsgNoticeAck,
                    MsgNoticeAck(TimeUtils.getServerTime(), it.mid, it.sid)
            )
        }
    }

    private var msgCmdListeners =
            synchronizedList<CmdMessageListener>(mutableListOf<CmdMessageListener>())

    fun addCmdMessageListener(cmdlistener: CmdMessageListener?) {
        cmdlistener?.let {
            msgCmdListeners.add(cmdlistener)
        }
    }

    fun removeCmdMessageListener(cmdlistener: CmdMessageListener?) {
        cmdlistener?.let {
            msgCmdListeners.remove(cmdlistener)
        }
    }

    interface CmdMessageListener {
        fun onMessageReceived(cmd: Int, data: String)
    }

    private var msgListeners = synchronizedList<MessageListener>(mutableListOf<MessageListener>())

    fun addMessageListener(listener: MessageListener?) {
        listener?.let {
            msgListeners.add(listener)
        }
    }

    fun removeMessageListener(listener: MessageListener?) {
        listener?.let {
            msgListeners.remove(listener)
        }
    }

    /**
     * 消息通知接口
     */
    interface MessageListener {

        /**
         * 接受消息接口，在接受到文本消息，图片，视频，语音，地理位置，文件这些消息体的时候，会通过此接口通知用户。
         */
        fun onMessageReceived(messages: List<ChatMessage>, isOffline: Boolean = false)

        /**
         * 区别于{#onMessageReceived(List<ChatMessage> messages)}, 这个接口只包含命令的消息体，包含命令的消息体通常不对用户展示。
         */
        fun onCmdMessageReceived(messages: List<ChatMessage>)

        /**
         * 接受到消息体的已读回执, 消息的接收方已经阅读此消息。
         */
        fun onMessageRead(messages: List<ChatMessage>)

        /**
         * 收到消息体的发送回执，消息体已经成功发送到对方设备。
         */
        fun onMessageDelivered(messages: List<ChatMessage>)

        /**
         * 消息变化通知
         */
        fun onMessageChanged(messages: List<ChatMessage>)

        /**
         * 阅后即焚删除消息通知
         */
        fun onMessageDelete(sid: String?, mid: String)

        /**
         * 接收到自己从其他页面发送过来的新消息
         */
        fun onOneselfSendMessage(messages: List<ChatMessage>)

        /**
         * 消息撤回通知
         */
        fun onMessageRecall(messages: ChatMessage)

        /**
         * 文件消息上传进度回调
         */
        fun onFileMessageUpProgress(msg: ChatMessage, upProgress: Int)
    }

    /**
     *  撤回某条消息
     */
    fun recallMessage(msg: ChatMessage) {
        val recallMessage = MessageBuilder.createRecallMessage(msg)
        IMClient.sendObservable(Cmd.ImpSendMsgReq, recallMessage.toReq())
                .map { it.data!!.fromJson<SendMsgRsp>() }
                .check()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeEx {
                    var m = MessageService.findById(msg.mid) ?: return@subscribeEx
                    onRecallMessage(m, recallMessage.fromid)
                }
    }

    /**
     * 获取历史消息列表
     */
    fun getRoamingMessage(
            sid: String,
            startTime: Long,
            size: Int,
            callback: DataCallback<MutableList<ChatMessage>>
    ) {
        val req = GetMessageReq(sid = sid, t = startTime, n = size)
        getRoamingMessage(sid, startTime, size)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess(it)
                }, {
                    var e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }


    fun getRoamingMessage(
            sid: String,
            startTime: Long,
            size: Int
    ): Single<MutableList<ChatMessage>> {
        val req = GetMessageReq(sid = sid, t = startTime, n = size)
        return IMClient.sendObservable(Cmd.ImpGetMessageReq, req)
                .map {
                    val resp = it.data!!.fromJson<GetMessageRsp>()
                    if (resp.isSuccess()) {
                        resp.messages?.forEach { it.preProcess() }
                        if (resp.messages != null && resp.messages?.size ?: 0 >= 1) {
                            val msg = resp.messages?.takeOrNull(0)
                            msg?.let {
                                if (MessageService.findById(msg?.mid) != null) {
                                    resp.messages?.removeAt(0)
                                }
                            }
                        }
                        resp.messages?.forEach {
                            it.sendStatus = ChatMessage.SEND_STATUS_SUCCESS
                            it.flag = it.flag or ChatMessage.FLAG_READ
                        }
                        val messages = resp.messages
                        val msgs =
                                messages?.filter { m -> m.isReceiptToMessage().not() }?.toMutableList()

                        MessageService.saveHistoryMsgs(msgs)
                        //处理回执消息
                        messages?.filter { m -> m.isReceiptToMessage() }
                                ?.forEach { m -> handleReceiptMessage(m, false) }


                        //TODO获取历史消息不加载资源数据
//                        resp.messages?.forEach { downloadResAndNotice(it) }
                        return@map msgs ?: mutableListOf()
                    } else {
                        throw IMException.create(ResultCode.ERR_UNKNOWN)
                    }
                }
    }

    /**
     * 从数据库搜索消息
     */
    fun searchMessage(sid: String, keyword: String, startTime: Long, pageSize: Int): MutableList<ChatMessage> {
        return MessageService.searchMessage(sid, keyword, startTime, pageSize)
                .filter { !it.isRecallMessage() && !it.isRecall() }.toMutableList()
    }

    /**
     * 从数据库搜索文件消息
     */
    fun searchMessageFile(
            keyword: String, pageSize: Int, offset: Long = 0, sid: String = ""
    ): MutableList<MessageFile> {
        if (sid.isNullOrBlank()) {
            return MessageService.searchMessageFile(keyword, pageSize, offset)
        } else {
            return MessageService.searchMessageFileBySid(sid, keyword, pageSize, offset)
        }

    }

    fun searchMessageCount(sid: String, keyword: String): Long {
        return MessageService.searchMessageCount(sid, keyword)
    }

    /**
     * 获取所有未读消息数
     */
    fun getUnReadMessageCount(): Long {
        return getAllConversations().map { it.getUnReadMsgCount() }.sum()
    }

    /**
     * 获取未读消息数
     */
    fun getUnReadMessageCountWithNoNotice(): Long {
        return if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
            getAllConversations().filter { it.type != SessionType.SYSTEM_NOTICE.value }//排除掉系统通知消息
                    .filter { it.isNotification }//排除掉设置消息免打扰的消息
                    .map { it.getUnReadMsgCount() }.sum()
        } else {
            getAllConversations().filter { it.isNotification }//排除掉设置消息免打扰的消息
                    .map { it.getUnReadMsgCount() }.sum()
        }
    }

    fun searchMessageGroup(keyword: String, pageSize: Int, offset: Long = 0): List<MessageGroup> {
        return MessageService.searchMessageByGroup(keyword, pageSize, offset)
    }

    fun loadMessageUntil(sid: String, mid: String): MutableList<ChatMessage> {
        return MessageService.loadMessageUntil(sid, mid)
    }

    //删除回话
    fun removeConversation(sid: String, deleteMessage: Boolean) {
        MessageService.deleteConversation(sid, deleteMessage)
        triggerConversationListener(sid)
    }

    //删除回话中的聊天记录
    fun removeConversationMessage(sid: String, deleteMessage: Boolean) {
        MessageService.deleteConversationMessage(sid, deleteMessage)
    }

    /**
     * 设置会话置顶或者是接受通知
     */
    fun updateConversationSetting(sid: String, top: Boolean, notify: Boolean) {
        val setting = SessionSetting(if (top) 1 else 0, if (notify) 0 else 1)
        Log.e("json:${gson.toJson(setting)}")
        IMClient.sendObservable(
                Cmd.ImpModifyMyDataReq,
                ModifyMyDataReq(
                        type = MY_DATA_SESSION_SETTING, del = false, key = sid, value = gson.toJson(setting)
                )
        )
                .map { it.data!!.fromJson<ModifyMyDataRsp>() }
                .check()
                .subscribe({
                    IMClient.sessionManager.sessionSettings.put(sid, setting)
                    triggerConversationListener(sid)
                }, {
                    it.printStackTrace()
                })
    }

    /**
     * 设置当前的群聊会话为常用群组 ，或者取消常用群组设置
     * sid 会话名称，ofter 是否设置为常用群组
     */
    fun ofterConversationSetting(sid: String, ofter: Boolean) {
        IMClient.sendObservable(
                Cmd.ImpModifyMyDataReq,
                ModifyMyDataReq(type = MY_DATA_MOST_USE_SESSION, del = !ofter, key = sid)
        )
                .map { it.data!!.fromJson<ModifyMyDataRsp>() }
                .check()
                .subscribe({
                    if (ofter) {
                        IMClient.sessionManager.sessionOfterSettings.add(sid)
                    } else {
                        if (IMClient.sessionManager.sessionOfterSettings.contains(sid))
                            IMClient.sessionManager.sessionOfterSettings.remove(sid)
                    }
                    triggerConversationListener(sid)
                }, {
                    it.printStackTrace()
                })
    }

    /**
     * 获取会话内容
     */
    fun getConversation(sid: String): Conversation? {
        var conv = ConversationService.findById(sid)
                ?: kotlin.run {
                    var msg = MessageService.peekMessage(sid)
                    if (msg == null) {
                        if (sid == IDUtil.createSingleChatId(
                                        IMClient.getCurrentUserId(),
                                        Constants.FILE_HELP_ID
                                )
                        ) {
                            //虚拟一个信息
                            msg = ChatMessage()
                            msg.type = SessionType.FILE_HELP.value
                            msg.t = TimeUtils.getServerTime()
                        } else {
                            return null
                        }
                    }

                    var name = when (msg.type) {
                        SessionType.SESSION_P2P.value -> {
                            IMClient.userManager.getUserById(
                                    IDUtil.parseTargetId(
                                            IMClient.getCurrentUserId(),
                                            sid
                                    )
                            )?.cname
                        }
                        SessionType.FILE_HELP.value -> "文件助手"
                        else -> IMClient.sessionManager.getSessionById(msg.sid)?.title
                    }
                    Conversation(sid, type = msg.type, name = name ?: "", lastMsgDate = msg.t)
                }

        return conv.apply {
            val setting = IMClient.sessionManager.sessionSettings.get(sid) ?: SessionSetting()
            val ofter = IMClient.sessionManager.sessionOfterSettings.contains(sid)
            isNotification = setting.isNotification
            isTop = setting.isTop
            isOfter = ofter
        }
    }

    /**
     * 根据sid和type创建conversation
     */
    fun getConversationWithNullMessage(sid: String, type: Int): Conversation? {
        var conv = ConversationService.findById(sid)
                ?: kotlin.run {
                    var name = when (type) {
                        com.liheit.im.core.bean.SessionType.SESSION_P2P.value -> {
                            IMClient.userManager.getUserById(
                                    IDUtil.parseTargetId(
                                            IMClient.getCurrentUserId(),
                                            sid
                                    )
                            )?.cname
                        }
                        SessionType.FILE_HELP.value -> "文件传输助手"
                        else -> IMClient.sessionManager.getSessionById(sid)?.title
                    }
                    Conversation(
                            sid, type = type, name = name
                            ?: "", lastMsgDate = TimeUtils.getServerTime()
                    )
                }

        return conv.apply {
            val setting = IMClient.sessionManager.sessionSettings.get(sid) ?: SessionSetting()
            val ofter = IMClient.sessionManager.sessionOfterSettings.contains(sid)
            isNotification = setting.isNotification
            isTop = setting.isTop
            isOfter = ofter

        }
    }

    fun getMessageById(mid: String): ChatMessage? {
        synchronized(mCache) {
            var msg = mCache.get(mid)
            if (msg == null) {
                msg = MessageService.findById(mid)
                        ?.apply {
                            if (type != com.liheit.im.core.bean.SessionType.SESSION_P2P.value && isNeedReceipt()) {
                                receiptCount = MessageService.getReceiptedCount(mid, true).toInt()
                                unReceiptCount = MessageService.getReceiptedCount(mid, false).toInt()
                            }
                        }
                if (msg != null) {
                    mCache.put(mid, msg)
                }
            }
            return msg
        }
    }

    fun getMessage(
            sid: String, startMsgId: String?, pageSize: Int, isUp: Boolean = true
    ): MutableList<ChatMessage> {
        return MessageService.findMessage(sid, startMsgId, pageSize, isUp)
    }

    fun getAllMessage(sid: String): MutableList<ChatMessage> {
        return MessageService.findAllMessage(sid)
    }

    fun getAllConversations(): MutableList<Conversation> {
        val cList = mutableListOf<Conversation>()
        ConversationService.findAll().forEach {
            it.apply {
                val setting = IMClient.sessionManager.sessionSettings.get(sid) ?: SessionSetting()
                isNotification = setting.isNotification
                isTop = setting.isTop
                isDelete = IMClient.sessionManager.getGroup(sid)?.type == SessionType.DISSOLVE.value
            }
            if (it.type == SessionType.SESSION_FIX.value || it.type == SessionType.SESSION_DEPT.value
                    || it.type == SessionType.SESSION.value || it.type == SessionType.SESSION_DISC.value) {
                if (IMClient.sessionManager.getSessionById(it.sid) != null) {
                    cList.add(it)
                }
            } else {
                if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
                    if (it.type != SessionType.OFFICIAL_ACCOUNTS.value) {
                        cList.add(it)
                    }
                } else {
                    cList.add(it)
                }
            }
        }
        return cList
    }

    fun searchConversation(
            keyword: String,
            pagesize: Int,
            offset: Long = 0
    ): MutableList<Conversation> {
        return ConversationService.search(keyword, pagesize, offset)
    }

    private var conversationListeners = mutableListOf<ConversationListener>()

    //添加会话消息变动的回调
    fun addConversationChangeListener(listener: ConversationListener) {
        conversationListeners.add(listener)
    }

    fun removeConversationChangeListener(listener: ConversationListener) {
        conversationListeners.remove(listener)
    }

    fun clearConversationChangeListener() {
        conversationListeners.clear()
        conversationListeners = mutableListOf<ConversationListener>()
    }

    /**
     * 发送消息回执，而不是发送回执消息。准确的描述是确认回执消息的回执信息
     */
    fun sendReceiptMsg(msg: ChatMessage) {
        IMClient.sendObservable(Cmd.ImpSendMsgReq, MessageBuilder.createReceipt(msg).toReq())
                .doOnSuccess {
                    val resp = it.data!!.fromJson<SendMsgRsp>()
                    var mid = msg.mid
                    //TODO
                    MessageService.messageAddFlag(mid, ChatMessage.FLAG_RECEIPTED)
                    val message = MessageService.findById(mid)
                    message?.let {
                        onMessageChanged(mutableListOf(it))
                        Log.e("message1  4")
                    }

                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
    }

    fun getMsgReceiptStatus(mid: String): MutableList<ReceiptStatus> {
        return MessageService.getMsgUsersReceiptStatus(mid)
    }

    fun findPage(
            sid: String,
            bodyType: MessageType,
            pageSize: Int,
            offset: Long
    ): MutableList<ChatMessage> {
        return MessageService.findPage(sid, bodyType, pageSize, offset)
    }

    private fun getFilePath(body: MsgBody): String {
        if (body is FileBody) {
            val fileName = "${body?.md5}.${body?.name?.substringAfterLast(".")}"
            Log.e("aaa fileName=${fileName}")
            if (body?.name?.substringAfterLast(".").isNullOrEmpty()) {
                Log.e(">>>>>>>>>>>>>>${body}")
            }
            return when (body.mtype) {
                MessageType.VOICE.value -> getVoiceCacheFile(fileName).absolutePath
                MessageType.IMAGE.value -> getImageCacheFile(fileName).absolutePath
                MessageType.FORWARD.value -> getForwardFile(fileName).absolutePath
                MessageType.VIDEO.value -> getVideoCacheFile(fileName).absolutePath
                MessageType.ANNEX.value -> getFileCacheFile(fileName).absolutePath
                else -> ""
            }
        }
        return ""
    }

    private fun getVideoCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "videos/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getVoiceCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "voices/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getImageCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "images/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getForwardFile(filename: String): File {
        var file = File(getUserCacheDir(), "forward/${filename}")
//        if (!file.parentFile.exists()) {
//            file.parentFile.mkdirs()
//        }
        return file
    }

    private fun getFileCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "files/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getUserCacheDir(): File {
        return File(
                IMClient.context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                "${IMClient.getCurrentUserId()}"
        )
    }

    fun getCollectVideoDirectory(filename: String): String {
        var file = File(getUserCacheDir(), "videos/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file.parentFile.absolutePath
    }

    fun getCollectVoiceDirectory(filename: String): String {
        var file = File(getUserCacheDir(), "voices/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file.parentFile.absolutePath
    }

    /**
     * 根据会话删除会话下所有消息
     */
    fun deleteHistoryMsg(sid: String): Completable {
        return Completable.fromAction {
            MessageService.deleteHistory(sid)
        }
    }

    /**
     * 设置会话草稿
     */
    fun setConverstaionDraft(sid: String, msg: String) {
        ConversationService.updateDraft(sid, msg)
        triggerConversationListener(sid)
    }

    /**
     * 根据消息id数据库删除消息
     */
    fun deleteMsgById(mid: String) {
        MessageService.delete(mid)
    }

    /**
     * 创建一条会话
     */
    fun createConversation(sid: String, convName: String, type: Int) {
        var conv = ConversationService.findById(sid)
        if (conv == null) {
            val conversation = Conversation(
                    sid = sid,
                    name = convName,
                    type = type,
                    lastMessageId = "",
                    lastMsgDate = TimeUtils.getServerTime(),
                    isTop = false,
                    isNotification = false,
                    draft = ""
            )
            ConversationService.insert(conversation)
            triggerConversationListener(sid)
        }
    }

    fun createConversationV(sid: String, convName: String, type: Int): Conversation {
        var conv = ConversationService.findById(sid)
        if (conv == null) {
            val conversation = Conversation(
                    sid = sid,
                    name = convName,
                    type = type,
                    lastMessageId = "",
                    lastMsgDate = TimeUtils.getServerTime(),
                    isTop = false,
                    isNotification = false,
                    draft = ""
            )
            ConversationService.insert(conversation)
            triggerConversationListener(sid)
            return conversation
        } else {
            return conv
        }
    }

    fun deleteFile(mid: String) {
        MessageService.deleteMessageFile(mid)
    }

    /**
     * 删除无痕消息（向服务器发通知，服务器删除）
     */
    fun deleteSecrecyMsg(mid: String, sid: String, noticeuserid: Long): Single<Long>? {
        return IMClient.sendObservable(
                Cmd.ImpGetDeleteMsg,
                DeleteSecrecyMsgReq(mid, sid, noticeuserid)
        )
                .map { it.data!!.fromJson<DeleteSecrecyMsgRsp>() }
                .map { it.result }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    return@doOnSuccess
                }
    }

    /**
     * 检查消息在服务器是否存在
     */
    fun checkSecrecyMsgExist(mids: List<String>): Single<List<SecrecyMsgExistState>>? {
        return IMClient.sendObservable(Cmd.ImpGetCheckMsgExist, CheckSecrecyMsgExistReq(mids))
                .map { it.data!!.fromJson<CheckSecrecyMsgExistRsp>() }
                .map { it.msgs }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    return@doOnSuccess
                }
    }

    //是否可以创建音视频通话房间
    fun createVoiceCallMsg(sid: String, bvideo: Boolean): Single<CreateVoiceCallRspMsg>? {
        return IMClient.sendObservable(
                Cmd.ImpGetCreateVoiceCallReq,
                CreateVoiceCallReqMsg(sid = sid, bvideo = bvideo)
        )
                .map { it.data!!.fromJson<CreateVoiceCallRspMsg>() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .check()
                .doOnSuccess {
                    return@doOnSuccess
                }
    }

    //设置音视频通话房间人员列表
    fun setVoiceRoomMember(
            sid: String,
            roomid: Int,
            ids: MutableList<Long>
    ): Single<SetVoiceRoomMemberRsp>? {
        return IMClient.sendObservable(
                Cmd.ImpSetVoiceRoomMemberReq,
                SetVoiceRoomMemberReq(sid = sid, roomid = roomid, ids = ids)
        )
                .map { it.data!!.fromJson<SetVoiceRoomMemberRsp>() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .check()
                .doOnSuccess {
                    Log.e("aaa setVoiceRoomMember${gson.toJson(ids)}//${gson.toJson(it)}")
                    return@doOnSuccess
                }
    }

    //加入退出音视频通话房间
    fun joinVoiceCall(
            sid: String,
            roomid: Int,
            userid: Long,
            status: Int,
            inviterid: Long
    ): Single<JoinVoiceCallRspMsg>? {
        return IMClient.sendObservable(
                Cmd.ImpJoinVoiceCallReq,
                JoinVoiceCallReqMsg(
                        sid = sid,
                        roomid = roomid,
                        userid = userid,
                        status = status,
                        inviterid = inviterid
                )
        )
                .map { it.data!!.fromJson<JoinVoiceCallRspMsg>() }
                .check()
                .doOnSuccess {
                    Log.e("aaa ImpJoinVoiceCallRsp=${gson.toJson(it)}")
                    return@doOnSuccess
                }
    }

    //获取音视频聊天相关参数
    fun getVoiceTokenMsg(): Single<GetVoiceTokenRspMsg>? {
        return IMClient.sendObservable(Cmd.ImpGetVoiceTokenReq, "")
                .map { it.data!!.fromJson<GetVoiceTokenRspMsg>() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .check()
                .doOnSuccess {
                    return@doOnSuccess
                }
    }

    //获取音视频聊天房间人数
    fun getVoiceRoomMemberMsg(roomid: Int): Single<VoiceRoomMemberRspMsg>? {
        return IMClient.sendObservable(
                Cmd.ImpGetVoiceRoomMemberReq,
                VoiceRoomMemberReqMsg(roomid = roomid)
        )
                .map { it.data!!.fromJson<VoiceRoomMemberRspMsg>() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .check()
                .doOnSuccess {
                    Log.e("aaa GetVoiceRoomMemberMsg${gson.toJson(it)}")
                    return@doOnSuccess
                }
    }

    //获取当前用户是否有音视频通话
    fun getVoiceStateMsg(): Single<Boolean> {
        return IMClient.sendObservable(Cmd.ImpGetVoiceStateReq, "")
                .map { it.data!!.fromJson<GetVoiceStateRspMsg>() }
                .check()
                .map {
                    Log.e("aaa getVoiceStateMsg= ${gson.toJson(it)}")
                    it.let {
                        if (it.talking && it.term != 2) {
                            return@map true
                        }
                    }
                    return@map false
                }
    }

    //设置打开或关闭摄像头
    fun toOpenVideo(roomid: Int, open: Boolean): Single<Boolean> {
        return IMClient.sendObservable(
                Cmd.ImpVoiceCallOpenVideoReq,
                VoiceCallOpenVideoReq(roomid, open)
        )
                .map { it.data!!.fromJson<VoiceCallOpenVideoRsp>() }
                .check()
                .map {
                    it.let { return@map true }
                    return@map false
                }
    }

    //单人视频通话切换到语音通话
    fun switchVideoCall(roomid: Int): Single<Boolean> {
        return IMClient.sendObservable(Cmd.ImpVideoSwitchReq, SwitchVideoReq(roomid))
                .map {
//                Log.e("aaa switchVideoCall=$it")
                    it.let { return@map true }
                }
    }

    //发送加入或退出视频会议消息
    fun joinLeaveMeetingRoomCall(sid: String, status: Int): Single<Boolean> {
        var userid = IMClient.getCurrentUserId()
        return im.sendObservable(
                Cmd.ImpJoinLeaveMeetingRoomReq,
                MeetingJoinLeaveRoomMemberReq(sid = sid, userid = userid, status = status)
        )
                .map { it.data!!.fromJson<MeetingJoinLeaveRoomMemberRsp>() }
                .check()
                .map { return@map it.state }
    }

    //请求视频会议房间信息
    fun getMeetingRoomInfo(sid: String): Single<MeetingRoomInfoRsp> {
        return im.sendObservable(Cmd.ImpGetMeetingRoomInfoReq, MeetingRoomInfoReq(sid = sid))
                .map { it.data!!.fromJson<MeetingRoomInfoRsp>() }
                .check()
                .map { return@map it }
    }

    //同步公众号数据
    fun syncSubscriptionsData() {
        val sids = ConversationService.findSubscriptionConversation()
        Log.e("aaa sids=${gson.toJson(sids)}")
        PkurgClient.officialAccountsGetBySid(sids)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeEx({
                    it.forEach { sub ->
                        val subscription = SubscriptionService.findBySid(sub.sid)
                        subscription?.let {
                            if (sub.logo != it.logo || sub.name != it.name) {
                                Log.e("aaa sub=${gson.toJson(sub)}")
                                it.name = sub.name
                                it.logo = sub.logo
                                SubscriptionService.update(it)
                                val conv = ConversationService.findById(sub.sid)
                                conv?.let {
                                    it.name = sub.name.toString()
                                    ConversationService.update(conv)
                                }
                                triggerConversationListener(it.sid)
                            }
                        }
                    }
                }, {
                    Log.e("aaa syncSubscriptionsData $it")
                })
    }

    fun writeHtml(graphicId:String,content:String){
        try {
            val fs = StorageUtils.getHtmlCacheFile(graphicId)
            val outputStream = FileOutputStream(fs)
            outputStream.write(content.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
