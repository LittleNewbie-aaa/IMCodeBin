package com.liheit.im.core.manager

import com.liheit.im.core.Cmd
import com.liheit.im.core.IMClient
import com.liheit.im.core.MsgHandler
import com.liheit.im.core.protocol.*
import com.liheit.im.utils.DBConfigDelegates
import com.liheit.im.utils.check
import com.liheit.im.utils.json.fromJson
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject


/**
 * 用户常用数据管理器
 */

class UserDataManager(private var im: IMClient) : MsgHandler {

    override fun getHandlerType(): List<Int> {
        return mutableListOf(Cmd.ImpModifyMyDataNotice)
    }

    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        when (cmd) {
            Cmd.ImpModifyMyDataNotice -> {
                val myData = data.fromJson<MyData>()
                when (myData.type) {
                    MY_DATA_MOST_USE_USER -> {
                    }
                }
            }
        }
    }

    private val mostOftenUserIds = mutableListOf<Long>()
    private val mostOftenSessionIds = mutableListOf<Long>()
    private val mostOftenDepIds = mutableListOf<Long>()

    private val changeSubject = PublishSubject.create<UserDataType>()

    fun listenerUserDataChange() = changeSubject.share()

    enum class UserDataType {
        MOST_OFTEN_USER,
        MOST_OFTEN_SESSION,
        MOST_OFTEN_DEP
    }


    fun getMostOftenUserIds(): List<Long> {
        return mostOftenUserIds.toList()
    }

    fun getMostOftenDepIds(): List<Long> {
        return mostOftenDepIds.toList()
    }

    fun isMostOftenUser(id: Long?): Boolean {
        return mostOftenUserIds.contains(id)
    }

    //获取常用信息列表请求
    fun getMostOftenUsers(): Single<List<Long>> {
        return IMClient.sendObservable(
            Cmd.ImpGetMyDataListReq,
            GetMyDataListReq(0, MY_DATA_MOST_USE_USER)
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .map {
                    it.values?.keys?.map { it.toLong() } ?: listOf()
                }
                .doOnSuccess {
                    mostOftenUserIds.clear()
                    mostOftenUserIds.addAll(it)
                }
    }

    fun getCommonSession(): Single<List<Long>> {
        return IMClient.sendObservable(
            Cmd.ImpGetMyDataListReq,
            GetMyDataListReq(0, MY_DATA_MOST_USE_SESSION)
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .map {
                    it.values?.keys?.map { it.toLong() } ?: listOf()
                }
                .doOnSuccess {
                    mostOftenSessionIds.clear()
                    mostOftenSessionIds.addAll(it)
                }
    }

    fun setCommonSession(id: Long, add: Boolean): Completable {
        return IMClient.sendObservable(
            Cmd.ImpModifyMyDataReq, ModifyMyDataReq(
                type = MY_DATA_MOST_USE_SESSION,
                del = !add,
                key = id.toString()
            )
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .doOnSuccess {
                    var id = it.key.toLongOrNull()
                    when (it.del) {
                        true -> mostOftenSessionIds.remove(id)
                        false -> id?.let { mostOftenSessionIds.add(it) }
                    }
                    changeSubject.onNext(UserDataType.MOST_OFTEN_SESSION)
                }
                .toCompletable()
    }

    fun setMostOftenUser(id: Long, add: Boolean): Completable {
        return IMClient.sendObservable(
            Cmd.ImpModifyMyDataReq, ModifyMyDataReq(
                type = MY_DATA_MOST_USE_USER,
                del = !add,
                key = id.toString()
            )
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .doOnSuccess {
                    var id = it.key.toLongOrNull()
                    when (it.del) {
                        true -> mostOftenUserIds.remove(id)
                        false -> id?.let { mostOftenUserIds.add(it) }
                    }
                    changeSubject.onNext(UserDataType.MOST_OFTEN_USER)
                }
                .toCompletable()
    }

    //获取常用信息列表请求
    fun getMostOftenDeps(): Single<List<Long>> {
        return IMClient.sendObservable(
            Cmd.ImpGetMyDataListReq,
            GetMyDataListReq(0, MY_DATA_MOST_USE_DEPARTMENT)
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .map {
                    it.values?.filter { it.value != null }?.keys?.map { it.toLong() } ?: listOf()
                }
                .doOnSuccess {
                    mostOftenDepIds.clear()
                    mostOftenDepIds.addAll(it)
                }
    }

    fun setMostOftenDep(id: Long?, add: Boolean): Completable {
        return IMClient.sendObservable(
            Cmd.ImpModifyMyDataReq, ModifyMyDataReq(
                type = MY_DATA_MOST_USE_DEPARTMENT,
                del = !add,
                key = id.toString()
            )
        )
                .map { it.data!!.fromJson<MyData>() }
                .check()
                .doOnSuccess {
                    var id = it.key.toLongOrNull()
                    when (it.del) {
                        true -> mostOftenDepIds.remove(id)
                        false -> id?.let { mostOftenDepIds.add(it) }
                    }
                    changeSubject.onNext(UserDataType.MOST_OFTEN_DEP)
                }
                .toCompletable()
    }

    fun isMostOftenDep(id: Long?): Boolean {
        return mostOftenDepIds.contains(id)
    }

    var lastUpdateTime: Long  by DBConfigDelegates<Long>("lastUpdateTime", -1)
    var serverLastUpdateTime: Long by DBConfigDelegates<Long>("serverLastUpdateTime", 0)

    var lastDepUserUpdateTime: Long  by DBConfigDelegates<Long>("lastDepUserUpdateTime", -1)

}
