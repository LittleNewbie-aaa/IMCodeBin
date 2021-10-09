package com.liheit.im.core.http

import com.google.gson.GsonBuilder
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMException
import com.liheit.im.core.bean.UploadInfo
import com.liheit.im.core.bean.UploadParam
import com.liheit.im.core.protocol.GetDeptListRsp
import com.liheit.im.core.protocol.GetDeptShowRsp
import com.liheit.im.core.protocol.GetDeptUserListRsp
import com.liheit.im.core.protocol.GetUserInfoRsp
import com.liheit.im.utils.*
import com.liheit.im.utils.json.gson
import com.liheit.im_core.BuildConfig
import io.reactivex.Observable
import okhttp3.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/**
 * Created by daixun on 2018/7/2.
 */

internal object ApiClient {

    private var api: Api

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.readTimeout(15, TimeUnit.SECONDS)
        clientBuilder.writeTimeout(15, TimeUnit.SECONDS)
        clientBuilder.connectTimeout(15, TimeUnit.SECONDS)
        clientBuilder.connectionPool(ConnectionPool(3,3,TimeUnit.MINUTES))
        clientBuilder.followSslRedirects(true)
                .retryOnConnectionFailure(true)
        //忽略ssl证书验证
        clientBuilder.sslSocketFactory(SSLSupport.getSSLSocketFactory())
        clientBuilder.hostnameVerifier(SSLSupport.getHostnameVerifier())
//        clientBuilder.addNetworkInterceptor(SignatureInterceptor())

        if (BuildConfig.DEBUG) {
//            clientBuilder.addNetworkInterceptor(LogInterceptor())
//            clientBuilder.addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
//            clientBuilder.addNetworkInterceptor(LogInterceptor().setLevel(LogInterceptor.Level.BODY))
        }

        //添加公共请求头信息
//        clientBuilder.addInterceptor { chain ->
//            val request: Request = chain.request()
//                    .newBuilder()
//                    .addHeader("Accept-Encoding", "gzip, deflate, br")
//                    .build()
//            chain.proceed(request)
//        }

        var gson = GsonBuilder().setLenient().create()
        val builder = Retrofit.Builder()
                .baseUrl(HttpUrl.parse("${IMClient.config.fileServer}:${IMClient.config.fileServerPort}"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(clientBuilder.build())
        val retrofit = builder.build()
        api = retrofit.create(Api::class.java)
    }

    fun getSign(account: String, password: String, sign: String): Observable<List<String>> {
        var userType = 0
        var params = mutableMapOf<String, String>()
        params.put("userType", userType.toString())
        params.put("userCode", account)
        params.put("userTerm", "2")
        params.put("password", password)
        params.put("terminal", "2")
        params.put("account", account)
        params.put("timeStamp", TimeUtils.getServerTime().toString())
        params.put("sign", sign)
        val url = "${IMClient.config.restServer}:${IMClient.config.restServerPort}/rest/api/sign/gen"
        return api.getSign(url, params).map<List<String>> {
            if (it.code == 0) {
                return@map it.result?.map { AESUtils.decryptToString(it) } ?: mutableListOf<String>()
            } else {
                throw RuntimeException(it.message)
            }
        }
    }

    /**
     * 下载文件
     */
    fun downloadFile(
            range: String? = null, type: Int, fileToken: String? = null,
            fileName: String? = null, size: Long? = null, source: Boolean,
            userCode: String? = null, sign: String
    ): Observable<Response<ResponseBody>> {
        var account = IMClient.getCurrentUserAccount() ?: ""
        val url = "${IMClient.config.fileServer}:${IMClient.config.fileServerPort}/file/api/download"
        return api.downloadFile(
                url, range, type, fileToken, size, fileName,
                source, userCode, 2, account, sign
        )
    }

    /**
     * 下载文件
     */
    fun downloadCollectionFile(
            range: String? = null, type: Int, fileToken: String? = null,
            fileName: String? = null, size: Long? = null, source: Boolean,
            userCode: String? = null, sign: String
    ): Observable<Response<ResponseBody>> {
        var account = IMClient.getCurrentUserAccount() ?: ""
        val url = "${IMClient.config.fileServer}:${IMClient.config.fileServerPort}/file/collection/download"
        return api.downloadFile(
                url, range, type, fileToken, size, fileName,
                source, userCode, 2, account, sign
        )
    }

    fun uploadFile(type: Int, file: File, userCode: String?, sign: String): Observable<UploadInfo> {
        return Observable.create<UploadInfo> { emt ->

            val fileMd5 = MD5Util.md5Hex(FileInputStream(file))
            var fileName = fileMd5
            var account = IMClient.getCurrentUserAccount() ?: ""
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
            val widthHeight = BitmapUtil.getImageWidthHeight(file.absolutePath)

            val info = UploadInfo(
                    md5 = fileMd5, token = fileMd5, bytes = file.length(),
                    width = widthHeight[0], height = widthHeight[1], process = 0
            )

            var requestBody = ProgressRequestBody(requestFile) { total, transferSize ->
                Log.v("progress ${total} $transferSize")
                info.process = (transferSize * 100 / total).toInt()
                emt.onNext(info)
            }
            var body = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val resp = api.uploadFile(
                    url = getUploadUrl(), type = type, fileMD5 = fileMd5, fileName = fileName,
                    fileSize = file.length(), userCode = userCode, terminal = 2, account = account,
                    sign = sign, file = body
            ).blockingLast()
            if (resp.isSuccess()) {
                emt.onComplete()
            } else {
                emt.onError(RuntimeException(resp.message))
            }
        }
    }

    /**
     * 上传文件
     */
    fun uploadFile(param: UploadParam): Observable<Int> {
        return Observable.create<Int> { emt ->
            var account = param.account
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), File(param.filePath))
            var requestBody = ProgressRequestBody(requestFile){ total, transferSize ->
                        emt.onNext((transferSize * 100 / total).toInt()) }
            var body = MultipartBody.Part.createFormData("file", param.fileName, requestBody)
            val resp = api.uploadFile(url = getUploadUrl(), type = param.type, fileMD5 = param.token, fileName = param.fileName,
                    fileSize = param.fileSize, userCode = param.userCode, terminal = param.terminal,
                    account = account, sign = param.sign, file = body)
                    .blockingLast()
            Log.e("sendfile ${param.token} ${gson.toJson(resp)}")
            if (resp.isSuccess()) {
                emt.onComplete()
            } else {
                Log.e("aaa resp.message ${resp.message}")
                emt.onError(RuntimeException(resp.message))
            }
        }
    }

    private fun getUploadUrl(): String {
        return "${IMClient.config.fileServer}:${IMClient.config.fileServerPort}/file/api/upload"
    }

    fun download(url: String, saveTo: String): Observable<Int> {
        return api.download(url)
                .flatMap { resp ->
                    return@flatMap Observable.create<Int> { emt ->
                        val length = resp.contentLength()
                        val target = File(saveTo)
                        val outputStream = target.outputStream().buffered(1024)
                        resp.byteStream().buffered().use {
                            it.copyTo(outputStream, 1024, callback = { copyed ->
                                emt.onNext((copyed * 100 / length).toInt())
                            })
                        }
                        outputStream.close()
                        emt.onComplete()
                    }.distinctUntilChanged()
                }
    }

    /**
     * 上传文件初始化
     */
    fun uploadInit(type: Int, file: File, userCode: String?, sign: String): Observable<Api.FResp> {
        //TODO
        val fileMd5 = MD5Util.md5Hex(FileInputStream(file))
        var fileName = fileMd5
        var account = IMClient.getCurrentUserAccount() ?: ""
        val url = "${IMClient.config.fileServer}:${IMClient.config.fileServerPort}/file/api/init"
        return api.uploadInit(
                url = url, type = type, fileMD5 = fileMd5, fileSize = file.length(),
                fileName = fileName, userCode = userCode, account = account, sign = sign
        )
    }

    fun getContactsData(userInfoReq: Any, deptListReqDto: Any, deptUserListReqDto: Any, deptShowReqDto: Any)
            : Observable<Api.RestResp<ContactsData>> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.restServer}:${IMClient.config.restServerPort}/rest/Contacts/getData"
                    val param = Api.ContactsDataParam(
                            userInfoReq = userInfoReq,
                            deptListReqDto = deptListReqDto,
                            deptUserListReqDto = deptUserListReqDto,
                            timeStamp = TimeUtils.getServerTime(),
                            deptShowReqDto = deptShowReqDto,
                            sign = s
                    )
                    api.getContactsData(path, param).map {
                        if (it.isSuccess()) return@map it
                        else throw IMException.create("获取用户信息协议错误")
                    }
                }
    }

    /**
     * 获取用户数据
     */
    fun getContactsUserInfoData(t: Long, ids: List<Long>? = mutableListOf()): Observable<GetUserInfoRsp> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.contactsServer}:${IMClient.config.contactsServerPort}/contacts/userInfoReq"
//                    val path = "http://192.168.0.165:8072/contacts/userInfoReq"
                    val param = Api.ContactsUserDataParam(t = t, ids = ids, sign = s,
                            timeStamp = TimeUtils.getServerTime())
                    api.getContactsUserInfoData(path, param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create("获取用户信息协议错误")
                    }
                }
    }

    /**
     * 获取部门数据
     */
    fun getContactsDeptListData(t: Long, ids: List<Long>? = mutableListOf()): Observable<GetDeptListRsp> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.contactsServer}:${IMClient.config.contactsServerPort}/contacts/deptListReq"
//                    val path = "http://192.168.0.165:8072/contacts/deptListReq"
                    val param = Api.ContactsDeptListParam(t = t, ids = ids, sign = s,
                            timeStamp = TimeUtils.getServerTime())
                    api.getContactsDeptListData(path, param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create("获取用户信息协议错误")
                    }
                }
    }

    /**
     * 获取部门用户数据
     */
    fun getContactsDeptUserListData(t: Long): Observable<GetDeptUserListRsp> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.contactsServer}:${IMClient.config.contactsServerPort}/contacts/deptUserListReq"
//                    val path = "http://192.168.0.165:8072/contacts/deptUserListReq"
                    val param = Api.ContactsDeptUserListParam(t = t, sign = s, timeStamp = TimeUtils.getServerTime())
                    api.getContactsDeptUserListData(path, param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create("获取用户信息协议错误")
                    }
                }
    }

    /**
     * 获取部门权限
     */
    fun getContactsDeptShowData(): Observable<Api.RestResp<GetDeptShowRsp>> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.contactsServer}:${IMClient.config.contactsServerPort}/contacts/deptShowReq"
//                    val path = "http://192.168.0.165:8078/contacts/deptShowReq"
                    val param = Api.ContactsDeptShowParam(userId = IMClient.getCurrentUserId(),
                            sign = s, timeStamp = TimeUtils.getServerTime())
                    api.getContactsDeptShowData(path, param).map {
                        if (it.isSuccess()) return@map it
                        else throw IMException.create("获取用户信息协议错误")
                    }
                }
    }
}
