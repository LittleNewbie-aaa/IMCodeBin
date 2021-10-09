package com.liheit.im.core.manager

import android.annotation.SuppressLint
import android.os.Environment
import android.support.v4.util.LruCache
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.liheit.im.common.ext.AppNameFlag
import com.liheit.im.common.ext.getAppName
import com.liheit.im.core.*
import com.liheit.im.core.bean.*
import com.liheit.im.core.http.PkurgClient
import com.liheit.im.core.protocol.*
import com.liheit.im.core.protocol.session.*
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionAdd
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionAddAdmins
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionCreaterID
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionDel
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionDelAdmins
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionExit
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionRemove
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionTextNotice
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionTitle
import com.liheit.im.core.protocol.session.ModifySessionReq.Companion.ModifySessionType
import com.liheit.im.core.service.ConversationService
import com.liheit.im.core.service.MessageService
import com.liheit.im.core.service.SessionService
import com.liheit.im.utils.*
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.regex.Pattern


/**
 * 群组消息管理器
 */
@SuppressLint("CheckResult")
class SessionManager(private var im: IMClient) : MsgHandler {

    private var cacheDir: File
    private var lastSessionUpdateTime: Long by DBConfigDelegates<Long>("lastGroupUpdateTime", 0)

    val MAX_MSG_CACHE_SIZE = 1024
    private var mCache = LruCache<String, Session>(MAX_MSG_CACHE_SIZE)

    //会话设置同步（是否置顶与是否设置消息免打扰）
    var sessionSettings = mutableMapOf<String, SessionSetting>()

    //常用群组数据
    var sessionOfterSettings = mutableListOf<String>()

    init {
        cacheDir = im.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

        //获取缓存中的数据
        var sessionSetData = SharedPreferencesUtil.getInstance(IMClient.context)
                .getSP("${IMClient.account}sessionSettings")
        if (!sessionSetData.isNullOrEmpty()) {
            sessionSettings = sessionSetData.fromJson<MutableMap<String, Any>>().mapValues {
                it.value.toString().fromJson<SessionSetting>()
            }.toMutableMap()
        }
        var sessionOfter = SharedPreferencesUtil.getInstance(IMClient.context)
                .getSP("${IMClient.account}sessionOfterSettings")
        if (!sessionOfterSettings.isNullOrEmpty()) {
            sessionOfterSettings = sessionOfter.fromJson<MutableList<String>>()
        }
    }

    //添加命令筛选控制 如果有，就执行对命令的处理，没有就过滤掉
    override fun getHandlerType(): List<Int> {
        return mutableListOf<Int>(
                Cmd.ImpCreateSessionNotice,
                Cmd.ImpModifySessionNotice,
                Cmd.ImpModifySessionRsp,
                Cmd.ImpGetSessionMemberRsp,
                Cmd.ImpGetSessionMemberRsp,
                Cmd.ImpModifyMyDataNotice
        )
    }

    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        if (data == null) return
        when (cmd) {
            Cmd.ImpGetSessionMemberRsp -> {
                val resp = data.fromJson<GetSessionerMemberRsp>()
                if (resp.isSuccess()) {
                    SessionService.updateSessionMember(resp.sid, resp.ids ?: mutableListOf())
                    IMClient.runOnMainThread {
                        memberCallback.get(resp.sid)?.forEach { it.onSuccess(resp.ids) }
                        memberCallback.get(resp.sid)?.clear()
                    }
                } else {
                    IMClient.runOnMainThread {
                        memberCallback.get(resp.sid)?.forEach {
                            it.onError(resp.result, ResultCode.formatResultCode(resp.result))
                        }
                        memberCallback.get(resp.sid)?.clear()
                    }
                }
            }
            Cmd.ImpCreateSessionRsp -> {
                val resp = data.fromJson<CreateSessionRsp>()
                if (resp.isSuccess()) {
                    val session = SessionService.findById(resp.sid)
                    session?.let {
                        SessionService.save(it)
                        IMClient.runOnMainThread {
                            callbacks.remove(sendNumber)?.onSuccess()
                            sessionChangeListeners.forEach { it.onCreateSession(session) }
                        }
                    }
                } else {
                    IMClient.runOnMainThread {
                        callbacks.remove(sendNumber)
                                ?.onError(resp.result, ResultCode.formatResultCode(resp.result))
                    }
                }
            }
            Cmd.ImpModifySessionRsp,
            Cmd.ImpModifySessionNotice
            -> {
                Log.e("aaa ImpModifySessionNotice $data")
                val notice = data.fromJson<ModifySessionNotice>()
                Single.just(notice)
                        .flatMap {
                            val session = SessionService.findById(notice.sid)
                            if (session == null) {
                                getSessionFromServer(notice.sid)
                                        .doOnSuccess { SessionService.saveSessionAndMember(it) }
                            } else {
                                Single.just(session)
                            }
                        }
                        .subscribe({ session ->
                            if ((notice.flag and ModifySessionTitle) == ModifySessionTitle) {
                                session.title = notice.title
                                addLocalNotice(session, notice)
                                SessionService.updateSessionTitle(session)
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onTitleChange(notice.sid, notice.title)
                                    }
                                    IMClient.chatManager.triggerConversationListener(session.sid)
                                }
                            }
                            if (notice.flag and ModifySessionAdd == ModifySessionAdd) {
                                syncSessionFromServer(notice.sid)
//                            notice.adds?.let { SessionService.addMember(notice.sid, it) }
                                addLocalNotice(session, notice)
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onMemberJoined(notice.sid, notice.adds)
                                    }
                                }
                            }
                            if (notice.flag and ModifySessionAddAdmins == ModifySessionAddAdmins) {
                                session?.let {
                                    val admins = mutableListOf<Long>()
                                    SessionService.getSessionAdmins(it.sid)?.let { admins.addAll(it) }
                                    notice.admins?.let { admins.addAll(it) }
                                    it.admins = admins

                                    SessionService.setAdmins(sid = it.sid, adminIds = admins)
                                }
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onAdminChange(
                                                notice.sid,
                                                notice.admins
                                        )
                                    }
                                }
                            }

                            if (notice.flag and ModifySessionDelAdmins == ModifySessionDelAdmins) {
                                session?.let {
                                    val admins = mutableListOf<Long>()
                                    SessionService.getSessionAdmins(it.sid)?.let { admins.addAll(it) }
                                    admins.removeAll { notice.admins?.contains(it) ?: false }
                                    it.admins = admins
                                    SessionService.setAdmins(sid = it.sid, adminIds = admins)
                                }
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onAdminChange(notice.sid, notice.admins)
                                    }
                                }
                            }
                            if (notice.flag and ModifySessionCreaterID == ModifySessionCreaterID) {
                                session?.let {
                                    it.cid = notice.cid
                                    SessionService.save(it)
                                }
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onOwnerChanged(
                                                notice.sid,
                                                notice.cid
                                        )
                                    }
                                }
                            }

                            if (notice.flag and ModifySessionDel == ModifySessionDel) {
                                if (notice.dels?.contains(IMClient.getCurrentUserId()) == true) {
                                    SessionService.delete(sid = session.sid)
                                    IMClient.chatManager.triggerConversationListener(session.sid)
                                } else {
                                    SessionService.deleteMember(
                                            session.sid, notice.dels ?: mutableListOf()
                                    )
                                }
                                addLocalNotice(session, notice)
                                runMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onMemberExited(notice.sid, notice.uid, notice.dels)
                                    }
                                }
                            }

                            if (notice.flag and ModifySessionType == ModifySessionType) {
                                onSessionTypeChange(session, notice.type)
                                addLocalNotice(session, notice)
                            }

                            if (notice.flag and ModifySessionExit == ModifySessionExit) {
                                SessionService.deleteMember(notice.sid, mutableListOf(notice.uid))
                                addLocalNotice(session, notice)
                                if (notice.uid == IMClient.getCurrentUserId()) {
                                    ConversationService.delete(notice.sid)
                                }
                                IMClient.runOnMainThread {
                                    sessionChangeListeners.forEach {
                                        it.onMemberExited(notice.sid, notice.uid, mutableListOf(notice.uid))
                                    }
                                }
                            }

                            if (notice.flag and ModifySessionRemove == ModifySessionRemove) {
                                onSessionRemove(notice.sid, notice.uid)
                            }

                            if (notice.flag and ModifySessionTextNotice == ModifySessionTextNotice) {
                                SessionService.findById(notice.sid)?.let { session ->
                                    if (notice.notice.isNotEmpty()) {
                                        var affiche = notice.notice.fromJson<SessionNotice>()
                                        if (affiche.content.isNullOrEmpty()) {
                                            session.notice = ""
                                        } else {
                                            session.notice = notice.notice
                                        }
                                    } else {
                                        session.notice = ""
                                    }
                                    session.utime = notice.utime
                                    SessionService.save(session)
                                    addLocalNotice(session, notice)
                                    IMClient.chatManager.triggerConversationListener(session.sid)
                                    runMainThread {
                                        sessionChangeListeners.forEach {
                                            it.onTitleChange(session.sid, session.title)
                                        }
                                    }
                                }
                            }
                            //send ack
                            IMClient.sendACK(Cmd.ImpModifySessionNoticeAck, ModifySessionNoticeAck(notice.sid))
                            runMainThread { callbacks.remove(sendNumber)?.onSuccess() }
                        }, {
                            it.printStackTrace()
                        })
            }
            Cmd.ImpCreateSessionNotice -> {
                val notice = data.fromJson<CreateSessionNotice>()
                val session = notice.toSession()
                SessionService.saveSessionAndMember(session)
                sendCreateSessionNoticeAck(session)
                onCreateSession(session)
            }
            Cmd.ImpModifyMyDataNotice -> {
                val myData = data.fromJson<MyData>()
                when (myData.type) {
                    MY_DATA_MOST_USE_SESSION,
                    MY_DATA_MOST_USE_DEPARTMENT -> {
                        if (!myData.del!!) {
                            sessionOfterSettings.add(myData.key)
                        } else {
                            if (sessionOfterSettings.contains(myData.key))
                                sessionOfterSettings.remove(myData.key)
                        }
                        IMClient.chatManager.getConversation(myData.key)?.let { conv ->
                            IMClient.chatManager.triggerConversationListener(conv.sid)
                        }
                    }
                    MY_DATA_SESSION_SETTING -> {
                        val sessionSetting = myData.value.fromJson<SessionSetting>()
                        sessionSettings[myData.key] = sessionSetting
//                        Log.e("aaa sessionSettings=${sessionSettings[myData.key]}")
                        IMClient.chatManager.getConversation(myData.key)?.let { conv ->
                            IMClient.chatManager.triggerConversationListener(conv.sid)
                        }
                        SessionService.findById(myData.key)?.let { session ->
                            IMClient.sessionManager.sessionChangeNotice(session.sid, session.title)
                        }
                    }
                }
            }
        }
    }

    /**
     * 解散群处理
     */
    private fun onSessionRemove(sid: String, operator: Long) {
        if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
            SessionService.delete(sid)
            ConversationService.delete(sid)
            IMClient.chatManager.triggerConversationListener(sid)
            runMainThread { sessionChangeListeners.forEach { it.onSessionRemove(sid) } }
        } else {
            val session = SessionService.findById(sid)
            if (session != null) {
                session.type = SessionType.DISSOLVE.value
                SessionService.save(session)
                val conversation = IMClient.chatManager.getConversation(sid)
                if (conversation != null && !conversation.isDelete) {
                    conversation.draft = ""
                    conversation.type = SessionType.DISSOLVE.value
                    conversation.isDelete = true
                    ConversationService.update(conversation)
                }
                runMainThread { sessionChangeListeners.forEach { it.onSessionRemove(sid) } }
            }
        }
    }

    private fun addLocalNotice(session: Session, notice: ModifySessionNotice) {
        /*val noticeBody = when (notice.flag) {
            ModifySessionTitle -> LocalNotice(LocalNotice.EDIT_TITLE)
            ModifySessionDel -> LocalNotice(LocalNotice.REMOVE_MEMBER, notice.dels
                    ?: mutableListOf())
            ModifySessionAdd -> LocalNotice(LocalNotice.ADD_MEMBER, notice.adds ?: mutableListOf())
            ModifySessionExit -> LocalNotice(LocalNotice.MEMBER_EXIT, mutableListOf(notice.uid))
            ModifySessionType -> LocalNotice(noticeType = LocalNotice.CHANGE_TYPE, newType = notice.type)
            else -> null
        }
        noticeBody?.let {
            var localNotice = ChatMessage(
                    mid = IDUtil.generatorMsgId(),
                    t = TimeUtils.getServerTime(),
                    sid = session.sid,
                    type = session.type,
                    fromid = notice.uid,
                    bodyType = MessageType.SESSION_CHANGE.value,
                    flag = ChatMessage.FLAG_READ,
                    sendStatus = ChatMessage.SEND_STATUS_SUCCESS,
                    msgs = arrayListOf(noticeBody)
            )
            onGenLocalNotice(localNotice)
        }*/
    }

    private fun onGenLocalNotice(localNotice: ChatMessage) {
        MessageService.save(localNotice)
        IMClient.chatManager.onMessageChange(localNotice)
    }

    private fun addLocalNotice(session: Session, notice: ModifySessionRsp) {

        /*val noticeBody = when (notice.flag) {
            ModifySessionTitle -> LocalNotice(LocalNotice.EDIT_TITLE)
            ModifySessionDel -> LocalNotice(LocalNotice.REMOVE_MEMBER, notice.dels
                    ?: mutableListOf())
            ModifySessionAdd -> LocalNotice(LocalNotice.ADD_MEMBER, notice.adds ?: mutableListOf())
            ModifySessionExit -> LocalNotice(LocalNotice.MEMBER_EXIT, mutableListOf(notice.cid))
            ModifySessionType -> LocalNotice(noticeType = LocalNotice.CHANGE_TYPE,newType = notice.type)
            else -> null
        }
        noticeBody?.let {
            var localNotice = ChatMessage(
                    mid = IDUtil.generatorMsgId(),
                    t = TimeUtils.getServerTime(),
                    sid = session.sid,
                    type = session.type,
                    fromid = notice.uid,
                    bodyType = MessageType.SESSION_CHANGE.value,
                    flag = ChatMessage.FLAG_READ,
                    sendStatus = ChatMessage.SEND_STATUS_SUCCESS,
                    msgs = arrayListOf(noticeBody)
            )
            onGenLocalNotice(localNotice)
        }*/
    }

    private fun onSessionTypeChange(session: Session, newSessionType: Int) {
        session.type = newSessionType
        SessionService.save(session)
        ConversationService.updateType(session.sid, newSessionType)
        IMClient.chatManager.triggerConversationListener(session.sid)
        runMainThread {
            sessionChangeListeners.forEach {
                it.onSessionTypeChanged(
                        session.sid,
                        newSessionType
                )
            }
        }
    }

    private fun runMainThread(run: () -> Unit) {
        IMClient.runOnMainThread(run)
    }

    private fun sendCreateSessionNoticeAck(session: Session) {
        IMClient.sendACK(Cmd.ImpCreateSessionNoticeAck, CreateSessionNoticeAck(session.sid))
    }

    private fun onCreateSession(session: Session) {
        IMClient.runOnMainThread {
            synchronized(sessionChangeListeners) {
                sessionChangeListeners.forEach { it.onCreateSession(session) }
            }
        }
    }

    //创建群
    fun createSession(title: String, userIds: MutableList<Long>): Single<Session> {
        var sid = IDUtil.generatorSessionId()
        var userId = IMClient.getCurrentUserId()
        if (!userIds.contains(userId)) {
            userIds.add(userId)
        }
        if (userIds.size < 3)
            return Single.error<Session>(RuntimeException("群人数至少三人"))

        if (userIds.size > 500) {
            return Single.error<Session>(RuntimeException("群人员不能超过500人"))
        }

        var type = com.liheit.im.core.bean.SessionType.SESSION_DISC.value
        if (userIds.size > 50) {
            type = com.liheit.im.core.bean.SessionType.SESSION.value
        }

        var createTime = TimeUtils.getServerTime()
        val sessionReq = CreateSessionReq(
                sid = sid, type = type, cid = userId,
                title = title, ctime = createTime, ids = userIds
        )

        return IMClient.sendObservable(Cmd.ImpCreateSessionReq, sessionReq)
                .map {
                    val resp = it.data!!.fromJson<CreateSessionRsp>()
                    if (resp.isSuccess()) {
                        var s = Session(sid = resp.sid, type = resp.type, title = resp.title, cid = resp.cid,
                                ctime = resp.ctime, utime = resp.utime, admins = resp.admins?.toMutableList(),
                                ids = resp.ids?.toMutableList())
                        return@map s
                    } else {
                        throw IMException.create(ResultCode.ERR_UNKNOWN)
                    }
                }
                .doOnSuccess { s ->
                    SessionService.saveSessionAndMember(s)
                    IMClient.runOnMainThread {
                        sessionChangeListeners.forEach { it.onCreateSession(s) }
                    }
                    IMClient.chatManager.createConversation(s.sid, s.title, s.type)
                }
                .subscribeOn(Schedulers.io())
    }

    //获取会话设置（群同步与是否置顶）
    fun getSessionSetting(): Completable {
        return IMClient.sendObservable(Cmd.ImpGetMyDataListReq, GetMyDataListReq(0, MY_DATA_SESSION_SETTING))
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .map {
                    it.values?.let {
                        SharedPreferencesUtil.getInstance(IMClient.context)
                                .putSP("${IMClient.account}sessionSettings", gson.toJson(it))
                    }
                    it.values?.mapValues {
                        it.value.toString().fromJson<SessionSetting>()
                    } ?: mutableMapOf()
                }
                .doOnSuccess {
                    sessionSettings.clear()
                    sessionSettings.putAll(it)
                }.doOnError {
                }.toCompletable()
    }

    //获取常用群组数据列表
    fun getOfterSessionSetting(): Completable {
        return IMClient.sendObservable(
                Cmd.ImpGetMyDataListReq,
                GetMyDataListReq(0, MY_DATA_MOST_USE_SESSION)
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .map {
                    it.values?.mapValues { it.value.toString().fromJson<SessionOfterSetting>() }
                            ?: mutableMapOf()
                }
                .doOnSuccess {
                    sessionOfterSettings.clear()
                    sessionOfterSettings.addAll(it.keys)
                    SharedPreferencesUtil.getInstance(IMClient.context)
                            .putSP("${IMClient.account}sessionOfterSettings", gson.toJson(sessionOfterSettings))
                }.toCompletable()
    }

    fun updateSetting(sid: String, top: Boolean, notify: Boolean) {
        val setting = SessionSetting(if (top) 1 else 0, if (notify) 0 else 1)
        IMClient.sendObservable(Cmd.ImpModifyMyDataReq,
                ModifyMyDataReq(type = MY_DATA_SESSION_SETTING,
                        del = false, key = sid, value = gson.toJson(setting)))
                .map { it.data!!.fromJson<ModifyMyDataRsp>() }
                .check()
                .subscribe({
                    sessionSettings.put(sid, setting)
                }, {
                    it.printStackTrace()
                })
    }

    private var sessionChangeListeners = mutableListOf<SessionChangeListener>()
    fun addSessionChangeListener(listener: SessionChangeListener?) {
        listener?.let {
            sessionChangeListeners.add(listener)
        }
    }

    fun removeSessionChangeListener(listener: SessionChangeListener?) {
        listener?.let {
            sessionChangeListeners.remove(listener)
        }
    }

    fun sessionChangeNotice(sid: String, title: String) {
        runMainThread {
            sessionChangeListeners.forEach { it.onTitleChange(sid, title) }
        }
    }

    fun getSessionFromServer(id: String): Single<Session> {
        return IMClient.sendObservable(
                Cmd.ImpGetSessionListReq,
                GetSessionListReq(sids = mutableListOf(id))
        )
                .map { it.data?.fromJson<GetSessionListRsp>()?.sessions?.get(0) }
    }

    //同步当前会话列表中全部群组信息
    fun syncSessionFromServer() {
        var sIds = IMClient.chatManager.getAllConversations()
                .filter {
                    it.type == SessionType.DISSOLVE.value || it.type == SessionType.SESSION_FIX.value
                            || it.type == SessionType.SESSION_DEPT.value || it.type == SessionType.SESSION.value
                            || it.type == SessionType.SESSION_DISC.value
                }
                .map { it.sid }.toMutableList()
        IMClient.sendObservable(Cmd.ImpGetSessionListReq, GetSessionListReq(sids = sIds))
                .scheduler()
                .subscribeEx {
                    val pkg = it.data!!.fromJson<GetSessionListRsp>()
                    if (pkg.isSuccess()) {
                        if (pkg.sessions != null) {
//                        Log.e("aaa pkg.sessions=${gson.toJson(pkg.sessions)}")
                            SessionService.saveSessionAndMember(pkg.sessions!!)
                        }
                    }
                }
    }

    //同步某个群组信息
    fun syncSessionFromServer(sid: String) {
        IMClient.sendObservable(
                Cmd.ImpGetSessionListReq,
                GetSessionListReq(sids = mutableListOf(sid))
        )
                .scheduler()
                .subscribeEx {
                    val pkg = it.data!!.fromJson<GetSessionListRsp>()
                    if (pkg.isSuccess()) {
                        if (pkg.sessions != null) {
                            SessionService.saveSessionAndMember(pkg.sessions!!)
                            IMClient.chatManager.triggerConversationListener(sid)
                        }
                    }
                }
    }

    //获取会话列表请求
    fun groupList(): Completable {
        return IMClient.sendObservable(Cmd.ImpGetSessionListReq, GetSessionListReq(0))
                .flatMapObservable<Session> {
//                Log.e("aaa lastSessionUpdateTime=$lastSessionUpdateTime   groupList=${gson.toJson(it)}")
                    var packageType = it.packageType
                    var data = it.data!!
                    when (packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            Observable.create<Session> { emt ->
                                val pkg = data.fromJson<GetSessionListRsp>()
                                if (pkg.isSuccess()) {
                                    pkg.sessions?.forEach { emt.onNext(it) }
                                    lastSessionUpdateTime = pkg.utime
                                    emt.onComplete()
                                } else {
                                    emt.onError(IMException.create(pkg.result))
                                }
                            }
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file =
                                    File(File(cacheDir.absolutePath, "sis"), UUID.randomUUID().toString())
                            urlRespToFile(data, file)
                                    .flatMapObservable { f ->
                                        io.reactivex.Observable.create<Session> { emitter ->
                                            val reader = JsonReader(FileReader(f))
                                            val gson = GsonBuilder().create()
                                            reader.beginObject()
                                            var begin = System.currentTimeMillis()
                                            var lastUpdateTime = 0L
                                            while (reader.hasNext()) {
                                                val name = reader.nextName()
                                                Log.d("name ${name}")
                                                when (name) {
                                                    "type" -> lastUpdateTime = reader.nextLong()
                                                    "sessions" -> {
                                                        reader.beginArray()
                                                        while (reader.hasNext()) {
                                                            val session = gson.fromJson<Session>(
                                                                    reader, Session::class.java
                                                            )
                                                            if (!emitter.isDisposed) {
                                                                emitter.onNext(session)
                                                            }
                                                        }
                                                        reader.endArray()
                                                        emitter.onComplete()
                                                    }
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            Log.e("耗时：${System.currentTimeMillis() - begin}")
                                            reader.endObject()
                                            lastSessionUpdateTime = lastUpdateTime
                                            Log.i("lastSessionUpdateTime:${lastUpdateTime}")
                                        }.doFinally {
                                            f.delete()
                                            file.delete()
                                        }
                                    }
                        }
                        else -> Observable.error(java.lang.RuntimeException("协议错误"))
                    }
                }
                .buffer(10000)
                .doOnNext { sessions ->
                    Log.e("aaa sessions.size=${sessions.size}")
                    SessionService.saveSessionAndMember(sessions)
//                IMClient.chatManager.triggerConversationListener("")
                }
                .single(mutableListOf()).toCompletable()
                .subscribeOn(Schedulers.io())
    }

    //获取全部群组
    fun getAllGroup(): MutableList<Session> {
        return SessionService.findAll()
    }

    //根据群组类型获取群组列表
    fun getAllGroupByType(type: Int): MutableList<Session> {
        return SessionService.findAllByType(type)
    }

    //匹配模糊搜索
    fun contains(keyword: String, name: String): Boolean {
        val pattern: Pattern = Pattern.compile(
                CharacterParser.getInstance().getSelling(keyword), Pattern.CASE_INSENSITIVE
        )
        //首字母搜索
        var flag = pattern.matcher(FirstLetterUtil.getFirstLetter(name)).find()
        if (!flag) {
            //全拼搜索（如果搜字母搜索到就不进行全拼搜索了）
            flag = pattern.matcher(CharacterParser.getInstance().getSelling(name)).find()
        }
        return flag
    }

    fun getGroup(sid: String): Session? {
        return SessionService.findById(sid)
    }

    private var memberCallback = mutableMapOf<String, MutableList<IMDateCallback<MutableList<Long>>>>()

    //从服务器中更新会话人员
    fun getSessionMembers(sid: String, callback: IMDateCallback<MutableList<Long>>) {
        IMClient.sendObservable(Cmd.ImpGetSessionMemberReq, GetSessionerMemberReq(sid))
                .map {
                    val resp = it.data!!.fromJson<GetSessionerMemberRsp>()
                    if (resp.isSuccess()) {
                        SessionService.updateSessionMember(resp.sid, resp.ids ?: mutableListOf())
                    } else {
                        throw IMException.create(ResultCode.ERR_UNKNOWN)
                    }
                    return@map resp
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ resp ->
                    callback.onSuccess(resp.ids)
                }, {
                    var e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //从本地数据库中获取会话人员
    fun getSessionMembers(sid: String): List<Long> {
        return SessionService.getSessionMemberIds(sid)
    }

    //从本地数据库中获取会话人员
    fun getSessionMember(sid: String, offset: Long, pageSize: Int): List<Member> {
        return SessionService.getSessionMembers(sid, offset, pageSize)
    }

    //从本地数据库中获取群组管理员
    fun getSessionAdmins(sid: String): List<Long> {
        return SessionService.getSessionAdmins(sid)
    }

    //删除群组
    fun removeSession(sid: String, callback: OperationCallback) {
        Single.just(sid).map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(ModifySessionRemove,
                            IMClient.getCurrentUserId(), type = it.type, sid = sid)
                    IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess { onSessionUpdate(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //更新群组管理员
    fun updateSessionAdmin(sid: String, newAdminId: Long, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(
                            flag = ModifySessionCreaterID, uid = IMClient.getCurrentUserId(),
                            type = it.type, sid = sid, cid = newAdminId
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //群组添加管理员
    fun addAdmin(sid: String, admins: MutableList<Long>): Single<Session> {
        return editSession(sid) {
            ModifySessionReq(
                    flag = ModifySessionAddAdmins,
                    uid = IMClient.getCurrentUserId(),
                    type = it.type,
                    sid = sid,
                    admins = admins
            )
        }
    }

    //群组删除管理员
    fun deleteAdmin(sid: String, admins: MutableList<Long>): Single<Session> {
        return editSession(sid) {
            ModifySessionReq(
                    flag = ModifySessionDelAdmins,
                    uid = IMClient.getCurrentUserId(),
                    type = it.type,
                    sid = sid,
                    admins = admins
            )
        }
    }

    //修改群组信息
    private fun editSession(sid: String, operation: (Session) -> ModifySessionReq): Single<Session> {
        return Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = operation.invoke(it)
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .map { SessionService.findById(sid)!! }
                .subscribeOn(Schedulers.io())
    }

    //修改群组类型
    fun updateSessionType(sid: String, type: Int): Completable {
        return Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(flag = ModifySessionType, uid = IMClient.getCurrentUserId(),
                            type = type, sid = sid, cid = it.cid)
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }.toCompletable()
                .subscribeOn(Schedulers.io())

    }

    private fun processModifySessionResp(resp: CommondResp): ModifySessionRsp {
        val response = resp.data!!.fromJson<ModifySessionRsp>()
        if (!response.isSuccess()) {
            throw IMException.create(ResultCode.ERR_UNKNOWN)
        }
        return response
    }

    private fun onSessionUpdate(response: ModifySessionRsp) {
        if (response.flag and ModifySessionDel == ModifySessionDel) {
            response.dels?.let {
                SessionService.deleteMember(response.sid, it)
            }
            SessionService.findById(response.sid)?.let { addLocalNotice(it, response) }
            IMClient.runOnMainThread {
                sessionChangeListeners.forEach {
                    it.onMemberExited(response.sid, response.uid, response.dels)
                }
            }
        }
//        if (response.flag and ModifySessionRemove == ModifySessionRemove) {
//            onSessionRemove(response.sid)
//        }
        if (response.flag and ModifySessionExit == ModifySessionExit) {
            SessionService.delete(response.sid)
            if (response.uid == IMClient.getCurrentUserId()) {
                ConversationService.delete(response.sid)
            }
            IMClient.runOnMainThread {
                sessionChangeListeners.forEach {
                    it.onMemberExited(
                            response.sid,
                            response.uid,
                            mutableListOf(IMClient.getCurrentUserId())
                    )
                }
            }
        }
        if (response.flag and ModifySessionCreaterID == ModifySessionCreaterID) {
            var session = SessionService.findById(response.sid)
            session?.let {
                it.cid = response.cid
                it.utime = response.utime
                SessionService.save(it)
            }
            IMClient.runOnMainThread {
                sessionChangeListeners.forEach { it.onOwnerChanged(response.sid, response.cid) }
            }
        }
        if (response.flag and ModifySessionAdd == ModifySessionAdd) {
            response.adds?.let {
                SessionService.addMember(response.sid, it)
            }
            SessionService.findById(response.sid)?.let { addLocalNotice(it, response) }
            IMClient.runOnMainThread {
                sessionChangeListeners.forEach { it.onMemberJoined(response.sid, response.adds) }
            }
        }
        if (response.flag and ModifySessionType == ModifySessionType) {
            SessionService.findById(response.sid)?.let { session ->
                onSessionTypeChange(session, response.type)
                addLocalNotice(session, response)
                IMClient.runOnMainThread {
                    sessionChangeListeners.forEach {
                        it.onMemberJoined(
                                response.sid,
                                response.adds
                        )
                    }
                }
            }
        }

        if (response.flag and ModifySessionAddAdmins == ModifySessionAddAdmins) {
            SessionService.findById(response.sid)?.let {
                val admins = mutableListOf<Long>()
                SessionService.getSessionAdmins(response.sid)?.let { ids -> admins.addAll(ids) }
                response.admins?.let { ids -> admins.addAll(ids) }
                it.admins = admins

                SessionService.setAdmins(sid = it.sid, adminIds = admins)
                runMainThread {
                    sessionChangeListeners.forEach {
                        it.onAdminChange(
                                response.sid,
                                admins
                        )
                    }
                }
            }
        }

        if (response.flag and ModifySessionDelAdmins == ModifySessionDelAdmins) {
            SessionService.findById(response.sid)?.let {
                val admins = mutableListOf<Long>()
                SessionService.getSessionAdmins(response.sid)?.let { admins.addAll(it) }
                admins.removeAll { id -> response.admins?.contains(id) ?: false }
                it.admins = admins
                SessionService.setAdmins(sid = it.sid, adminIds = admins)
                runMainThread {
                    sessionChangeListeners.forEach {
                        it.onAdminChange(
                                response.sid,
                                admins
                        )
                    }
                }
            }
        }

        if (response.flag and ModifySessionTitle == ModifySessionTitle) {
            val session = SessionService.findById(response.sid)
            session?.let {
                session.title = response.title
                session.utime = response.utime
                SessionService.save(it)
                ConversationService.updateTitle(session.sid, session.title)
                addLocalNotice(session, response)
                IMClient.chatManager.triggerConversationListener(session.sid)
            }
            IMClient.runOnMainThread {
                sessionChangeListeners.forEach { it.onTitleChange(response.sid, response.title) }
            }
        }

        if (response.flag and ModifySessionTextNotice == ModifySessionTextNotice) {
            SessionService.findById(response.sid)?.let { session ->
                if (response.notice.isNotEmpty()) {
                    var affiche = response.notice.fromJson<SessionNotice>()
                    if (affiche.content.isNullOrEmpty()) {
                        session.notice = ""
                    } else {
                        session.notice = response.notice
                    }
                } else {
                    session.notice = ""
                }
                session.utime = response.utime
                SessionService.save(session)
                addLocalNotice(session, response)
                IMClient.chatManager.triggerConversationListener(session.sid)
                runMainThread {
                    sessionChangeListeners.forEach { it.onTitleChange(session.sid, session.title) }
                }
            }
        }
    }

    fun addUsersToSession(sid: String, ids: List<Long>, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(
                            ModifySessionAdd,
                            IMClient.getCurrentUserId(),
                            type = it.type,
                            sid = sid,
                            adds = ids.toMutableList()
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    /**
     * 扫码加入群聊
     */
    fun addSelfToSession(uid: Long, sid: String, ids: List<Long>, callback: OperationCallback) {
        Single.just(sid)
                .flatMap {
                    val req = ModifySessionReq(
                            ModifySessionAdd,
                            uid = uid,
                            sid = sid,
                            adds = ids.toMutableList()
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .doOnSuccess {}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.data != null && !it.data.isNullOrEmpty()) {
                        var data = it.data!!.fromJson<ResultData>()
                        if (data.result == ResultCode.ErrNoSession) {
                            callback.onError(data.result, "会话不存在")
                        } else {
                            callback.onSuccess()
                        }
                    } else {
                        callback.onSuccess()
                    }
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    fun deleteSessionUser(sid: String, ids: MutableList<Long>, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(ModifySessionDel, IMClient.getCurrentUserId(),
                            type = it.type, sid = sid, dels = ids.toMutableList())
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    fun deleteSessionUser(sid: String, ids: MutableList<Long>): Completable {
        return Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(
                            ModifySessionDel,
                            IMClient.getCurrentUserId(),
                            type = it.type,
                            sid = sid,
                            dels = ids.toMutableList()
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toCompletable()
    }

    fun getSessionById(sid: String): Session? {
        return SessionService.findById(sid)
    }

    //修改群组名称
    fun sessionRename(sid: String, newName: String, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(
                            flag = ModifySessionTitle,
                            uid = IMClient.getCurrentUserId(),
                            title = newName,
                            type = it.type,
                            sid = sid,
                            cid = it.cid
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //退出群组
    fun exitSession(sid: String, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(
                            flag = ModifySessionExit,
                            uid = IMClient.getCurrentUserId(),
                            type = it.type,
                            sid = sid,
                            cid = it.cid
                    )
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //修改群聊公告
    fun sessionNotice(sid: String, notice: String, callback: OperationCallback) {
        Single.just(sid)
                .map { SessionService.findById(it) }
                .flatMap {
                    val req = ModifySessionReq(flag = ModifySessionTextNotice, uid = IMClient.getCurrentUserId(),
                            notice = notice, type = it.type, sid = sid, cid = it.cid)
                    return@flatMap IMClient.sendObservable(Cmd.ImpModifySessionReq, req)
                }
                .map { processModifySessionResp(it) }
                .doOnSuccess {
                    onSessionUpdate(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    callback.onSuccess()
                }, {
                    val e = it.toIMException()
                    callback.onError(e.code, e.message)
                })
    }

    //更新群公告已读同步
    fun updateSessionNoticeTime(sid: String, noticetime: Long, title: String) {
        var setting = sessionSettings[sid]
        if (setting == null) {
            setting = SessionSetting(0, 0, noticetime)
        }else{
            setting.noticetime = noticetime
        }
        IMClient.sendObservable(Cmd.ImpModifyMyDataReq, ModifyMyDataReq(type = MY_DATA_SESSION_SETTING,
                        del = false, key = sid, value = gson.toJson(setting)))
                .map { it.data!!.fromJson<ModifyMyDataRsp>() }
                .check()
                .subscribe({
                    sessionSettings.put(sid, setting)
                    IMClient.sessionManager.sessionChangeNotice(sid, title)
                }, {
                    it.printStackTrace()
                })
    }

    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Session> {
        return SessionService.search(keyword, pageSize, offset)
    }

    private var callbacks = mutableMapOf<Int, OperationCallback>()
}
