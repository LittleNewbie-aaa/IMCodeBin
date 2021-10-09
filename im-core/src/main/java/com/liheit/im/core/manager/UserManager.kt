package com.liheit.im.core.manager

import android.annotation.SuppressLint
import android.os.Environment
import com.github.promeg.pinyinhelper.Pinyin
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.liheit.im.core.*
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.EditAction
import com.liheit.im.core.bean.User
import com.liheit.im.core.http.ApiClient
import com.liheit.im.core.protocol.GetUserInfoReq
import com.liheit.im.core.protocol.GetUserInfoRsp
import com.liheit.im.core.protocol.UrlPackage
import com.liheit.im.core.protocol.user.*
import com.liheit.im.core.service.UserService
import com.liheit.im.utils.*
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 用户信息管理器
 */
@Suppress("IMPLICIT_CAST_TO_ANY")
class UserManager(private var im: IMClient) : MsgHandler {
    override fun getHandlerType(): List<Int> {
//        return mutableListOf<Int>(Cmd.ImpGetUserInfoRsp, Cmd.ImpGetUserStatusRsp)
        return mutableListOf()
    }

    var cacheDir: File
    var lastUserUpdateTime: Long by DBConfigDelegates<Long>("lastUserUpdateTime", 0)
    var serverLastUpdateTime: Long by DBConfigDelegates<Long>("serverLastUserUpdateTime", 0)
    var userStatusSyncTime: Long = 100L
    var userStatus: MutableMap<Long, Long> = mutableMapOf()

    init {
        cacheDir = IMClient.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    private val userStatusSubject = PublishSubject.create<Map<Long, Long>>()

    public val stateListener = userStatusSubject.hide()

    @SuppressLint("SwitchIntDef")
    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        when (cmd) {
        }
    }

    /**
     *  获取用户信息请求
     */
    fun getAllUserInfo(refresh: Boolean = false): Completable {
        return IMClient.sendObservable(Cmd.ImpGetUserInfoReq, GetUserInfoReq(if (refresh) 0 else 1611883310200))
                .flatMap { resp ->
                    Log.e("part usergson: ${resp.packageType}")
                    when (resp.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            return@flatMap Single.just(resp.data!!)
                                    .map {
//                                        Log.e("part usergson:$it")
                                        it.fromJson<GetUserInfoRsp>()
                                    }
                                    .check()
                                    .doOnSuccess { rep ->
                                        rep.users?.let(UserService::saveBatch)
//                                        UserService.deleteByType(EditAction.DELETE)
                                        Log.e("user count ${rep?.users?.size ?: 0}")
                                        lastUserUpdateTime = rep.t
                                    }
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "uss"), UUID.randomUUID().toString())
                            return@flatMap urlRespToFile(resp.data!!, file)
                                    .flatMap { f ->
                                        return@flatMap Observable.create<User> { emitter ->
                                            var reader = JsonReader(FileReader(f))
                                            val gson = GsonBuilder().create()
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
                                                        println("t:" + lastUpdateTime)
                                                    }
                                                    "type" -> println("type:" + reader.nextInt())
                                                    "users" -> {
                                                        reader.beginArray()
                                                        while (reader.hasNext()) {
                                                            val user = gson.fromJson<User>(reader, User::class.java)
//                                                            Log.e("part usergson2: ${user}")
                                                            userCount++
                                                            emitter.onNext(user)
                                                        }
                                                        reader.endArray()
                                                        emitter.onComplete()
                                                        lastUserUpdateTime = lastUpdateTime
                                                        Log.i("lastUserUpdateTime:${lastUpdateTime}")
                                                    }
                                                }
                                            }
                                            Log.e("耗时：${System.currentTimeMillis() - begin} 用户总数 ${userCount}")
                                            reader.endObject()
                                        }
                                                .buffer(10000)
                                                .flatMap { users ->
                                                    Observable.create<Int> { emt ->
                                                        users?.find { it.account.equals("tdaixun") }?.let {
                                                            Log.e(it.email + " " + it.toString())
                                                        }
                                                        users?.let(UserService::saveBatch)
//                                                        UserService.deleteByType(EditAction.DELETE)

                                                        emt.onNext(users.count())
                                                        emt.onComplete()
                                                        Log.d("user count ${users?.size ?: 0}")
                                                    }
                                                }
                                                .doFinally {
                                                    f.delete()
                                                    file.delete()
                                                }
                                                .lastOrError()
                                    }

                        }
                        else -> return@flatMap Single.error<GetUserInfoRsp>(IMException.create("获取用户信息协议错误"))
                    }
                }
                .subscribeOn(Schedulers.io())
                .toCompletable()
    }

    fun getNewAllUserInfo(refresh: Boolean = false): Completable {
        return ApiClient.getContactsUserInfoData(if (refresh) 0 else lastUserUpdateTime)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { it ->
                    Log.e("aaa getNewAllUserInfo =${gson.toJson(it)}")
                    when (it.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            it.users?.let(UserService::saveBatch)
//                        UserService.deleteByType(EditAction.DELETE)
                            lastUserUpdateTime = it.t
                            return@flatMapCompletable Completable.complete()
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "uss"), UUID.randomUUID().toString())
                            return@flatMapCompletable urlRespToFile2(it.fileUrl, file).flatMap { f ->
                                Observable.create<User> { emitter ->
                                    var reader = JsonReader(FileReader(f))
                                    val gson = GsonBuilder().create()
                                    reader.beginObject()
                                    var userCount = 0
                                    var begin = System.currentTimeMillis()
                                    var lastUpdateTime = 0L
                                    while (reader.hasNext()) {
                                        val name = reader.nextName()
                                        Log.d("reader.nextName() ${name}")
                                        when (name) {
                                            "result" -> println("result:" + reader.nextInt())
                                            "t" -> {
                                                lastUpdateTime = reader.nextLong()
                                                println("getNewAllUserInfo t:$lastUpdateTime")
                                            }
                                            "type" -> println("type:" + reader.nextInt())
                                            "users" -> {
                                                reader.beginArray()
                                                while (reader.hasNext()) {
                                                    val user = gson.fromJson<User>(reader, User::class.java)
                                                    userCount++
                                                    emitter.onNext(user)
                                                }
                                                reader.endArray()
                                                emitter.onComplete()
                                                lastUserUpdateTime = lastUpdateTime
                                                Log.i("lastUserUpdateTime:${lastUpdateTime}")
                                            }
                                        }
                                    }
                                    Log.e("users 耗时：${System.currentTimeMillis() - begin} 用户总数 ${userCount}")
                                    reader.endObject()
                                }
                                        .buffer(10000)
                                        .flatMap { users ->
                                            Observable.create<Int> { emt ->
                                                users.let(UserService::saveBatch)
//                                                    UserService.deleteByType(EditAction.DELETE)
                                                emt.onNext(users.size)
                                                emt.onComplete()
                                                Log.e("users count ${users?.size ?: 0}")
                                            }
                                        }
                                        .doFinally {
                                            f.delete()
                                            file.delete()
                                        }
                                        .doOnError {
                                            Log.e("aaa getNewAllUserInfo doOnError ${gson.toJson(it)}")
                                        }
                                        .lastOrError()
                            }.toCompletable()
                        }
                        else -> {
                            return@flatMapCompletable Single.error<Any>(IMException.create("获取用户信息协议错误")).toCompletable()
                        }
                    }
                }
    }

    //同步单个用户的信息
    fun getUserInfoAsync(userId: Long): Completable {
        return IMClient.sendObservable(Cmd.ImpGetUserInfoReq, GetUserInfoReq(1, longArrayOf(userId)))
                .map { it.data!!.fromJson<GetUserInfoRsp>() }
                .check()
                .doOnSuccess { rep ->
                    rep.users?.let(UserService::saveBatch)
//                    UserService.deleteByType(EditAction.DELETE)
                    Log.d("user count ${rep?.users?.size ?: 0}")
                    lastUserUpdateTime = rep.t
                }.toCompletable()
    }

    fun updateHeader(filePath: String): Completable {
        var updateTime = TimeUtils.getServerTime()
        return IMClient.resourceManager.updateUserHeader(filePath)
                .toList()
                .flatMap {
                    return@flatMap IMClient.sendObservable(
                            Cmd.ImpUpdateUserInfoReq, UpdateUserInfoReq(
                            t = TimeUtils.getServerTime(),
                            flag = UpdateUserInfoFlag.Logo.value,
                            logo = updateTime
                    )
                    )
                            .map { it.data!!.fromJson<UpdateUserInfoRsp>() }
                            .check()
                            .doOnSuccess {
                                UserService.updateLogo(IMClient.getCurrentUserId(), it.logo
                                        ?: 0)
                            }
                }.toCompletable()
//                .flatMapCompletable { getUserInfoAsync(IMClient.getCurrentUserId()) }
    }

    /*
    根据部门id获取到部门的人员
    pid：部门id
     */
    fun getUserByDepid(pid: Long): MutableList<User> {
        return UserService.findByDepId(pid)
    }

    fun getUserStatus(id: Long): Long? {
        return userStatus.get(id)
    }

    fun asyncGetUserStatusById(id: Long, callback: DataCallback<Long>) {
        asyncGetUserStatusById(longArrayOf(id), object : DataCallback<MutableList<Long>> {
            override fun onSuccess(data: MutableList<Long>?) {
                callback.onSuccess(data!!.get(0))
            }

            override fun onError(code: Long, errorMsg: String?) {
                callback.onError(code, errorMsg);
            }
        })
    }

    //获取人员在线状态
    fun asyncGetUserStatusById2(ids: List<Long>, callback: DataCallback<Map<Long, Long>>) {
//        Log.e("aaa id${IMClient.loginUserId()}")
        IMClient.send(
                Cmd.ImpGetUserStatusReq,
                GetUserStatusReq(0, ids.toLongArray()),
                object : IMRespCallback {
                    override fun onSuccess(resp: CommondResp) {
                        Observable.just(resp)
                                .flatMap {
//                            Log.e("aaa it ${gson.toJson(it)}")
                                    var data = it.data!!
                                    val resp = data.fromJson<GetUserStatusRsp>()
                                    return@flatMap Observable.just(resp.status)
                                }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    callback.onSuccess(it)
                                }, {
                                    val e = it.toIMException()
                                    callback.onError(e.code, e.message)
                                })
                    }

                    override fun onProcess(state: Int, progress: Int) {
                    }

                    override fun onError(e: IMException) {
                        callback.onError(e.code, e.message)
                    }
                })
    }

    fun asyncGetUserStatusById(ids: LongArray, callback: DataCallback<MutableList<Long>>) {
        IMClient.send(Cmd.ImpGetUserStatusReq, GetUserStatusReq(0, ids), object : IMRespCallback {
            override fun onSuccess(resp: CommondResp) {
                Observable.just(resp)
                        .flatMap {
                            var data = it.data!!
                            when (resp.packageType) {
                                IMClient.PACKETE_TYPE_GSON -> {
                                    val resp = data.fromJson<GetUserStatusRsp>()
                                    if (resp.all) {
                                        userStatus.clear()
                                    }
                                    resp.status?.let {
                                        userStatus.putAll(it)
                                    }
                                    if (resp.t > 2) {
                                        userStatusSyncTime = resp.t
                                    }
                                    return@flatMap Observable.just(ids.map {
                                        userStatus.get(it) ?: 0
                                    }.toMutableList())
                                }
                                IMClient.PACKETE_TYPE_URL -> {
                                    val pkg = data.fromJson<UrlPackage>()
                                    val file = File(
                                            File(cacheDir.absolutePath, "temp"),
                                            UUID.randomUUID().toString()
                                    )
                                    return@flatMap DownloadUtil
                                            .get()
                                            .download(pkg.url, file.absolutePath)
                                            .lastElement()
                                            .map {
                                                var file = it.first
                                                ZipUtils.unzip(
                                                        file.absolutePath,
                                                        file.parentFile,
                                                        ZipUtils.calculateZipPwd(pkg.key)
                                                )
                                                return@map it.first.parentFile.listFiles { dir, name ->
                                                    return@listFiles if (name.endsWith(".json")) true else false
                                                }.first()
                                            }.toFlowable().toObservable()
                                            .map {
                                                var reader = JsonReader(FileReader(it))
                                                val gson = GsonBuilder().create()
                                                val resp = gson.fromJson<GetUserStatusRsp>(
                                                        reader,
                                                        GetUserStatusRsp::class.java
                                                )
                                                if (resp.all) {
                                                    userStatus.clear()
                                                }
                                                resp.status?.let {
                                                    userStatus.putAll(it)
                                                }
                                                if (resp.t > 2) {
                                                    userStatusSyncTime = resp.t
                                                }
                                                return@map ids.map {
                                                    userStatus.get(it) ?: 0
                                                }.toMutableList()
                                            }
                                }
                                else -> Observable.error<MutableList<Long>>(
                                        IMException.create(
                                                ResultCode.ERR_UNKNOWN
                                        )
                                )
                            }
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            callback.onSuccess(it)
                        }, {
                            val e = it.toIMException()
                            callback.onError(e.code, e.message)
                        })
            }

            override fun onProcess(state: Int, progress: Int) {

            }

            override fun onError(e: IMException) {
                callback.onError(e.code, e.message)
            }
        })
    }

    fun asyncGetAllUserStatus(callback: OperationCallback) {
        IMClient.send(Cmd.ImpGetUserStatusReq, GetUserStatusReq(t = userStatusSyncTime),
                object : IMRespCallback {
                    override fun onSuccess(resp: CommondResp) {
                        Observable.just(resp)
                                .flatMap {
                                    var data = it.data!!
//                                    Log.e("aaa ${data}")
                                    when (resp.packageType) {
                                        IMClient.PACKETE_TYPE_GSON -> {
                                            val resp = data.fromJson<GetUserStatusRsp>()
                                            if (resp.all) {
                                                userStatus.clear()
                                            }
                                            resp.status?.let {
                                                userStatus.putAll(it)
                                            }
                                            if (resp.t > 2) {
                                                userStatusSyncTime = resp.t
                                            }

                                            return@flatMap Observable.just(userStatus)
                                        }
                                        IMClient.PACKETE_TYPE_URL -> {
                                            val pkg = data.fromJson<UrlPackage>()
                                            val file = File(
                                                    File(cacheDir.absolutePath, "temp"),
                                                    UUID.randomUUID().toString()
                                            )
                                            return@flatMap DownloadUtil
                                                    .get()
                                                    .download(pkg.url, file.absolutePath)
                                                    .lastElement()
                                                    .map {
                                                        var file = it.first
                                                        ZipUtils.unzip(
                                                                file.absolutePath,
                                                                file.parentFile,
                                                                ZipUtils.calculateZipPwd(pkg.key)
                                                        )
                                                        return@map it.first.parentFile.listFiles { dir, name ->
                                                            return@listFiles if (name.endsWith(".json")) true else false
                                                        }.first()
                                                    }.toFlowable().toObservable()
                                                    .map {
                                                        var reader = JsonReader(FileReader(it))
                                                        val gson = GsonBuilder().create()
                                                        val resp = gson.fromJson<GetUserStatusRsp>(
                                                                reader,
                                                                GetUserStatusRsp::class.java
                                                        )
                                                        if (resp.all) {
                                                            userStatus.clear()
                                                        }
                                                        resp.status?.let {
                                                            userStatus.putAll(it)
                                                        }
                                                        if (resp.t > 2) {
                                                            userStatusSyncTime = resp.t
                                                        }
                                                        return@map Observable.just(userStatus)
                                                    }
                                        }
                                        else -> Observable.error<MutableMap<Long, Long>>(
                                                IMException.create(
                                                        ResultCode.ERR_UNKNOWN
                                                )
                                        )
                                    }
                                }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    callback.onSuccess()
                                    userStatusSubject.onNext(userStatus)
                                }, {
                                    val e = it.toIMException()
                                    callback.onError(e.code, e.message)
                                })
                    }

                    override fun onProcess(state: Int, progress: Int) {

                    }

                    override fun onError(e: IMException) {
                        callback.onError(e.code, e.message)
                    }
                })
    }

    var syncStatusTaskDisposable: Disposable? = null
    fun stopSyncStatusTask() {
        syncStatusTaskDisposable?.dispose()
    }

    //每隔60秒同步一次在线人员
    fun startSyncStatusTask() {
        stopSyncStatusTask()
        syncStatusTaskDisposable = Observable.interval(0, 60, TimeUnit.SECONDS)
                .flatMap {
                    return@flatMap Observable.create<Boolean> { emt ->
                        asyncGetAllUserStatus(object : OperationCallback {
                            override fun onSuccess() {
                                emt.onNext(true)
                                emt.onComplete()
                            }

                            override fun onError(code: Long, errorMsg: String?) {
                                Log.e(errorMsg + "")
                                emt.onNext(false)
                                emt.onComplete()
                            }

                        })
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                    it.printStackTrace()
                })
    }

    fun getUserById(i: Long): User? {
        return UserService.findById(i)
    }

    fun getUserByPhone(phone: String): List<User> {
        return UserService.findByPhone(phone)
    }

    fun getUserByIds(ids: List<Long>): MutableList<User> {
        return UserService.findByIds(ids)
    }

    fun getUserBySessionId(sid: String, pageSize: Int, offset: Long = 0): List<User> {
        return UserService.findBySessionId(sid, pageSize, offset)
    }

    fun findUserLikeName(keyword: String): MutableList<User> {
        return UserService.findByNameLike(keyword)
    }

    fun searchUser(keyword: String, pageSize: Int, offset: Long): MutableList<User> {
        return UserService.searchUser("|%${keyword}%|,", pageSize, offset, IMClient.getUserInfoPermissions())
    }

    /**
     * 设置用户名（cname）拼音字段
     */
    @SuppressLint("CheckResult")
    fun disposeUserPinyin() {
        Observable.create<List<User>> {
            val users = UserService.findUserPinyinIsNoll()
            it.onNext(users)
            it.onComplete()
        }.observeOn(Schedulers.io())
                .subscribe {
                    it.forEach {
                        UserService.updateUserNamePinyin(it.id, Pinyin.toPinyin(it.cname, ""))
                    }
                }
    }
}



