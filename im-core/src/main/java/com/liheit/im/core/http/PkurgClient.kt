package com.liheit.im.core.http

import android.util.Log
import com.blankj.utilcode.util.DeviceUtils
import retrofit2.converter.gson.GsonConverterFactory2
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMException
import com.liheit.im.core.bean.*
import com.liheit.im.core.protocol.GetDeptShowRsp
import com.liheit.im.utils.AESUtils
import com.liheit.im.utils.SSLSupport
import com.liheit.im.utils.TimeUtils
import com.liheit.im_core.BuildConfig
import com.pkurg.lib.ui.solitaire.ContentItem
import com.pkurg.lib.ui.solitaire.SolitaireTitle
import com.pkurg.lib.ui.vote.Record
import com.pkurg.lib.ui.vote.ResultVoteInfo
import com.pkurg.lib.ui.vote.VoteInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object PkurgClient {
    var client: PkurgApi

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.readTimeout(15, TimeUnit.SECONDS)
        clientBuilder.writeTimeout(15, TimeUnit.SECONDS)
        clientBuilder.connectTimeout(15, TimeUnit.SECONDS)
        clientBuilder.followSslRedirects(true)
                .retryOnConnectionFailure(true)
        //忽略ssl证书验证
        clientBuilder.sslSocketFactory(SSLSupport.getSSLSocketFactory())
        clientBuilder.hostnameVerifier(SSLSupport.getHostnameVerifier())

//        clientBuilder.addNetworkInterceptor(SignatureInterceptor())
        if (BuildConfig.DEBUG) {
            clientBuilder.addNetworkInterceptor(LogInterceptor().setLevel(LogInterceptor.Level.BODY))
        }

        val builder = Retrofit.Builder()
                .baseUrl(HttpUrl.parse("${IMClient.config.restServer}:${IMClient.config.restServerPort}")!!)
                .addConverterFactory(GsonConverterFactory2.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(clientBuilder.build())

        val retrofit = builder.build()
        client = retrofit.create<PkurgApi>(PkurgApi::class.java)
    }

    fun getHomeData(account: String, depId: LongArray): Observable<List<HomeData>> {
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    client.getHomeData(
                            PkurgApi.HomeParam(
                                    depId,
                                    account,
                                    TimeUtils.getServerTime(),
                                    sign
                            )
                    )
                            .map {
                                if (it.isSuccess()) return@map it.result
                                        ?: listOf<HomeData>() else throw IMException.create(it.message)
                            }
                }
    }

    fun getPageData(
            key: String, page: Int = 0, pageSize: Int = 20,
            account: String, depId: LongArray
    ): Observable<List<HomeItem>> {
        return getData(key, "page", page, pageSize, account, depId)
    }

    fun getAllData(key: String, account: String, depId: LongArray): Observable<List<HomeItem>> {
        return getData(key, "list", 0, 100, account, depId)
    }

    fun feedback(content: String, images: List<File>): Single<Boolean> {
        val map = mutableMapOf<String, RequestBody>()

        val files = images.map {
            MultipartBody.Part.createFormData(
                    "files",
                    it.name,
                    RequestBody.create(MultipartBody.FORM, it)
            )
        }
        map["content"] = RequestBody.create(MultipartBody.FORM, content)
        map["account"] = RequestBody.create(
                MultipartBody.FORM, IMClient.getCurrentUserAccount() ?: ""
        )
        map["terminal"] = RequestBody.create(MultipartBody.FORM, 2.toString())

        return client.feedback(map, files).map {
            if (it.isSuccess())
                return@map true
            else {
                Log.e("test", it.message + ":-itresult" + it.result + ":statues-" + it.status)
                throw IMException.create(it.message)
            }

        }
    }

    fun logFilePush(file: File): Single<Boolean> {
        val map = mutableMapOf<String, RequestBody>()

        val file = MultipartBody.Part.createFormData("file", file.name, RequestBody.create(MultipartBody.FORM, file))
        map["account"] = RequestBody.create(MultipartBody.FORM, IMClient.getCurrentUserAccount()
                ?: "")
        map["terminal"] = RequestBody.create(MultipartBody.FORM, 2.toString())
        map["equipment"] = RequestBody.create(MultipartBody.FORM, DeviceUtils.getManufacturer()
                + "/" + DeviceUtils.getModel()+ "/" + DeviceUtils.getSDKVersionName())

        return client.logFilePush(map, file).map {
            if (it.isSuccess())
                return@map true
            else {
                Log.e("test", it.message + ":-itresult" + it.result + ":statues-" + it.status)
                throw IMException.create(it.message)
            }

        }
    }

    fun upload(images: List<File>): Single<Boolean> {
        val fileBuilder = MultipartBody.Builder()
        images.forEach {
            fileBuilder.addPart(RequestBody.create(MultipartBody.FORM, it))
        }

        val files = images.map {
            MultipartBody.Part.createFormData(
                    "files",
                    it.name,
                    RequestBody.create(MultipartBody.FORM, it)
            )
        }
        return client.upload(files).map {
            if (it.isSuccess())
                return@map true
            else
                throw IMException.create(it.message)
        }
    }

    private fun getData(
            key: String, type: String, page: Int = 0, pageSize: Int = 20, account: String, depId: LongArray
    ): Observable<List<HomeItem>> {
        return IMClient.resourceManager.getSign()
                .flatMap { sing ->
                    val param = PkurgApi.PageParam(
                            pageNo = page, deptIds = depId, pageSize = pageSize,
                            account = account, timeStamp = TimeUtils.getServerTime(), sign = sing
                    )
                    client.getPageData(key, type, param).map {
                        if (it.isSuccess()) return@map it.result
                                ?: listOf<HomeItem>() else throw IMException.create(it.message)
                    }
                }
    }

    fun getCookies(): Observable<List<AppCookies>> {
        return IMClient.resourceManager.getSign()
                .flatMap { sing ->
                    client.getCookies(
                            PkurgApi.GetTokenParam(
                                    password = AESUtils.encryptToBase64String(IMClient.password),
                                    account = IMClient.getCurrentUserAccount() ?: "",
                                    timeStamp = TimeUtils.getServerTime(),
                                    sign = sing
                            )
                    ).map {
                        if (it.isSuccess()) return@map it.result
                                ?: listOf<AppCookies>() else throw IMException.create(it.message)
                    }
                }
    }

    //扫码登录pc 传递用户信息
    fun saveRQInfo(qrUuid: String, usercode: String): Observable<PkurgResp<Any>> {
        val param = PkurgApi.RqParam(qruuid = qrUuid, userCode = usercode)
        return client.saveRQInfo(param)
    }

    //获取系统通知待办状态
    fun getWorkflowTask(ids: String): Observable<List<TaskDatasResult>> {
        val param = PkurgApi.WorkflowTaskParam(
                receiveUserId = IMClient.getCurrentUserAccount(),
                ids = ids
        )
        return client.getWorkflowTask(param).map {
            if (it.isSuccess()) return@map it.result?.result
                    ?: listOf<TaskDatasResult>() else throw IMException.create(it.message)
        }
    }

    //语音转文字
    fun getConversionText(base64: String): Observable<ConversionText> {
        val param = PkurgApi.VoiceParam(base64 = base64)
        return client.getConversionText(param).map {
            if (it.result?.code == 1) return@map it.result?.textJson
            else throw IMException.create(it.result?.MSG.toString())
        }
    }

    //获取敏感词库
    fun getViolateText(): Observable<Set<String>> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.GetViolateParam(sign = sign, timeStamp = timeStamp)
                    client.getViolateText(param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create(it.message)
                    }
                }
    }

    //上传敏感词到服务器
    fun saveViolate(type:Int,code:String, msg: String, words: String): Observable<String> {
        var timeStamp = TimeUtils.getServerTime()
        val url = "${IMClient.config.restServer}:${IMClient.config.restServerPort}/rest/violate/save"
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.ViolateParam(sign = sign, timeStamp = timeStamp,type=type,
                            code=code, msg=msg, words=words,
                            sendTime=timeStamp, sendeId=IMClient.getCurrentUserId())
                    client.saveViolate(url = url,params=param).map {
                        return@map it.message
                    }
                }
    }

    //创建投票活动
    fun createVote(title: String, sid: String, invalidTime: Long, options: List<String>): Observable<ResultVoteInfo> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.CreateVoteParam(title = title, sid = sid, createUserId = IMClient.getCurrentUserId(),
                            invalidTime = invalidTime, options = options, sign = sign, timeStamp = timeStamp)
                    client.createVote(param).map {
                        if (it.isSuccess()) return@map it.result?.vote
                        else throw IMException.create(it.message)
                    }
                }
    }

    //获取投票列表
    fun getVoteList(sid: String, pageNo: Int): Observable<List<Record>> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.GetVoteListParam(account = IMClient.getCurrentUserAccount(), sid = sid,
                            pageNo = pageNo, sign = sign, timeStamp = timeStamp)
                    client.getVoteList(param).map {
                        if (it.isSuccess()) return@map it.result?.pageData?.records
                                ?: listOf<Record>()
                        else throw IMException.create(it.message)
                    }
                }
    }

    //获取投票详情
    fun getVoteInfo(voteId: Long): Observable<VoteInfo> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.GetVoteInfoParam(voteId = voteId, sign = sign, timeStamp = timeStamp)
                    client.getVoteInfo(param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create(it.message)
                    }
                }
    }

    //参与投票
    fun toVote(sid: String, voteId: Long, option: String): Observable<PkurgResp<Any>> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.ToVoteParam(sid = sid, userId = IMClient.getCurrentUserId(),
                            voteId = voteId, option = option, sign = sign, timeStamp = timeStamp)
                    client.toVote(param).map {
                        return@map it
                    }
                }
    }

    //删除投票
    fun deleteVote(voteId: Long): Observable<Int> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.DeleteVoteParam(voteId = voteId, sign = sign, timeStamp = timeStamp)
                    client.deleteVote(param).map {
                        return@map it.status
                    }
                }
    }

    //保存接龙
    fun saveChains(chainsDtoHeader: SolitaireTitle, chainsDtoBody: List<ContentItem>): Observable<PkurgApi.ChainsResult> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.SaveChainsParam(chainsDtoHeader = chainsDtoHeader,
                            chainsDtoBody = chainsDtoBody, sign = sign, timeStamp = timeStamp)
                    client.saveChains(param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create(it.message)
                    }
                }
    }

    //获取接龙详情
    fun getChainsInfo(chainsDtoHeader: SolitaireTitle): Observable<PkurgApi.ChainsResult> {
        var timeStamp = TimeUtils.getServerTime()
        return IMClient.resourceManager.getSign()
                .flatMap { sign ->
                    val param = PkurgApi.GetChainsInfoParam(chainsDtoHeader = chainsDtoHeader,
                            sign = sign, timeStamp = timeStamp)
                    client.getChainsInfo(param).map {
                        if (it.isSuccess()) return@map it.result
                        else throw IMException.create(it.message)
                    }
                }
    }

    /**
     * 公众号消息已读同步
     */
    fun officialAccountsMsgRead(graphicId: Long, subscriptionId: Long): Observable<Api.RestResp<GetDeptShowRsp>> {
        return IMClient.resourceManager.getSign()
                .flatMap { s ->
                    val path = "${IMClient.config.officialAccountsServer}:${IMClient.config.officialAccountsServerPort}/officialAccounts/statistics/read"
                    val param = PkurgApi.OfficialAccountsMsgReadParam(graphicIds = mutableListOf(graphicId),subscriptionId=subscriptionId,
                            userId = IMClient.getCurrentUserId(), timeStamp = TimeUtils.getServerTime(), sign = s)
                    client.officialAccountsMsgRead(path, param).map {
                        if (it.isSuccess()) return@map it
                        else throw IMException.create(it.message)
                    }
                }
    }

    /**
     * 同步公众号数据详情
     */
    fun officialAccountsGetBySid(sids: MutableList<String>): Observable<List<Subscription>> {
        val path = "${IMClient.config.officialAccountsServer}:${IMClient.config.officialAccountsServerPort}/officialAccounts/subscription/getBySid"
        val param = PkurgApi.OfficialAccountsGetBySidParam(sids = sids)
        return client.officialAccountsGetBySid(path, param).map {
            if (it.isSuccess()) return@map it.result
            else throw IMException.create(it.message)
        }
    }
}