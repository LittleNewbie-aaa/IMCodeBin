package com.liheit.im.core

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.Keep
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.liheit.im.common.ext.AppNameFlag
import com.liheit.im.common.ext.getAppName
import com.liheit.im.core.bean.Config
import com.liheit.im.core.bean.DBConfig
import com.liheit.im.core.bean.User
import com.liheit.im.core.bean.UserInfo
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.core.manager.*
import com.liheit.im.core.protocol.*
import com.liheit.im.core.protocol.contact.ContactNotice
import com.liheit.im.core.protocol.contact.ContactNoticeAck
import com.liheit.im.core.protocol.user.UpdateDevTokenReq
import com.liheit.im.core.protocol.user.UpdateDevTokenRsp
import com.liheit.im.core.service.ConfigService
import com.liheit.im.utils.*
import com.liheit.im.utils.json.fromJson
import com.tencent.mars.xlog.Xlog
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.Exceptions
import io.reactivex.functions.Function3
import io.reactivex.functions.Function8
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger


@SuppressLint("StaticFieldLeak")
/**
 *  IM初始化控制器
 */

object IMClient {

    //IM服务器url配置
    var config: IMConfig = IMConfig()

    private val gson = GsonBuilder().setExclusionStrategies(ProtocolExclusionStrategy()).create()
    lateinit var context: Context

    private var servers: MutableList<Server>? = null

    private val handler: Handler = MyHandler(Looper.getMainLooper())

    internal class MyHandler(lpr: Looper) : Handler() {
        init { }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    private var userData: AccessResp? = null
    var account: String = ""
        get() {
            if (field.isNullOrEmpty()) {
                field = ConfigService.findByKeyAndAccount("account", "com.dx.im.default")?.value ?: ""
            }
            return field
        }
        set(value) {
            field = value
            ConfigService.save(Config("account", "com.dx.im.default", value))
        }

    private var id: Long = 0
        get() {
            if (field == 0L) {
                field = ConfigService.findByKeyAndAccount("id", "com.dx.im.default")?.value?.toLongOrNull() ?: 0
            }
            return field
        }
        set(value) {
            field = value
            ConfigService.save(Config("id", "com.dx.im.default", value.toString()))
        }

    private var deviceToken: String = ""
        get() {
            if (field.isNullOrEmpty()) {
                field = ConfigService.findByKeyAndAccount("deviceToken", "com.dx.im.default")?.value ?: ""
            }
            return field
        }
        set(value) {
            field = value
            ConfigService.save(Config("deviceToken", "com.dx.im.default", value))

        }
    var password: String = ""
        get() {
            if (field.isNullOrEmpty()) {
                field = ConfigService.findByKeyAndAccount("password", "com.dx.im.default")?.value ?: ""
            }
            return field
        }
        set(value) {
            field = value
            ConfigService.save(Config("password", "com.dx.im.default", value))
        }

    var permission: Powers? = null

    private var userInfo: UserInfo? = null

    //下次登录去要重登录
    fun needRelogin() {
        account = ""
    }

    //获取当前登录用户信息
    fun getCurrentUser(): UserInfo? {
        return userInfo
    }

    //获取当前登录用户id
    fun getCurrentUserId(): Long {
        return id
    }

    //部门信息管理器
    val departmentManager: DepartmentManager by lazy { DepartmentManager(this) }

    //用户信息管理器
    val userManager: UserManager by lazy { UserManager(this) }

    //用户常用数据管理器
    val userDataManager: UserDataManager by lazy { UserDataManager(this) }

    //聊天消息管理器
    val chatManager: ChatManager by lazy { ChatManager(this) }

    //群组消息管理器
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    //文件服务上传下载管理器
    val resourceManager by lazy { ResourceManager() }

    //心跳服务管理
    private val heartbeatManager: HeartbeatManager by lazy { HeartbeatManager(this) }

    internal var operationCallbacks = Collections.synchronizedMap(mutableMapOf<Int, OperationCallback>())
    internal var respCallbacks = Collections.synchronizedMap(mutableMapOf<Int, IMRespCallback>())

    var msgHandlers: MutableList<MsgHandler> = mutableListOf<MsgHandler>()

    private var unProcessedRequest = LinkedBlockingQueue<String>()
    private var requestQueue = LinkedBlockingQueue<Commond>()
    private var responseQueue = LinkedBlockingQueue<CommondResp>()

    private var receiveThread: MsgReceiveThread? = null

    private val userStatus = BehaviorSubject.createDefault<Int>(LogoutReq.OFFLINE)

    //注册app更新时间
    private val upgradeEvent = PublishSubject.create<UpgradeInfo>()

    //app更新监听回调
    fun upgradeListener(): Observable<UpgradeInfo> =
            upgradeEvent.distinctUntilChanged { o, n -> o.equals(n) }.observeOn(AndroidSchedulers.mainThread())

    var needInit = true
    var isLogined = false
    var firstLogin = true

    private var msgHandler = object : MsgHandler {
        override fun getHandlerType(): List<Int> {
            return arrayListOf(
                    Cmd.ImpLoginRsp,
                    Cmd.ImpLogoutRsp,
                    Cmd.ImpContactNotice
            )
        }

        override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
            when (cmd) {
                Cmd.ImpContactNotice -> {
                    val notice = data.fromJson<ContactNotice>()
                    //TODO 更新通讯录
                    departmentManager.serverLastUpdateTime = notice.dtime
                    departmentManager.serverDepUserLastUpdateTime = notice.dutime
                    userManager.serverLastUpdateTime = notice.utime
                    //发送回执
                    sendACK(Cmd.ImpContactNoticeAck, ContactNoticeAck())
                    //同步通讯录
                    syncUserAndDep()
                }
                Cmd.ImpLogoutRsp -> {
                    var resp = data.fromJson<LogoutRsp>()
//                    Log.e("aaa ${gson.toJson(resp)}")
                    when (resp.status) {
                        LogoutReq.KICKED -> {
                            account = ""
                            needInit = true
                            socketConnected = false
                            onStop()
//                            password = ""
//                            stopReconnectTask()
                            ChatService.stopReconnect(context)
                            ChatService.stopHeartbeat(context)
//                            heartbeatManager.stop()
                            userManager.stopSyncStatusTask()
                        }
                        LogoutReq.MANAGEKICKOUT -> {
                            needInit = true
                            onStop()
                            account = ""
                            id = 0
                        }
                        LogoutReq.ONLINE -> {
//                            heartbeatManager.start()
                            ChatService.startHeartbeat(context)
                            userManager.startSyncStatusTask()
                            if (needInit) {
//                                chatManager.getOfflineMsg()
                                needInit = false
                            }
                        }
                        LogoutReq.LEAVE -> {
                        }
                        LogoutReq.DISABLED -> {
                        }
                        LogoutReq.LOGIN -> {
                        }
                        LogoutReq.OFFLINE -> {
                            needInit = true
                            onStop()
//                            account = ""
//                            id = 0
//                            password = ""
                        }
                    }
                    UserState.values().find { it.state == resp.status }?.let {
                        dispatcherUserState(it)
                    }
                    runOnMainThread {
                        operationCallbacks.remove(sendNumber)?.onSuccess()
                    }
                }
            }
        }
    }

    //数据同步
    @SuppressLint("CheckResult")
    private fun syncUserAndDep() {
        //励信通过java服务器同步数据
        var syncUser = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.userManager.getNewAllUserInfo(true).toSingleDefault(true).toObservable()
        } else {
            IMClient.userManager.getAllUserInfo().toSingleDefault(true).toObservable()
        }
        var syncDep = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.departmentManager.getNewDepartment(true).toSingleDefault(true).toObservable()
        } else {
            IMClient.departmentManager.getDepartmentForServer().toSingleDefault(true).toObservable()
        }
        var syncDepAndUser = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.departmentManager.getNewDepartmentUserListData(true).toSingleDefault(true).toObservable()
        } else {
            IMClient.departmentManager.getDepartmentUserForServer().toSingleDefault(true).toObservable()
        }
        Observable.zip(syncUser, syncDep, syncDepAndUser,
                Function3<Any, Any, Any, Any> { t1, t2, t3 -> 1 })
                .retry()
                .subscribeOn(Schedulers.io())
                .map {
                    if(getAppName() == AppNameFlag.THE_LX_FLAG.value
                            ||getAppName() == AppNameFlag.THE_XY_FLAG.value){
                        departmentManager.getNewPermission()
                    }else{
                        departmentManager.getPermission()
                    }
                    it
                }
                .subscribe({
                    println("同步成功")
                }, {
                    println("同步失败")
                })
    }

    private fun onStop() {
        IMNetty.instance().stop()
//        SnUtil.reset()
    }

    //初始化
    fun init(context: Context, debug: Boolean) {

        initLog(context, debug)
        this.context = context.applicationContext
        //数据库初始化
        DbUtils.init(context)

        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        context.applicationContext.registerReceiver(NetworkReceiver(), intentFilter)

        context.registerReceiver(netStatusReceiver, intentFilter)

        msgHandlers.clear()

        msgHandlers.add(msgHandler)
        msgHandlers.add(sessionManager)
        msgHandlers.add(heartbeatManager)
        msgHandlers.add(chatManager)
        msgHandlers.add(departmentManager)
        msgHandlers.add(userManager)
        msgHandlers.add(userDataManager)

        IMNetty.instance()
                .connectState()
                .skip(1)
                .subscribe({
                    if (!it) {
                        onDisconnect()
                    }
                }, {
                    it.printStackTrace()
                })

        //如果记住了账户和密码，直接切换到用户
        if (!account.isNullOrEmpty() && !password.isNullOrEmpty()) {
            DbUtils.switchToUser(context, account)
        }
    }

    //断开连接
    private fun onDisconnect() {
        synchronized(respCallbacks) {
            respCallbacks.keys.forEach { key ->
                respCallbacks.get(key)?.onError(IMException.create(ResultCode.ErrNetNotConnWebapp))
            }
            respCallbacks.clear()
        }
        if (DbUtils.isSetup()) {
            chatManager.resetAllSendingMessage()
        }
    }

    //初始化log工具
    private fun initLog(context: Context, debug: Boolean) {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("marsxlog")
        var logPath =
                Environment.getExternalStorageDirectory().absolutePath + "/log/${context.packageName}"
        // this is necessary, or may cash for SIGBUS
        var cachePath = File(context.getFilesDir(), "/xlog").absolutePath
        //init xlog
//        if (debug) {
        Xlog.appenderOpen(
                Xlog.LEVEL_VERBOSE,
                Xlog.AppednerModeAsync,
                cachePath,
                logPath,
                "IM",
                null
        )
        Xlog.setConsoleLogOpen(true)
//        } else {
//            Xlog.appenderOpen(
//                Xlog.LEVEL_VERBOSE,
//                Xlog.AppednerModeAsync,
//                cachePath,
//                logPath,
//                "IM",
//                null
//            )
//            Xlog.setConsoleLogOpen(false)
//        }
        com.tencent.mars.xlog.Log.setLogImp(Xlog());
    }

    //IM登录服务器
    fun login(account: String, password: String): Observable<Boolean> {
        deviceToken = ""
        return loginInner(account, password)
                .map { true }
                .doOnSuccess {
                    if (it) {
                        isLogined = true
                    }
                }
                .toObservable()
                .subscribeOn(Schedulers.io())
    }

    //退出登录后清除数据
    fun logout() {
        dispatcherUserState(UserState.OFFLINE)
//        this@IMClient.account = ""
        this@IMClient.password = ""
        this@IMClient.id = 0
        userData = null
        servers = null
        userInfo = null
    }

    // 同步刷新数据
    fun syncData(refresh: Boolean = false): Single<Boolean> {
        Log.e("aaa syncData()")
        userManager.startSyncStatusTask()
        //励信通过java服务器同步数据

        //获取用户表
        var syncUser = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.userManager.getNewAllUserInfo(refresh).toSingleDefault(true)
        } else {
            IMClient.userManager.getAllUserInfo(refresh).toSingleDefault(true)
        }
        //获取部门表
        var syncDep = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.departmentManager.getNewDepartment(refresh).toSingleDefault(true)
        } else {
            IMClient.departmentManager.getDepartmentForServer(refresh).toSingleDefault(true)
        }
        //获取部门人员表
        var syncDepAndUser = if (getAppName() == AppNameFlag.THE_LX_FLAG.value||getAppName() == AppNameFlag.THE_XY_FLAG.value) {
            IMClient.departmentManager.getNewDepartmentUserListData(refresh).toSingleDefault(true)
        } else {
            IMClient.departmentManager.getDepartmentUserForServer(refresh).toSingleDefault(true)
        }
        //获取常用联系人列表
        var getMostOftenUser = userDataManager.getMostOftenUsers()
        //获取常用部门列表
        var getMostOftenDeps = userDataManager.getMostOftenDeps()
        //获取全部群组数据
        var syncSession = sessionManager.groupList().toSingleDefault(true)
        //获取群组设置同步信息
        var sessionSetting = sessionManager.getSessionSetting().toSingleDefault(true)
        //获取常用群组列表
        var sessionOfterSetting = sessionManager.getOfterSessionSetting().toSingleDefault(true)
        return Single.zip(syncUser, syncDep, syncDepAndUser,
                getMostOftenUser, getMostOftenDeps, syncSession, sessionSetting, sessionOfterSetting,
                Function8<Any, Any, Any, Any, Any, Any, Any, Any, Any> { t1, t2, t3, t4, t5, t6, t7, t8 -> 1 })
//            Function7<Any, Any, Any, Any, Any, Any, Any, Any> { t1, t2, t3, t4, t5, t6, t7 -> 1 })
                .flatMap {
                    //同步当前会话列表中全部群组信息
                    sessionManager.syncSessionFromServer()
                    if(getAppName()==AppNameFlag.THE_LX_FLAG.value|| getAppName()==AppNameFlag.THE_XY_FLAG.value){
                        //同步公众号数据
                        chatManager.syncSubscriptionsData()
                    }
                    //获取离线消息
                    chatManager.getOfflineMsg()
                }
                .flatMap {
                    //设置用户名（cname）拼音字段
                    userManager.disposeUserPinyin()
                    //获取部门权限请求
                    if(getAppName() == AppNameFlag.THE_LX_FLAG.value
                            ||getAppName() == AppNameFlag.THE_XY_FLAG.value){
                        departmentManager.getNewPermission()
                    }else{
                        departmentManager.getPermission()
                    }
                    //获取同步已读消息
                    chatManager.getChatReadState().toSingleDefault(true)
                }
//                .doOnSuccess {
//                    sessionManager.getAllOfficialAccounts()
//                    sessionManager.getSessionSetting()
//                    sessionManager.getOfterSessionSetting()
//                }
                .doOnSuccess { Log.e("aaa 同步成功") }
    }

    private fun dispatcherUserState(state: UserState) {
        userState.onNext(state)
    }

    //登录
    private fun loginInner(account: String, password: String): Single<LoginRsp> {
        this@IMClient.account = account
        try {
            if (receiveThread?.isAlive == true) {
                receiveThread?.stopThread()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        receiveThread = MsgReceiveThread()
        receiveThread?.start()

        var ip = extractIp(config.host)
        val ePwd = AESUtils.encryptToBase64String(password)
        return Single.create<Boolean> {
            try {
                DbUtils.switchToUser(context, account)
                it.onSuccess(true)
            } catch (e: Exception) {
                e.printStackTrace()
                it.onError(e)
            }
        }
                .flatMap {
                    IMNetty.instance().access(ip, config.port,
                            gson.toJson(AccessReq(account = account,
                                    psw = ePwd, ver = context.versionName)))
                }
                .check()
                .doOnSuccess { data ->
                    Log.e("aaa data=${gson.toJson(data)}")
                    this@IMClient.account = account
                    this@IMClient.password = password
                    this@IMClient.id = data.uid
                    userData = data
                    servers = data.servers
                    val info = UserInfo()
                    info.token = data.token
                    info.cname = data.cname
                    info.ename = data.ename
                    info.sex = data.sex
                    info.uid = data.uid
                    info.upgrade = data.upgrade
                    info.url = data.url
                    info.ver = data.ver
                    info.info = data.info
                    userInfo = info
                    if (data.upgrade != 0) {//检测是否需要更新app
                        val forceUpdate = data.upgrade == 1
                        upgradeEvent.onNext(
                                UpgradeInfo(
                                        ver = data.ver,
                                        url = data.url,
                                        force = forceUpdate,
                                        info = data.info
                                )
                        )
                    }
                    resourceManager.init(account, ePwd, AESUtils.decryptToString(data.token))
                }
                .flatMap {
                    val forceUpdate = it.upgrade == 1
                    if (forceUpdate) {
                        Exceptions.propagate(IMException.create(ResultCode.NEED_UPGRADE_CLIENT))
                    }
                    Single.create<Boolean> { emt ->
                        val conn = servers!!.find { addr ->
                            var ip = extractIp(addr.addr)
                            IMNetty.instance().stop()
                            IMNetty.instance().setMessageHandler(msgCallback)
                            IMNetty.instance().connect(ip, addr.port)
                        }
                        if (conn == null) {
                            emt.onError(RuntimeException("无法连接服务器"))
                        } else {
                            emt.onSuccess(true)
                            isLogined = true
                        }
                    }.flatMap {
                        var req = LoginReq(account = IMClient.account, token = userData!!.token, mac = "", devtoken = deviceToken)
                        //发送登陆命令 登陆后的修改状态
                        sendObservable(Cmd.ImpLoginReq, req)
                    }.map { it.data!!.fromJson<LoginRsp>() }.check()
                }
                .doFinally { firstLogin = false }
                .doOnSuccess {
                    Log.e("aaa it.powers=${gson.toJson(it.powers)}")
                    permission = it.powers
                    dispatcherUserState(UserState.LOGIN)
                    socketConnected = true
                    ChatService.startReconnect(context)
                }
    }

    internal val msgCallback = object : IMNative.Callback {
        override fun onMessage(
                isConnect: Boolean,
                data: String?,
                packageType: Int,
                sendNumber: Int,
                cmd: Int,
                rawData: ByteArray
        ) {
            if (isConnect) {
                val resp = CommondResp(data, packageType, sendNumber, cmd, rawData)
                responseQueue.put(resp)
            } else {
                socketConnected = false
                isLogined = false
                Log.e("连接中断。。。")
                onStop()
                Log.e("stop结束")
//                heartbeatManager.stop()
                ChatService.stopHeartbeat(context)
                userManager.stopSyncStatusTask()
                runOnMainThread {
                    loginCallback?.onError(-1, "连接中断")
                    dispatcherUserState(UserState.OFFLINE)
                }
                //重连
                StateManager.notifySocketState(false)
            }
        }
    }

    //获取真实ip地址
    internal fun extractIp(addr: String): String {
        var ip = addr
        if (addr.startsWith("http://", true)) {
            ip = addr.substring(7)
        } else if (addr.startsWith("https://", true)) {
            ip = addr.substring(8)
        }
        return ip
    }

    private var loginCallback: IMCallBack? = null

    const val PACKETE_TYPE_GSON = 0 //  0 －表示JSON字符串，以’\0’结尾；
    const val PACKETE_TYPE_URL = 1 //  1 －表示url方式下载的JSON字符串，以’\0’结尾；
    const val PACKETE_TYPE_ZIP = 2//  2 －表示内容为ZIP压缩数据。 实际处理中直接转换成了String 减小解析的复杂度

    //向服务器发送命令消息
    internal fun sendObservable(cmd: Int, userData: Any): Single<CommondResp> {
        return Single.create<CommondResp> { emt ->
            send(cmd, userData, object : SimpleIMRespCallback() {
                override fun onSuccess(resp: CommondResp) {
                    super.onSuccess(resp)
                    emt.onSuccess(resp)
                }

                override fun onError(e: IMException) {
                    super.onError(e)
                    emt.onError(e)
                }
            })
        }.subscribeOn(ImSchedulers.send())
    }

    internal fun send(
            cmd: Int, userData: Any, callback: IMRespCallback = object : SimpleIMRespCallback() {}
    ) {
        val json = gson.toJson(userData)
        var sn = SnUtil.genIndex()
        respCallbacks.put(sn, callback)
        doSend(cmd, json, sn, callback)
    }

    internal fun sendACK(cmd: Int, userData: Any) {
        val json = gson.toJson(userData)
        var sn = SnUtil.genIndex()
        doSend(cmd, json, sn, object : SimpleIMRespCallback() {})
    }

    @SuppressLint("CheckResult")
    private fun doSend(cmd: Int, json: String?, sn: Int, callback: IMRespCallback) {
        Log.v("send package ${cmd} ${Cmd.getCmdName(cmd)}  ${json}")
        Single.just(isConnected())
                .flatMap<Boolean> {
                    if (it) {
                        Single.just(true)
                    } else {
                        Log.e("login 连接断开，重连")
                        loginInner(account, password)
                                .flatMap { syncData() }
                                .flatMap { switchStateObservable(UserState.ONLINE) }
                    }.map { true }
                }
                .flatMapCompletable { IMNetty.instance().sendPackage(PACKETE_TYPE_GSON, sn, cmd, json) }
                .subscribe({
                    //                    Log.v("发送成功")
                }, {
                    it.printStackTrace()
                    callback.onError(it.toIMException())
                    respCallbacks.remove(sn)
                })
    }

    internal fun runOnMainThread(run: () -> Unit) {
        handler.post(run)
    }

    fun AtomicInteger.getAndUpdateKt(f: (Int) -> Int): Int {
        var prev: Int
        var next: Int
        do {
            prev = get()
            next = f.invoke(prev)
        } while (!compareAndSet(prev, next))
        return prev
    }

    fun switchState(status: UserState) {
        switchStateObservable(status)
                .subscribeOn(Schedulers.io())
                .subscribeEx {}
    }

    fun switchStateObservable(status: UserState): Single<LogoutRsp> {
        return sendObservable(Cmd.ImpLogoutReq, LogoutReq(status.state))
                .map {
                    it.data!!.fromJson<LogoutRsp>()
                }
                .check()
                .doOnSuccess { resp ->
//                Log.e("aaa it.state=${resp}")
                    UserState.values().find { it.state == resp.status }?.let {
                        dispatcherUserState(it)
                    }
                }
                .doOnSuccess { handlerUserStatus(it) }
                .subscribeOn(Schedulers.io())
    }

    private fun handlerUserStatus(rsp: LogoutRsp) {
        when (rsp.status) {
            LogoutReq.KICKED -> {
                needInit = true
                socketConnected = false
                onStop()
//                            stopReconnectTask()
                ChatService.stopReconnect(context)
                ChatService.stopHeartbeat(context)
//                            heartbeatManager.stop()
                userManager.stopSyncStatusTask()
            }
            LogoutReq.MANAGEKICKOUT -> {
                needInit = true
                onStop()
                account = ""
                id = 0
            }
            LogoutReq.ONLINE -> {
//                            heartbeatManager.start()
                ChatService.startHeartbeat(context)
                userManager.startSyncStatusTask()
                if (needInit) {
//                    chatManager.getOfflineMsg()
                    needInit = false
                }
            }
            LogoutReq.LEAVE -> {
            }
            LogoutReq.DISABLED -> {
            }
            LogoutReq.LOGIN -> {
            }
            LogoutReq.OFFLINE -> {
                needInit = true
                onStop()
            }
        }
    }

    private fun onLogin() {

    }

    @Keep
    interface AccessCallback {

        fun onSuccess(userInfo: UserInfo)

        fun onError(errorCode: Long, errorMessage: String)
    }

    @Keep
    interface IMCallBack {
        fun onSuccess()
        fun onProgress(progress: Int) {}
        fun onError(errorCode: Long, errorMessage: String)
    }

    enum class UserState(val state: Int) {
        //-5-被踢下线;-2-账号禁用;-1-被踢;0-退出(离线);1-登录中;2-在线;3-离开
        MANAGEKICKOUT(-5),
        DISABLED(-2),
        KICKED_OUT(-1),
        OFFLINE(0),
        LOGIN(1),
        ONLINE(2),
        LEAVE(3)
    }

    private val userState = BehaviorSubject.createDefault<UserState>(UserState.OFFLINE)

    fun userStateListener(): Observable<UserState> = userState.hide()

    private var netConnected = false
    private var socketConnected = false
    private var reconnectLock = Semaphore(1)
    private var reconnectTaskDisposable: Disposable? = null


    fun isConnected(): Boolean {
        return IMNetty.instance().isConnected
    }

    fun isLogin(): Boolean {
        return IMNetty.instance().isConnected
    }

    private fun stopReconnectTask() {
        if (!(reconnectTaskDisposable?.isDisposed ?: true)) {
            reconnectTaskDisposable?.dispose()
        }
    }

    @Synchronized
    internal fun onNetworkChange() {
        val isNetOpened = isNetConnection()
        if (isNetOpened && !isLogined && !firstLogin) {
            //TODO 网络打开,如果之前网络是关闭的，那就要触发重连
            checkReconnect()
        } else {
            socketConnected = false
            onDisconnect()
            //TODO 网络断开，是否需要触发相关事件
        }
    }

    fun triggerReconnect() {
        if (!firstLogin)
            checkReconnect()
    }

    @Synchronized
    internal fun checkReconnect() {
        if (!isConnected()) {
            Log.e("login:需要重连 netConnected:${isNetConnection()}  socketConnected:" + "${socketConnected}")
            if (reconnectLock.tryAcquire()) {
                Log.e("login:开始重连 netConnected:${isNetConnection()}  socketConnected:" + "${socketConnected}")
                if (isNetConnection()) {
                    loginInner(account, password)
                            .flatMap { syncData() }
                            .subscribeOn(Schedulers.io())
                            .subscribeEx({
                                switchState(UserState.ONLINE)
                                reconnectLock.release()
                            }, { code, msg ->
                                reconnectLock.release()
                            })
                } else {
                    Log.e("网络断开，等待网络连接后重试")
                    reconnectLock.release()
                }
            }
        } else {
            Log.v("连接状态正常")
        }
    }

    private fun isNetConnection(): Boolean {
        return context.connectivityManager.isNetConnection()
    }

    private var netStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            /*context?.let {
                it.startService(ChatService.createNetChangeIntent(it))
            }

            val connectionManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager  //得到系统服务类
            val networkInfo = connectionManager.getActiveNetworkInfo()
            if (networkInfo != null && networkInfo!!.isConnected) {
                StateManager.notifyNetState(true)
                netConnected = true
                if (userData != null) {
//                    login(null)
                }
            } else {
                netConnected = false
                socketConnected = false
                StateManager.notifyNetState(false)
            }*/
        }
    }


    fun getCurrentUserAccount(): String {
        return account
    }

    fun isLogout(): Boolean {
        return getCurrentUserAccount().isNullOrEmpty() || password.isNullOrEmpty()
    }

    class MsgReceiveThread : Thread("MsgReceiveThread") {
        private var isRunning = false


        fun stopThread() {
            responseQueue.put(CommondResp.NULL)
        }

        //所有消息发送
        override fun run() {
            super.run()
            isRunning = true
            while (isRunning) {
                try {
                    val commondResp = responseQueue.take()
                    if (commondResp == CommondResp.NULL) {
                        isRunning = false
                        return
                    } else {
                        //Log.v("${commondResp}")
                        var packageType = commondResp.packageType
                        var cmd = commondResp.cmd
                        var sendNumber = commondResp.sendNumber
                        var data = commondResp.data
                        var content = ""
                        if (packageType == PackageType.PACKAGE_TYPE_ZIP) {
                            content = ZipUtils.readZip(
                                    StorageUtils.getRandomTempFile(),
                                    commondResp.rawData
                            )
                            commondResp.packageType = PACKETE_TYPE_GSON
                            commondResp.data = content
                        }
                        Log.v(
                                "Rec PackType ${packageType} SendNumber ${sendNumber} cmd ${cmd}(${Cmd.getCmdName(cmd)}) data:${data}"
                        )

                        Schedulers.computation().scheduleDirect {
                            //TODO 如果加入消息超时或者重传机制，这里要考虑同步问题
                            val callback = respCallbacks.remove(sendNumber)
                            if (callback != null) {
                                try {
                                    when (cmd) {
                                        //这里有些命令返回的是错误的
                                        Cmd.ImpModifySessionRsp -> {
                                            loopMessages(
                                                    packageType,
                                                    cmd,
                                                    data!!,
                                                    sendNumber,
                                                    content
                                            )
                                        }
                                        else -> {
                                        }
                                    }
                                    if (commondResp != null) {
                                        callback.onSuccess(commondResp)
                                    }
                                } catch (e: Exception) {
                                    Log.e("handler response error", e)
                                }
                            } else {
                                loopMessages(packageType, cmd, data!!, sendNumber, content)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.w("msgProcessError")
                    e.printStackTrace()
                }
            }

            isRunning = false
        }
    }

    fun loopMessages(packageType: Int, cmd: Int, data: String, sendNumber: Int, content: String) {
        msgHandlers.forEach {
            if (it.getHandlerType().contains(cmd)) {
                if (packageType == PackageType.PACKAGE_TYPE_ZIP) {
                    it.onMessage(content, PackageType.PACKAGE_TYPE_GSON, sendNumber, cmd)
                } else {
                    it.onMessage(data!!, packageType, sendNumber, cmd)
                }
            }
        }
    }

    fun exportDB() {
        IMNetty.instance().stop()
        Log.e("lastUserUpdateTime:${userManager.lastUserUpdateTime}")
        Log.e("lastDepUserUpdateTime:${departmentManager.lastDepUserUpdateTime}")
        Log.e("lastDepUpdateTime:${departmentManager.lastDepUpdateTime}")
        val file = context.getDatabasePath("${account}_msg_database")
        DbUtils.currentDB.convDao().deleteAll()
        DbUtils.currentDB.msgDao().deleteAll()
        DbUtils.currentDB.receiptStatusDao().deleteAll()
        DbUtils.currentDB.sessionDao().deleteAll()
        DbUtils.currentDB.sessionMemberDao().deleteAll()
        DbUtils.currentDB.msgFileDao().deleteAll()
        DbUtils.currentDB.collectDao().deleteAll()
        DbUtils.currentDB.close()
        val config = DBConfig(
                lastUserUpdateTime = userManager.lastUserUpdateTime,
                lastDepUserUpdateTime = departmentManager.lastDepUserUpdateTime,
                lastDepUpdateTime = departmentManager.lastDepUpdateTime
        )
        File("/sdcard/dbConfig.json").bufferedWriter().use {
            it.write(Gson().toJson(config))
        }
        file.copyTo(File("/sdcard/db"), true)
    }

    fun updateDeviceToken(token: String, deviceType: String) {
        deviceToken = token
        sendObservable(Cmd.ImpUpdateDevTokenReq, UpdateDevTokenReq(token, deviceType))
                .map { it.data!!.fromJson<UpdateDevTokenRsp>() }.check()
                .subscribe({
                    Log.e("login token上报成功")
                }, {
                    it.printStackTrace()
                })
    }

    fun refreshContact(): Completable {
        return syncData(true).toCompletable()
    }

    fun canReadUserInfo(user: User?): Boolean {
        return permission?.info?.contains(user?.level) ?: false
    }

    fun canReadUserPhone(user: User?): Boolean {
        return permission?.phone?.contains(user?.level) ?: false
    }

    fun canCommunication(user: User?): Boolean {
        return permission?.session?.contains(user?.level) ?: false
    }

    fun canVideoCommunication(user: User?): Boolean {
        return permission?.vosip?.contains(user?.level) ?: false
    }

    fun getUserInfoPermissions(): IntArray {
        return permission?.info?.toIntArray() ?: intArrayOf()
    }

    private var directoryListeners = Collections.synchronizedList<DirectoryListener>(mutableListOf<DirectoryListener>())

    fun addDirectoryListener(listener: DirectoryListener?) {
        listener?.let {
            directoryListeners.add(listener)
        }
    }

    fun removeDirectoryListener(listener: DirectoryListener?) {
        listener?.let {
            directoryListeners.remove(listener)
        }
    }

    interface DirectoryListener {
        /**
         * 在通讯录刷新完成后，会通过此接口通知用户刷新通讯录。
         */
        fun onDirectoryChanged()
    }
}
