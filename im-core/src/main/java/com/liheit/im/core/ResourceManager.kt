package com.liheit.im.core

import android.os.Parcelable
import com.google.gson.GsonBuilder
import com.liheit.im.core.bean.*
import com.liheit.im.core.http.ApiClient
import com.liheit.im.core.protocol.FileBody
import com.liheit.im.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import okhttp3.MediaType
import okio.Buffer
import okio.ForwardingSink
import okio.Okio
import java.io.File
import java.io.FileInputStream
import java.util.*


/**
 * 文件服务上传下载管理器
 */

class ResourceManager() {

    private val DOWNLOAD_URL = "http://193.112.5.114:8085/file/api/download"

    private val INIT_URL = "http://193.112.5.114:8083/rest/api/sign/gen"

    val JSON = MediaType.parse("application/json; charset=utf-8")

    private val gson = GsonBuilder().create()

    private var sign: String = ""
    private var account: String = ""
    private var pwd: String = ""

    fun init(account: String, password: String, sign: String) {
        this.account = account
        this.pwd = password
        this.sign = sign
        this.temporarySing = Observable.just(sign)
        tokens.clear()
    }

    fun getUserHeaderImg(userAccount: String, thumb: Boolean = true): Observable<String> {
        val fileName = "head" + userAccount + "${if (thumb) "thumb" else ""}.jpg"
        Log.e("aaa fileName=${fileName}")
        var imageFile = StorageUtils.getImageHeadsCacheFile(fileName)
        if (imageFile.exists()) {
            return Observable.just(imageFile.absolutePath)
        }
        return getSign().flatMap {
            var type = 0 //头像
            ApiClient.downloadFile(userCode = userAccount, sign = it, type = type, source = !thumb)
                    .map {
                        var saveFile = StorageUtils.getImageHeadsCacheFile(fileName)
                        val source = Okio.buffer(it.body()!!.source())
                        //原来的方法，注销掉 将随机生成头像文件，转发为固定的名称，这样可以添加缓存头像
//                        var saveFile = StorageUtils.getImageHeadsCacheFile(UUID.randomUUID().toString() + "${if (thumb) "thumb" else ""}.jpg")
                        val sink = Okio.buffer(Okio.sink(saveFile))
                        sink.writeAll(source)
                        source.close()
                        sink.close()
                        return@map saveFile.absolutePath
                    }
        }
    }

    /*
     *  根据user对象来获取用户的头像
     */
    fun getUserHeaderImg(user: User, thumb: Boolean = true): Observable<String> {
        val fileName = "head" + user.account + "${if (thumb) "thumb" else ""}.jpg"
        var imageFile = StorageUtils.getImageHeadsCacheFile(fileName)
        Log.e("aaa imageFile.exists()=${imageFile.exists()}")
        if (imageFile.exists()) {
            return Observable.just(imageFile.absolutePath)
        }
        return getSign().flatMap {
            var type = 0 //头像
            ApiClient.downloadFile(userCode = user.account, sign = it, type = type, source = !thumb)
                    .map {
                        var saveFile = StorageUtils.getImageHeadsCacheFile(filename = fileName)
                        val source = Okio.buffer(it.body()!!.source())
                        Log.e("aaa saveFile=${saveFile.absoluteFile}")
                        //原来的方法，注销掉 将随机生成头像文件，转发为固定的名称，这样可以添加缓存头像
//                        var saveFile = StorageUtils.getImageHeadsCacheFile(UUID.randomUUID().toString() + "${if (thumb) "thumb" else ""}.jpg")
                        val sink = Okio.buffer(Okio.sink(saveFile))
                        sink.writeAll(source)
                        source.close()
                        sink.close()
                        return@map saveFile.absolutePath
                    }
        }
    }

    fun updateUserHeader(path: String): Observable<Int> {
        var type = 0 //头像
        var file = File(path)
        var userCode = IMClient.getCurrentUserAccount() ?: ""

        return Observable.fromCallable {
            val fileMd5 = MD5Util.md5Hex(FileInputStream(file))
            UploadParam(
                    type = type,
                    token = fileMd5,
                    fileSize = file.length(),
                    fileName = file.name,
                    userCode = userCode,
                    filePath = path,
                    sign = sign,
                    account = userCode
            )
        }.flatMap { uploadFile("", it) }.map { it.second }
    }

    fun getMsgImage(type: Int, msg: FileBody, thumbnail: Boolean): Observable<String> {
        val fileName = "${msg.md5}${if (thumbnail) "" else "thumb"}${getExtension(msg.name)}"
        var imageFile = StorageUtils.getImageCacheFile(fileName)
        if (imageFile.exists()) {
            return Observable.just(imageFile.absolutePath)
        }
        return getSign().flatMap {
            var type = type //图片 或者视频的缩略图
            ApiClient.downloadFile(fileToken = msg.token, size = msg.bytes, fileName = msg.md5, sign = it, type = type, source = !thumbnail)
                    .map {
                        val body = it.body()
                        val source = Okio.buffer(body!!.source())
                        val sink = Okio.buffer(Okio.sink(imageFile))
                        sink.writeAll(source)
                        source.close()
                        sink.close()
                        return@map imageFile.absolutePath
                    }
        }
    }

    fun getMsgCollectionImage(type: Int, msg: FileBody, thumbnail: Boolean): Observable<String> {
        val fileName = "${msg.md5}${if (thumbnail) "" else "thumb"}${getExtension(msg.name)}"
        var imageFile = StorageUtils.getImageCacheFile(fileName)
        if (imageFile.exists()) {
            return Observable.just(imageFile.absolutePath)
        }
        return getSign().flatMap {
            var type = type //图片 或者视频的缩略图
            ApiClient.downloadCollectionFile(fileToken = msg.token, size = msg.bytes, fileName = msg.md5, sign = it, type = type, source = !thumbnail)
                    .map {
                        val body = it.body()
                        val source = Okio.buffer(body!!.source())
                        val sink = Okio.buffer(Okio.sink(imageFile))
                        sink.writeAll(source)
                        source.close()
                        sink.close()
                        return@map imageFile.absolutePath
                    }
        }
    }

    private fun getExtension(name: String): String {
//        var extension = name.substringAfterLast(".", "")
//        if (extension.isNullOrEmpty()) {
//            return extension
//        } else {
//            return ".${extension}"
//        }
        return ".png"
    }


    @Parcelize
    data class DownloadParam(
            var type: Int = 0,
            var token: String = "",
            var md5: String = "",
            var bytes: Long = 0,
            var name: String = "",
            var thumbnail: Boolean = true
    ) : Parcelable

    fun downloadResource(outputFile: String, param: DownloadParam): Observable<Int> {
        if (sign.isNullOrEmpty()) {
            return Observable.error(IMException(-1, "签名为空"))
        }
        val saveFile = File(outputFile)
        if (saveFile.exists()) {
            //TODO 校验MD5
            return Observable.just(100)
        }
        return getSign().flatMap {
            ApiClient.downloadFile(fileToken = param.token, size = param.bytes,
                    fileName = param.md5, sign = it,
                    type = param.type, source = !param.thumbnail)
                    .flatMap { it ->
                        if (it.code() != 200) {
                            if (it.code() == 404) {
                                return@flatMap Observable.error<Int>(IMException(it.code().toLong(), "没有找到对应资源(10002)"))
                            } else {
                                return@flatMap Observable.error<Int>(IMException(it.code().toLong(), "下载失败${it.code()}"))
                            }
                        }
                        val body = it.body()
                        Observable.create<Int> { emt ->
                            val total = body!!.contentLength()
                            val source = Okio.buffer(body.source())
                            var saveFile = saveFile
                            if (!saveFile.parentFile.exists()) {
                                saveFile.parentFile.mkdirs()
                            }
                            val sink = Okio.buffer(object : ForwardingSink(Okio.sink(saveFile)) {
                                var readerSize = 0L
                                override fun write(source: Buffer?, byteCount: Long) {
                                    super.write(source, byteCount)
                                    readerSize += byteCount
                                    emt.onNext((readerSize * 100 / total).toInt())
                                }
                            })
                            sink.writeAll(source)
                            source.close()
                            sink.close()
                            emt.onComplete()
                        }
                    }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun downloadResource2(): Observable<String> {
        return getSign().map {
            return@map it
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun downloadResource(outputFile: String, type: Int, param: FileBody, source: Boolean): Observable<Int> {
        if (sign.isNullOrEmpty()) {
            return Observable.error(IMException(-1, "签名为空"))
        }
        val saveFile = File(outputFile)
        if (saveFile.exists()) {
            //TODO 校验MD5
            return Observable.just(100)
        }
        return getSign().flatMap {
            ApiClient.downloadFile(fileToken = param.token, size = param.bytes,
                    fileName = param.md5, sign = it, type = type, source = source)
                    .flatMap { it ->
                        if (it.code() != 200) {
                            if (it.code() == 404) {
                                return@flatMap Observable.error<Int>(IMException(it.code().toLong(), "没有找到对应资源(10002)"))
                            } else {
                                return@flatMap Observable.error<Int>(IMException(it.code().toLong(), "下载失败${it.code()}"))
                            }
                        }
                        val body = it.body()
                        Observable.create<Int> { emt ->
                            val total = body!!.contentLength()
                            val source = Okio.buffer(body.source())
                            var saveFile = saveFile
                            if (!saveFile.parentFile.exists()) {
                                saveFile.parentFile.mkdirs()
                            }
                            val sink = Okio.buffer(object : ForwardingSink(Okio.sink(saveFile)) {
                                var readerSize = 0L
                                override fun write(source: Buffer?, byteCount: Long) {
                                    super.write(source, byteCount)
                                    readerSize += byteCount
                                    emt.onNext((readerSize * 100 / total).toInt())
                                }
                            })
                            sink.writeAll(source)
                            source.close()
                            sink.close()
                            emt.onComplete()
                        }
                    }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 上传文件(同步)
     */
    internal fun uploadFileSynchronous(id: String, param: UploadParam): Observable<Pair<String, Int>> {
        var type = param.type
        var file = File(param.filePath)
        var userCode = param.userCode
        Log.e("sendfile:init begin")
        return getSign().flatMap {
            Log.e("sendfile:init uploadInit")
            ApiClient.uploadInit(type, file, userCode, it)
                    .map {
                        if (it.isSuccess()) {
                            return@map true
                        } else {
                            throw RuntimeException("init 失败")
                        }
                    }
        }.flatMap {
            return@flatMap getSign().flatMap { s ->
                param.sign = s
                Log.e("sendfile:uploadFile begin")
                ApiClient.uploadFile(param).map { p -> return@map Pair<String, Int>(id, p) }
            }
        }
    }

    /**
     * 上传文件（并行）
     */
    internal fun uploadFile(id: String, param: UploadParam): Observable<Pair<String, Int>> {
        var type = param.type
        var file = File(param.filePath)
        var userCode = param.userCode
//        Log.e("sendfile:init begin")
        return getSign().flatMap {
//            Log.e("sendfile:init uploadInit")
            ApiClient.uploadInit(type, file, userCode, it)
                    .subscribeOn(Schedulers.io())
                    .map {
                        if (it.isSuccess()) {
                            return@map true
                        } else {
                            throw RuntimeException("init 失败")
                        }
                    }
        }.flatMap {
            return@flatMap getSign().flatMap { s ->
                param.sign = s
                Log.e("sendfile:uploadFile begin")
                ApiClient.uploadFile(param)
                        .map { p -> return@map Pair<String, Int>(id, p) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
            }
        }
    }

    fun download(url: String, saveTo: String): Observable<Int> {
        return ApiClient.download(url, saveTo)
    }

    private val tokens = LinkedList<String>()
    lateinit var temporarySing: Observable<String>
    var temporarySingTime: Long = 0L

    /**
     * 获取当前登陆账号sing
     */
    fun getSign(): Observable<String> {
        //FIXME 莫名其妙的token失效问题，所以禁用缓存，每次重新请求token
        val time = TimeUtils.getServerTime()
        if (temporarySingTime == 0L || time - temporarySingTime > 3* 60 * 1000) {
            temporarySing = ApiClient.getSign(account, pwd, sign).subscribeOn(Schedulers.io()).map { it.getOrNull(0) }
            temporarySingTime = TimeUtils.getServerTime()
        }
        return temporarySing
    }

    interface ResourceCallback {
        fun success(filePath: String)
        fun error(code: Int, reason: String)
        fun progress(progress: Int)
    }

    data class Resp(
            var message: String = "",
            var result: MutableList<String>? = null,
            var code: Int = 0
    )

}

fun ChatMessage.toDownloadParam(): ResourceManager.DownloadParam {
    var msgBody = this.msgs!![0]
    if (msgBody is FileBody) {
        return ResourceManager.DownloadParam()
                .apply {
                    token = msgBody.token
                    md5 = msgBody.md5
                    name = msgBody.name
                    bytes = msgBody.bytes
                    type = msgBody.mtype
                    thumbnail = when (msgBody.mtype) {
                        MessageType.IMAGE.value,
                        MessageType.VIDEO.value
                        -> true
                        else -> false
                    }
                }
    }
    return ResourceManager.DownloadParam()
}

fun MessageFile.toDownloadParam(): ResourceManager.DownloadParam {
    var msgBody = this
    return ResourceManager.DownloadParam()
            .apply {
                token = msgBody.token
                md5 = msgBody.md5
                name = msgBody.name
                bytes = msgBody.bytes
                type = msgBody.type
                thumbnail = when (msgBody.type) {
                    MessageType.IMAGE.value,
                    MessageType.VIDEO.value
                    -> true
                    else -> false
                }
            }
}
