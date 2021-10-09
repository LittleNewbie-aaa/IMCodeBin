package com.im.bizvideolib

import android.os.Parcelable
import com.liheit.im.core.IMClient
import com.liheit.im.core.IMException
import com.liheit.im.core.http.LogInterceptor
import com.liheit.im.utils.DateUtil
import com.liheit.im.utils.SecurityUtils
import com.liheit.im.utils.TimeUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory2
import retrofit2.http.*
import retrofit2.http.Headers
import java.util.concurrent.TimeUnit

object BizConfVideoClient {
    var client: BizConfApi

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.readTimeout(15, TimeUnit.SECONDS)
        clientBuilder.writeTimeout(15, TimeUnit.SECONDS)
        clientBuilder.connectTimeout(15, TimeUnit.SECONDS)
        clientBuilder.followSslRedirects(true)

        if (BuildConfig.DEBUG) {
            clientBuilder.addNetworkInterceptor(LogInterceptor().setLevel(LogInterceptor.Level.BODY))
//            clientBuilder.addNetworkInterceptor(HttpLoggingInterceptor(HttpLogger()).setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        val builder = Retrofit.Builder()
                .baseUrl(HttpUrl.parse("https://api.bizvideo.cn/openapi/v1/"))
                .addConverterFactory(GsonConverterFactory2.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(clientBuilder.build())

        val retrofit = builder.build()
        client = retrofit.create<BizConfApi>(BizConfApi::class.java)
    }

    fun getConfReservation(uId: Long?, userName: String?): Observable<BizConfResp<ConfReservationItem>> {
        var timeStamp = TimeUtils.getServerTime()
        var startTime = DateUtil.msFormatTime(timeStamp)
        var token = SecurityUtils.get32MD5Str("${uId}|${BizVideoClient.APIKey}|${timeStamp}")
        val params = mutableMapOf<String, String>(
                "userId" to uId.toString(),//客户系统中的用户唯一 ID（客 户系统唯一 ID）
                "userName" to userName.toString(),//客户系统中的用户名称
                "token" to token,//用于请求验证
                "timeStamp" to timeStamp.toString(),//时间戳,用于 token 加密
                "siteSign" to BizVideoClient.siteSign,//站点标识
                "confName" to "${userName}发起的即时会议",//会议名称
                "startTime" to startTime.toString(),//开始时间
                "duration" to "60",//会议持续时间（分钟）
                "confParties" to "15",//会议方数
                "optionJbh" to "1",//是否允许先于主持人入会0 否	1 是
                "optionVideoHost" to "1",//开启主持人视频 0 关闭视频 1 开启视频
                "optionVideoParticipants" to "1",//开启参会者视频 0 关闭视频 1 开启视频
                "upCompatible" to "y"//配置方数是否向上兼容。y 允许向上兼容。
        )
//        com.liheit.im.utils.Log.e("aaa ${gson.toJson(params)}")
        return client.getConfReservation(params).map { return@map it }
    }

    fun getConfDetailByConfId(confId: String, uId: String): Observable<BizConfResp<ConfReservationItem>> {
//        var uId = IMClient.loginUserId()
        var timeStamp = TimeUtils.getServerTime()
        var token = SecurityUtils.get32MD5Str("${uId}|${BizVideoClient.APIKey}|${timeStamp}")
        val params = mutableMapOf<String, String>(
                "userId" to uId,//客户系统中的用户唯一 ID（客 户系统唯一 ID）
                "token" to token,//用于请求验证
                "timeStamp" to timeStamp.toString(),//时间戳,用于 token 加密
                "siteSign" to BizVideoClient.siteSign,//站点标识
                "confId" to confId//会议 ID
        )
        return client.getConfDetailByConfId(params).map { return@map it }
    }

    fun cancelConf(confId: String, uId: String): Observable<BizConfResp<@JvmSuppressWildcards Any>> {
        var timeStamp = TimeUtils.getServerTime()
        var token = SecurityUtils.get32MD5Str("${uId}|${BizVideoClient.APIKey}|${timeStamp}")
        val params = mutableMapOf<String, String>(
                "userId" to uId,//客户系统中的用户唯一 ID（客 户系统唯一 ID）
                "token" to token,//用于请求验证
                "timeStamp" to timeStamp.toString(),//时间戳,用于 token 加密
                "siteSign" to BizVideoClient.siteSign,//站点标识
                "confId" to confId//会议 ID
        )
        return client.cancelConf(params).map { return@map it }
    }

    //保存会议信息（到迈动）
    fun saveMeetingInstant(conf: ConfReservationItem): Observable<String> {
        val path = if(BuildConfig.siteSign=="hy-online"){
            //生产服
            "https://oams.hy-online.com:8443/huayuanOA/api/meeting/saveMeetingInstant"
        }else{
            "http://oamstest.hy-online.com:8443/huayuanOA/api/meeting/saveMeetingInstant"
        }
        val params = BizConfApi.MeetingInfoParam(activityid = "", beforeTime = conf.beforeTime, confDelFlag = conf.confDelFlag,
                confId = conf.confId, confName = conf.confName, confNumber = conf.confNumber, confParties = conf.confParties,
                confPassword = conf.confPassword, confStatus = conf.confStatus, duration = conf.duration, h323pwd = conf.h323pwd,
                hostStartUrl = conf.hostStartUrl, id = "", joinUrl = conf.joinUrl, optionJbh = conf.optionJbh,
                optionVideoHost = conf.optionVideoHost, optionVideoParticipants = conf.optionVideoParticipants,
                protocolHostStartUrl = conf.protocolHostStartUrl, protocolJoinUrl = conf.protocolJoinUrl,
                rcIps = conf.rcIps, roomId = conf.roomId, startTime = conf.startTime, userid = IMClient.getCurrentUserId().toString(),
                username = IMClient.getCurrentUser()?.cname, watchUrl = conf.watchUrl, webClientjoin = conf.webClientjoin)
//        val requestBody: RequestBody = MultipartBody.create(MediaType.parse("text/plain"), gson.toJson(params))
        return client.saveMeetingInstant(path, params)
                .map {
                    if(it.isSuccess()) return@map it.errmsg
                    else throw IMException.create(it.errmsg)
                }
    }
}

interface BizConfApi {
    //预约会议
    @FormUrlEncoded
    @POST("confReservation")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    fun getConfReservation(@FieldMap params: Map<String, String>): Observable<BizConfResp<ConfReservationItem>>

    //获取会议详情
    @GET("getConfDetailByConfId")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    fun getConfDetailByConfId(@QueryMap option: Map<String, String>): Observable<BizConfResp<ConfReservationItem>>

    //取消会议
    @FormUrlEncoded
    @POST("cancelConf")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    fun cancelConf(@FieldMap option: Map<String, String>): Observable<BizConfResp<@JvmSuppressWildcards Any>>

    //保存会议信息（向迈动发送会议信息）
    @POST
    @Headers("Content-Type:application/json; charset=utf-8")
    fun saveMeetingInstant(@Url url: String, @Body body: MeetingInfoParam): Observable<MeetingResp<Any>>

    //保存会议请求参数
    data class MeetingInfoParam(
            val activityid: String? = "",
            val protocolHostStartUrl: String,//主持人协议入会连接 用于 SDK
            val hostStartUrl: String,//主持人入会连接
            val confStatus: Int? = -1,//会议状态 0 预约成功 2 正在召开 3 已结束 9 取消的会议
            val roomId: String? = null,// 会 议 室 id
            val confPassword: String? = null,//会议密码
            val optionJbh: String? = null,//是否先与主持人入会
            val startTime: String? = null,//开始时间
            val optionVideoParticipants: String,
            val confId: String? = null,// 会 议 id
            val duration: Int,//会议时长
            val id:String?="",
            val h323pwd: String? = null,//h323 密码
            val confName: String? = null,
            val confParties: Int,//会议方数
            val userId: String? = null,//客户传入的 userId
            val joinUrl: String,//参会人入会连接
            val userName: String? = null,//主持人名字
            val confDelFlag: String? = null,//会议删除状态 1 未删除 2 已删除
            val rcIps: String? = null,//rc 接入 ip
            val optionVideoHost: String,// 是否开启主持人视频
            val protocolJoinUrl: String,//协议入会连接适用于 SDK 入会
            val confNumber: String,//视频会议号
            val videoConf: String? = null,//语音会议号
            val videoPhone: String? = null,//语音号码
            val beforeTime: String? = null, //提前开始时间
            val userid: String? = "",
            val username: String? = "",
            val watchUrl: String? = "",
            val webClientjoin: String? = ""
    )
}

/**
 * 视频会议返回数据
 */
class BizConfResp<T>(
        var message: String = "",
        var result: T? = null,
        var data: T? = null,
        var status: Int = 0
) {
    fun isSuccess(): Boolean {
        return status == 100
    }
}

/**
 * 保存视频会议返回数据
 */
class MeetingResp<T>(
        var errcode: Int = 0,
        var errmsg: String = ""
){
    fun isSuccess(): Boolean {
        return errcode == 0
    }
}

class ResultData<ConfReservationItem>(
        var data: ConfReservationItem? = null
)

/**
 * 视频会议信息
 */
@Parcelize
data class ConfReservationItem(
        val protocolHostStartUrl: String,//主持人协议入会连接 用于 SDK
        val hostStartUrl: String,//主持人入会连接
        val confStatus: Int? = -1,//会议状态 0 预约成功 2 正在召开 3 已结束 9 取消的会议
        val roomId: String? = null,// 会 议 室 id
        val confPassword: String? = null,//会议密码
        val optionJbh: String? = null,//是否先与主持人入会
        val startTime: String? = null,//开始时间
        val optionVideoParticipants: String,
        val confId: String? = null,// 会 议 id
        val duration: Int,//会议时长
        val h323pwd: String? = null,//h323 密码
        val confName: String? = null,
        val confParties: Int,//会议方数
        val userId: String? = null,//客户传入的 userId
        val joinUrl: String,//参会人入会连接
        val userName: String? = null,//主持人名字
        val confDelFlag: String? = null,//会议删除状态 1 未删除 2 已删除
        val rcIps: String? = null,//rc 接入 ip
        val optionVideoHost: String,// 是否开启主持人视频
        val protocolJoinUrl: String,//协议入会连接适用于 SDK 入会
        val confNumber: String,//视频会议号
        val videoConf: String? = null,//语音会议号
        val videoPhone: String? = null,//语音号码
        val beforeTime: String? = null, //提前开始时间
        val watchUrl: String? = null,//直播观看地址
        val webClientjoin: String? = ""
) : Parcelable