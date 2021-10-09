package com.liheit.im.utils

import com.liheit.im.core.IMClient
import com.liheit.im.core.IMException
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.protocol.Rsp
import com.liheit.im.core.protocol.UrlPackage
import com.liheit.im.core.toIMException
import com.liheit.im.utils.json.fromJson
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.experimental.and

/**
 * Created by daixun on 2018/9/29.
 */

fun <T : Rsp> Single<T>.check(): Single<T> {
    return this.map {
        if (!it.isSuccess()) {
            Log.e("login:check"+it.toString()+"--"+it.result)
            throw IMException.create(it.result)
        }
        return@map it
    }
}

fun <T> Single<T>.scheduler(): Single<T> {
    return this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

fun  Completable.scheduler(): Completable {
    return this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

fun Completable.subscribeEx(onComplete: () -> Unit, onError: (Long, String) -> Unit = { c, m -> println("${c}:${m}") }): Disposable? {
    var disposable: Disposable? = null
    this.subscribe(object : CompletableObserver {
        override fun onComplete() {
            onComplete.invoke()
        }

        override fun onSubscribe(d: Disposable) {
            disposable = d
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            val imExc = e.toIMException()
            onError.invoke(imExc.code, imExc.message ?: "")
        }
    })
    return disposable
}

fun Completable.subscribeEx(onComplete: () -> Unit): Disposable? {
    return subscribeEx(onComplete, { c, m -> println("${c}:${m}") })
}

fun <T> Single<T>.subscribeEx(onSuccess: (T) -> Unit, onError: (Long, String) -> Unit = { c, m -> println("${c}:${m}") }): Disposable? {
    var disposable: Disposable? = null
    this.subscribe(object : SingleObserver<T> {
        override fun onSuccess(t: T) {
            onSuccess.invoke(t)
        }

        override fun onSubscribe(d: Disposable) {
            disposable = d
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            val imExc = e.toIMException()
            onError.invoke(imExc.code, imExc.message ?: "")
        }
    })
    return disposable
}

fun <T> Single<T>.subscribeEx(onSuccess: (T) -> Unit): Disposable? {
    return subscribeEx(onSuccess, { c, m -> println("${c}:${m}") })
}


fun urlRespToFile(urlData: String, distFile: File): Single<File> {
    return Single.just(urlData).map { urlData.fromJson<UrlPackage>() }
            .flatMap { urlPkg ->
                DownloadUtil.get()
                        .download(urlPkg.url, distFile.absolutePath)
                        .lastElement().toSingle()
                        .map {
                            var file = it.first
                            ZipUtils.unzip(file.absolutePath, file.parentFile, ZipUtils.calculateZipPwd(urlPkg.key))
                            return@map it.first.parentFile.listFiles { dir, name ->
                                return@listFiles name.endsWith(".json")
                            }.first()
                        }
            }
}

fun urlRespToFile2(urlData: String, distFile: File): Single<File> {
    return Single.just(urlData)
            .flatMap { urlPkg ->
                DownloadUtil.get()
                        .download(urlPkg, distFile.absolutePath)
                        .lastElement().toSingle()
                        .map {
                            var file = it.first
                            ZipUtils.unzip(file.absolutePath, file.parentFile,"")
                            return@map it.first.parentFile.listFiles { dir, name ->
                                return@listFiles name.endsWith(".json")
                            }.first()
                        }
            }
}


fun Int.toUnsignedShort(): Int {
    return this and 0xFFFF
}
fun Short.toUnsignedByte(): Short {
    return this and 0xFF
}
fun Int.toUnsignedByte(): Int {
    return this and 0xFF
}

fun ChatMessage.isFromMySelf(): Boolean = IMClient.getCurrentUserId() == this.fromid
