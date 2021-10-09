package com.liheit.im.core.manager

import android.os.Environment
import com.liheit.im.core.bean.Department
import com.liheit.im.core.bean.UserDepartment
import com.liheit.im.core.protocol.*
import com.liheit.im.core.service.DepartmentService
import com.liheit.im.core.service.UserService
import com.liheit.im.utils.*
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.liheit.im.common.ext.subscribeEx
import com.liheit.im.core.*
import com.liheit.im.core.bean.EditAction
import com.liheit.im.core.bean.User
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.core.http.ApiClient
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
import kotlin.system.measureTimeMillis


/**
 * 部门数据控制器
 */

class DepartmentManager(private var im: IMClient) : MsgHandler {
    override fun getHandlerType(): List<Int> {
        return mutableListOf()
    }

    var cacheDir: File

    init {
        cacheDir = IMClient.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }


    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        when (cmd) {

        }
    }

    var lastDepUpdateTime: Long by DBConfigDelegates<Long>("lastDepUpdateTime", 0)
    var serverLastUpdateTime: Long by DBConfigDelegates<Long>("serverLastUpdateTime", 0)

    var lastDepUserUpdateTime: Long by DBConfigDelegates<Long>("lastDepUserUpdateTime", 0)
    var lastPermissionUpdateTime: Long by DBConfigDelegates<Long>("lastPermissionUpdateTime", 0)
    var serverDepUserLastUpdateTime: Long by DBConfigDelegates<Long>("serverDepUserLastUpdateTime", 0)


    // 获取部门列表请求

    fun getDepartmentForServer(refresh: Boolean = false): Completable {
        return IMClient.sendObservable(Cmd.ImpGetDeptListReq,
                GetDeptListReq(if (refresh) 0 else lastDepUpdateTime)
        ).flatMap { handlerDepListReso(it) }.toCompletable()
    }

    // 部门数据处理
    private fun handlerDepListReso(resp: CommondResp): Single<Pair<GetDeptListRsp, MutableList<Department>>> {
        var updateTime = 0L
        return Observable.just(resp)
                .flatMap { rp ->
                    var packageType = rp.packageType
                    var data = rp.data!!
                    Log.e("aaa packageType=$packageType")
                    when (packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            return@flatMap Observable.create<Pair<GetDeptListRsp, MutableList<Department>>> { emt ->
                                Log.e("aaa resp=${gson.toJson(data)}")
                                val resp = data.fromJson<GetDeptListRsp>()
                                if (resp.isSuccess()) {
                                    if (resp.type == UpdateType.FULL.type) {//全量更新
                                        DepartmentService.deleteAll()//删除全部数据
                                    }
                                    updateTime = resp.t
                                    emt.onNext(Pair(resp, resp.depts ?: mutableListOf()))
                                    emt.onComplete()
                                } else {
                                    emt.onError(IMException.create(resp.result))
                                }
                            }
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            return@flatMap Observable.create<UrlPackage> { emt ->
                                var p = data.fromJson<UrlPackage>()
                                emt.onNext(p)
                                emt.onComplete()
                            }.flatMap { p ->
                                val file = File(File(cacheDir.absolutePath, "dps"), UUID.randomUUID().toString())
                                Log.e("aaa p.url=${p.url}")
                                DownloadUtil.get().download(p.url, file.absolutePath)
                                        .lastElement()
                                        .map {
                                            var file = it.first
                                            ZipUtils.unzip(file.absolutePath, file.parentFile, ZipUtils.calculateZipPwd(p.key))
                                            return@map file.parentFile.listFiles { dir, name ->
                                                return@listFiles if (name.endsWith(".json")) true else false
                                            }.first()
                                        }.toObservable()
                            }.flatMap { file ->
                                return@flatMap Observable.create<GetDeptListRsp> { emitter ->
                                    var time = measureTimeMillis {
                                        val gson = GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
                                            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                                                return false
                                            }

                                            override fun shouldSkipField(f: FieldAttributes?): Boolean {
                                                return f?.name?.equals("depts") ?: false
                                            }
                                        }).create()
                                        var reader = JsonReader(FileReader(file))
                                        var respWithoutDepts = gson.fromJson<GetDeptListRsp>(reader, GetDeptListRsp::class.java)
                                        reader.close()
                                        if (respWithoutDepts.type == UpdateType.FULL.type) {
                                            DepartmentService.deleteAll()
                                        }
                                        emitter.onNext(respWithoutDepts)
                                        emitter.onComplete()
                                    }
                                    Log.v("解析response耗时${time}")
                                }.flatMap { resp ->
                                    return@flatMap Observable.create<Department> { emitter ->
                                        var gsonForDept = GsonBuilder().create()
                                        var reader = JsonReader(FileReader(file))
                                        reader.beginObject()

                                        var times = measureTimeMillis {
                                            while (reader.hasNext()) {
                                                val name = reader.nextName()
//                                                Log.e("name ${name}")
                                                when (name) {
                                                    "result" -> println("result:" + reader.nextInt())
                                                    "t" -> {
                                                        updateTime = reader.nextLong()
                                                        println("t:" + updateTime)
                                                    }
                                                    "type" -> println("type:" + reader.nextInt())
                                                    "depts" -> {
                                                        reader.beginArray()
                                                        while (reader.hasNext()) {
                                                            val dep = gsonForDept.fromJson<Department>(reader, Department::class.java)
                                                            if (!emitter.isDisposed) {
                                                                emitter.onNext(dep)
                                                            }
                                                        }
                                                        reader.endArray()
                                                    }
                                                }
                                            }
                                            emitter.onComplete()
                                        }
                                        Log.e("耗时：${times}")
                                        reader.endObject()
                                    }.buffer(10000)
                                            .map {
                                                return@map Pair<GetDeptListRsp, MutableList<Department>>(resp, it)
                                            }
                                }
                            }
                        }
                        else -> return@flatMap Observable.error<Pair<GetDeptListRsp, MutableList<Department>>>(
                                IMException.create(ResultCode.ERR_UNKNOWN)
                        )
                    }
                }
                .doOnNext { resp ->
                    DepartmentService.saveDepartmentBatch(resp.second)//数据库保存部门数据
                    DepartmentService.deleteByType(EditAction.DELETE)//删除类型是已删除的部门数据
                    Log.d("deps count ${resp.second?.size ?: 0}")
                }
                .subscribeOn(Schedulers.io())
                .doOnComplete {
                    Log.d("部门信息插入完成")
                    lastDepUpdateTime = updateTime
                    Log.i("lastDepUpdateTime${lastDepUpdateTime}")
                }
                .doOnError { e ->
                    e.printStackTrace()
                }
                .lastOrError()
    }

    // 获取部门用户列表请求
    fun getDepartmentUserForServer(refresh: Boolean = false): Completable {
        return IMClient.sendObservable(
                Cmd.ImpGetDeptUserListReq,
                GetDeptUserListReq(if (refresh) 0 else lastDepUserUpdateTime)
        )
                .flatMap { resp ->
//                    Log.e("aaa resp=${gson.toJson(resp)}")
                    when (resp.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            return@flatMap Single.just(resp).map { it.data!!.fromJson<GetDeptUserListRsp>() }
                                    .check()
                                    .doOnSuccess {
                                        if (it.type == UpdateType.FULL.type) {//全量更新
                                            DepartmentService.deleteAllUserDepartment()
                                        }
                                        it.dusers?.forEach { dep ->
                                            dep.users?.forEach {
                                                it.depId = dep.id
                                            }
                                        }
                                        it.dusers?.flatMap {
                                            it.users ?: mutableListOf()
                                        }?.let(DepartmentService::saveBatch)
                                        DepartmentService.deleteUserDepartmentByType(EditAction.DELETE)

                                        Log.d("user-dep count ${it.dusers?.size ?: 0}")
                                        lastDepUserUpdateTime = it.t
                                    }
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "dus"), UUID.randomUUID().toString())
                            Log.e("aaa resp.data=${gson.toJson(resp.data)}")
                            return@flatMap urlRespToFile(resp.data!!, file)
                                    .map { return@map parseWithoutDepartmentUser(it) }
                                    .flatMap { f ->
                                        return@flatMap Observable.create<UserDepartment> { emitter ->
                                            val reader = JsonReader(FileReader(f))
                                            val gson = GsonBuilder().create()
                                            reader.beginObject()
                                            var begin = System.currentTimeMillis()

                                            var lastUpdateTime = 0L
                                            while (reader.hasNext()) {
                                                val name = reader.nextName()
//                                                Log.e("name ${name}")
                                                when (name) {
                                                    "result" -> println("result:" + reader.nextInt())
                                                    "t" -> {
                                                        lastUpdateTime = reader.nextLong()
                                                        println("t:${lastUpdateTime}")
                                                    }
                                                    "type" -> println("type:" + reader.nextInt())
                                                    "dusers" -> {
                                                        reader.beginArray()
                                                        while (reader.hasNext()) {
                                                            val dep = gson.fromJson<Department>(reader, Department::class.java)
                                                            dep.users?.forEach {
                                                                it.depId = dep.id
                                                                if (!emitter.isDisposed) {
                                                                    emitter.onNext(it)
                                                                }
                                                            }
                                                        }
                                                        reader.endArray()
                                                        emitter.onComplete()
                                                    }
                                                }
                                            }
                                            Log.e("耗时：${System.currentTimeMillis() - begin}")
                                            reader.endObject()
                                            lastDepUserUpdateTime = lastUpdateTime
                                            Log.i("lastDepUserUpdateTime:${lastUpdateTime}")
                                        }
                                                .buffer(10000)
                                                .doOnNext { users ->
                                                    users?.let(DepartmentService::saveBatch)
                                                    DepartmentService.deleteUserDepartmentByType(EditAction.DELETE)
                                                    Log.d("user-dep count ${users?.size ?: 0}")
                                                }.doFinally {
                                                    Log.e("user-dep count ${DbUtils.currentDB.uDepDao().count()}")
                                                    f.delete()
                                                    file.delete()
                                                }.lastOrError()
                                    }
                        }
                        else -> Single.error<Any>(IMException.create(ResultCode.ERR_UNKNOWN))

                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toCompletable()
    }

    // 获取部门权限请求
    fun getPermission(): Single<Boolean> {
        return IMClient.sendObservable(Cmd.ImpGetDeptShowReq, GetDeptShowReq(0))
                .flatMap { resp ->
                    when (resp.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            Single.just(resp.data!!)
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "permissions"), UUID.randomUUID().toString())
                            return@flatMap urlRespToFile(resp.data!!, file).map { it.readText() }
                        }
                        else -> Single.error(RuntimeException("协议错误"))
                    }
                }
                .map { Gson().fromJson(it, GetDeptShowRsp::class.java) as GetDeptShowRsp }
                .map { resp ->
                    Log.e("aaa resp=${resp.shows?.size}")
                    var updatePermissionTime = measureTimeMillis {

                        var setDefaultTime = measureTimeMillis {
                            when (resp.default) {
                                GetDeptShowRsp.UpdateFlag.NO_UPDATE.value -> {
                                }
                                GetDeptShowRsp.UpdateFlag.HIDE.value -> {
                                    DepartmentService.updateAllVisible(false)
                                    UserService.updateAllVisible(false)
                                }
                                GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value -> {
                                    DepartmentService.updateAllVisible(true)
                                    UserService.updateAllVisible(false)
                                }
                                GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value -> {
                                    DepartmentService.updateAllVisible(true)
                                    UserService.updateAllVisible(true)
                                }
                                else -> {
                                }
                            }
                        }
                        Log.e("set defaultTime ${setDefaultTime}")
                        resp.shows?.filter { it.value == GetDeptShowRsp.UpdateFlag.HIDE.value }?.keys?.toList()?.let {
                            DepartmentService.updateVisible(it, false)
                            Log.e("aaa it.toLongArray()=${it.toLongArray()}")
                            UserService.updateVisible(it.toLongArray(), false)
                        }
                        resp.shows?.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value || it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value }?.let {
                            val showDeps = it.keys.toList()
                            DepartmentService.updateVisible(showDeps, true)
                            it.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value }.keys.let {
                                UserService.updateVisible(it.toLongArray(), false)
                            }
                            it.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value }.keys.let {
                                UserService.updateVisible(it.toLongArray(), true)
                            }
                        }
                    }
                    lastPermissionUpdateTime = resp.t
                    Log.e("update permission time ${updatePermissionTime}")
                    return@map true
                }
    }

    /**
     * 获取部门数据请求
     */
    fun getNewDepartment(refresh: Boolean = false): Completable {
        return ApiClient.getContactsDeptListData(if (refresh) 0 else lastDepUpdateTime)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { it ->
                    Log.e("aaa getNewDepartmentForServer =${it}")
                    when (it.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            var dep = it
                            if (dep.type == UpdateType.FULL.type) {
                                DepartmentService.deleteAll()
                            }
                            dep.depts?.let (DepartmentService::saveDepartmentBatch) //数据库保存部门数据
                            DepartmentService.deleteByType(EditAction.DELETE)//删除类型是已删除的部门数据
                            Log.d("deps count ${dep.depts?.size ?: 0}")
                            Log.d("部门信息插入完成")
                            lastDepUpdateTime = dep.t
                            return@flatMapCompletable Completable.complete()
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "dps"), UUID.randomUUID().toString())
                            return@flatMapCompletable urlRespToFile2(it.fileUrl, file)
                                    .flatMap { f ->
                                        Log.e("aaa f ${f.absoluteFile}")
                                        return@flatMap Observable.create<Department> { emitter ->
                                            var reader = JsonReader(FileReader(f))
                                            var gsonForDept = GsonBuilder().create()
                                            reader.beginObject()
                                            var lastUpdateTime = 0L
                                            var times = measureTimeMillis {
                                                while (reader.hasNext()) {
                                                    val name = reader.nextName()
//                                                    Log.e("reader.nextName() ${name}")
                                                    when (name) {
                                                        "result" -> println("result:" + reader.nextInt())
                                                        "t" -> {
                                                            lastUpdateTime = reader.nextLong()
                                                            println("getNewDepartment t:${lastUpdateTime}")
                                                        }
                                                        "type" -> {
                                                            val type : Int = reader.nextInt()
                                                            if (type == UpdateType.FULL.type) {
                                                                DepartmentService.deleteAll()
                                                            }
                                                            println("type:$type")
                                                        }
                                                        "depts" -> {
                                                            reader.beginArray()
                                                            while (reader.hasNext()) {
                                                                val dep = gsonForDept.fromJson<Department>(reader, Department::class.java)
                                                                if (!emitter.isDisposed) {
                                                                    emitter.onNext(dep)
                                                                }
                                                            }
                                                            reader.endArray()
                                                        }
                                                    }
                                                }
                                                emitter.onComplete()
                                            }
                                            Log.e("depts 耗时：${times}")
                                            reader.endObject()
                                            lastDepUpdateTime = lastUpdateTime
                                            Log.i("lastDepUpdateTime:${lastDepUpdateTime}")
                                        }.buffer(10000)
                                                .doOnNext { deps ->
                                                    DepartmentService.saveDepartmentBatch(deps)//数据库保存部门数据
                                                    DepartmentService.deleteByType(EditAction.DELETE)//删除类型是已删除的部门数据
                                                    Log.e("depts user-dep count ${deps?.size ?: 0}")
                                                }.doFinally {
                                                    f.delete()
                                                    file.delete()
                                                }.lastOrError()
                                                .doOnError {
                                                    Log.e("aaa getNewDepartment doOnError ${gson.toJson(it)}")
                                                }
                                    }.toCompletable()
                        }
                        else -> {
                            return@flatMapCompletable Single.error<Any>(IMException.create("获取用户信息协议错误")).toCompletable()
                        }
                    }
                }
    }

    /**
     * 获取部门用户数据请求
     */
    fun getNewDepartmentUserListData(refresh: Boolean = false): Completable {
        return ApiClient.getContactsDeptUserListData(if (refresh) 0 else lastDepUserUpdateTime)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { it ->
                    Log.e("aaa getNewDepartmentUserForServer ${it}")
                    when (it.packageType) {
                        IMClient.PACKETE_TYPE_GSON -> {
                            var depUser = it
                            if (depUser.type == UpdateType.FULL.type) {
                                DepartmentService.deleteAllUserDepartment()
                            }
                            depUser.dusers?.forEach { dep -> dep.users?.forEach { it.depId = dep.id } }
                            depUser.dusers?.flatMap { it.users ?: mutableListOf() }?.let(DepartmentService::saveBatch)//数据库保存部门用户数据
                            DepartmentService.deleteUserDepartmentByType(EditAction.DELETE)//删除类型是已删除的部门用户数据
                            lastDepUserUpdateTime = depUser.t
                            return@flatMapCompletable Completable.complete()
                        }
                        IMClient.PACKETE_TYPE_URL -> {
                            val file = File(File(cacheDir.absolutePath, "dus"), UUID.randomUUID().toString())
                            return@flatMapCompletable urlRespToFile2(it.fileUrl, file)
                                    .flatMap { f ->
                                        return@flatMap Observable.create<UserDepartment> { emitter ->
                                            val reader = JsonReader(FileReader(f))
                                            val gson = GsonBuilder().create()
                                            reader.beginObject()
                                            var lastUpdateTime = 0L
                                            var times = measureTimeMillis {
                                                while (reader.hasNext()) {
                                                    val name = reader.nextName()
                                                    Log.d("name ${name}")
                                                    when (name) {
                                                        "result" -> println("result:" + reader.nextInt())
                                                        "t" -> {
                                                            lastUpdateTime = reader.nextLong()
                                                            println("getNewDepartmentUserListData t:${lastUpdateTime}")
                                                        }
                                                        "type" -> {
                                                            val type : Int = reader.nextInt()
                                                            if (type == UpdateType.FULL.type) {
                                                                DepartmentService.deleteAllUserDepartment()
                                                            }
                                                            println("type:$type")
                                                        }
                                                        "dusers" -> {
                                                            reader.beginArray()
                                                            while (reader.hasNext()) {
                                                                val dep = gson.fromJson<Department>(reader, Department::class.java)
                                                                dep.users?.forEach {
                                                                    it.depId = dep.id
                                                                    if (!emitter.isDisposed) {
                                                                        emitter.onNext(it)
                                                                    }
                                                                }
                                                            }
                                                            reader.endArray()
                                                            emitter.onComplete()
                                                        }
                                                    }
                                                }
                                            }
                                            Log.e("dusers 耗时：${times}")
                                            reader.endObject()
                                            lastDepUserUpdateTime = lastUpdateTime
                                            Log.i("lastDepUserUpdateTime:${lastDepUserUpdateTime}")
                                        }
                                                .buffer(10000)
                                                .doOnNext { dusers ->
                                                    dusers?.let(DepartmentService::saveBatch)
                                                    DepartmentService.deleteUserDepartmentByType(EditAction.DELETE)
                                                    Log.e("dusers user-dep count ${dusers?.size ?: 0}")
                                                }.doFinally {
                                                    f.delete()
                                                    file.delete()
                                                }.lastOrError()
                                                .doOnError {
                                                    Log.e("aaa getNewDepartmentUserListData doOnError ${gson.toJson(it)}")
                                                }
                                    }.toCompletable()
                        }
                        else -> {
                            return@flatMapCompletable Single.error<Any>(IMException.create("获取用户信息协议错误")).toCompletable()
                        }
                    }
                }
    }

    /**
     * 获取部门权限请求
     */
    fun getNewPermission(){
        ApiClient.getContactsDeptShowData()
                .map {
                    if (it.isSuccess()) {
                        var deptShow = it.result
                        Log.e("aaa resp java=${deptShow?.shows?.size}")
                        var updatePermissionTime = measureTimeMillis {
                            var setDefaultTime = measureTimeMillis {
                                when (deptShow?.default) {
                                    GetDeptShowRsp.UpdateFlag.NO_UPDATE.value -> {
                                    }
                                    GetDeptShowRsp.UpdateFlag.HIDE.value -> {
                                        DepartmentService.updateAllVisible(false)
                                        UserService.updateAllVisible(false)
                                    }
                                    GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value -> {
                                        DepartmentService.updateAllVisible(true)
                                        UserService.updateAllVisible(false)
                                    }
                                    GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value -> {
                                        DepartmentService.updateAllVisible(true)
                                        UserService.updateAllVisible(true)
                                    }
                                    else -> {
                                    }
                                }
                            }
                            Log.e("set defaultTime ${setDefaultTime}")
                            deptShow?.shows?.filter { it.value == GetDeptShowRsp.UpdateFlag.HIDE.value }?.keys?.toList()?.let {
                                DepartmentService.updateVisible(it, false)
                                UserService.updateVisible(it.toLongArray(), false)
                            }
                            deptShow?.shows?.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value || it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value }?.let {
                                val showDeps = it.keys.toList()
                                DepartmentService.updateVisible(showDeps, true)
                                it.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT.value }.keys.let {
                                    UserService.updateVisible(it.toLongArray(), false)
                                }
                                it.filter { it.value == GetDeptShowRsp.UpdateFlag.SHOW_DEPT_USER.value }.keys.let {
                                    UserService.updateVisible(it.toLongArray(), true)
                                }
                            }
                        }
                        lastPermissionUpdateTime = deptShow!!.t
                        return@map true
                    } else {
                        error("获取用户信息协议错误")
                    }
                }.subscribeOn(Schedulers.io())
                .subscribeEx{}
    }

    private fun parseWithoutDepartmentUser(it: File): File {
        val gson = GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return false
            }

            override fun shouldSkipField(f: FieldAttributes?): Boolean {
                return f?.name?.equals("dusers") ?: false
            }
        }).create()

        val resp = gson.fromJson(FileReader(it), GetDeptUserListRsp::class.java)
        if (resp.isSuccess()) {
            if (resp.type == UpdateType.FULL.type) {
                DepartmentService.deleteAllUserDepartment()
            }
            return it
        } else {
            throw resp.toException()
        }
    }


    fun getDepartmentByPid(pid: Long): MutableList<Department> {
        return DepartmentService.findByPid(pid)
    }

    fun getDepartmentById(id: Long): Department? {
        return DepartmentService.findById(id)
    }

    fun getUserDepartment(userId: Long): List<Department> {
        return DepartmentService.getUserDepartment(userId)
    }

    fun getDepartmentByIds(ids: List<Long>): MutableList<Department> {
        return DepartmentService.findByIds(ids)
    }

    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Department> {
        return DepartmentService.search("%${keyword}%", pageSize, offset)
    }

}
