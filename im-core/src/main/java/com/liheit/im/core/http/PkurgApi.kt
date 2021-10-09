package com.liheit.im.core.http

import com.liheit.im.core.bean.*
import com.liheit.im.core.protocol.GetDeptShowRsp
import com.pkurg.lib.ui.solitaire.ContentItem
import com.pkurg.lib.ui.solitaire.SolitaireTitle
import com.pkurg.lib.ui.vote.CreateVoteResult
import com.pkurg.lib.ui.vote.VoteInfo
import com.pkurg.lib.ui.vote.VoteResult
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface PkurgApi {
    @POST("/rest/api/model/list")
    fun getHomeData(@Body param: HomeParam): Observable<PkurgResp<List<HomeData>>>

    @POST("/rest/api/token/gen")
    fun getCookies(@Body param: GetTokenParam): Observable<PkurgResp<List<AppCookies>>>

    @POST("/rest/api/{key}/{type}")
    fun getPageData(@Path("key") key: String, @Path("type") type: String,
                    @Body param: PageParam): Observable<PkurgResp<List<HomeItem>>>

    @Multipart
    @POST("/rest/api/feedback/push")
    fun feedback(@PartMap maps: Map<String, @JvmSuppressWildcards RequestBody>,
                 @Part files: List<MultipartBody.Part>): Single<PkurgResp<@JvmSuppressWildcards Any>>

    @Multipart
    @POST("/rest/api/logFile/push")
    fun logFilePush(@PartMap maps: Map<String, @JvmSuppressWildcards RequestBody>, @Part files: MultipartBody.Part): Single<PkurgResp<@JvmSuppressWildcards Any>>

    @Multipart
    @POST("/rest/api/feedback/upload")
    fun upload(@Part files: List<MultipartBody.Part>): Single<PkurgResp<@JvmSuppressWildcards Any>>

    /**
     * 登陆pc调用，上传二维码信息
     */
    @POST("/rest/QRCode/saveQRInfo")
    fun saveRQInfo(@Body rqParam: RqParam): Observable<PkurgResp<Any>>

    /**
     * 语音消息转文字
     */
    @POST("/rest/api/asr/getResult")
    fun getConversionText(@Body param: VoiceParam): Observable<PkurgResp<ConversionResultData>>

    /**
     * 获取系统通知待办状态
     */
    @POST("/rest/api/shidi/getWorkflowTask")
    fun getWorkflowTask(@Body param: WorkflowTaskParam): Observable<PkurgResp<TaskDatas<Any>>>

    /**
     * 获取敏感词库
     */
    @POST("/rest/violate/downLoad")
    fun getViolateText(@Body param: GetViolateParam): Observable<PkurgResp<Set<String>>>

    //保存包含敏感词的消息
    @POST
    fun saveViolate(@Url url: String, @Body() params:ViolateParam): Observable<PkurgResp<String>>

    /**
     * 创建一个新的投票活动
     */
    @POST("/rest/api/vote/create")
    fun createVote(@Body param: CreateVoteParam): Observable<PkurgResp<CreateVoteResult>>

    /**
     * 获取投票活动列表
     */
    @POST("/rest/api/vote/page")
    fun getVoteList(@Body param: GetVoteListParam): Observable<PkurgResp<VoteResult>>

    /**
     * 获取投票活动详情
     */
    @POST("/rest/api/vote/getVote")
    fun getVoteInfo(@Body param: GetVoteInfoParam): Observable<PkurgResp<VoteInfo>>

    /**
     * 用户投票
     */
    @POST("/rest/api/vote/voted")
    fun toVote(@Body param: ToVoteParam): Observable<PkurgResp<Any>>

    /**
     * 删除投票活动
     */
    @POST("/rest/api/vote/deleteVote")
    fun deleteVote(@Body param: DeleteVoteParam): Observable<PkurgResp<Any>>

    /**
     * 保存接龙
     */
    @POST("/rest/api/chains/save")
    fun saveChains(@Body param: SaveChainsParam): Observable<PkurgResp<ChainsResult>>

    /**
     * 获取接龙详情
     */
    @POST("/rest/api/chains/get")
    fun getChainsInfo(@Body param: GetChainsInfoParam): Observable<PkurgResp<ChainsResult>>

    /**
     * 公众号消息已读同步
     */
    @POST()
    fun officialAccountsMsgRead(@Url url: String, @Body param: OfficialAccountsMsgReadParam): Observable<Api.RestResp<GetDeptShowRsp>>

    /**
     * 同步公众号数据详情
     */
    @POST()
    fun officialAccountsGetBySid(@Url url: String, @Body param: OfficialAccountsGetBySidParam): Observable<Api.RestResp<List<Subscription>>>

    data class PageParam(
            val pageNo: Int = 1,
            val pageSize: Int = 10,
//            val userType:Int=0,//"用户类型 0:内部用户 1:外部用户 2:虚拟用户"
            val deptIds: LongArray,//当前用户的所属部门ID（如果多个部门全部带上）
            val terminal: Int = 2,
            val account: String,
            val timeStamp: Long,
            val sign: String
    )

    data class GetTokenParam(
            val password: String,
            val account: String,
            val timeStamp: Long,
            val sign: String,
            val terminal: Int = 2
    )

    data class HomeParam(
            val deptIds: LongArray,
            val account: String,
            val timeStamp: Long,
            val sign: String,
            val terminal: Int = 2
    )

    //扫描二维码传递的参数
    data class RqParam(
            val qruuid: String,
            val userCode: String
    )

    //获取系统通知待办状态参数
    data class WorkflowTaskParam(
            val start: Int = 0,
            val pageSize: Int = 100,
            val receiveUserId: String? = "",
            val taskStatus: Int = 0,
            val ids: String
    )

    //音频文件base64数据
    data class VoiceParam(
            var base64: String
    )

    //获取敏感词库请求参数
    data class GetViolateParam(
            var sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //上传敏感词参数
    data class ViolateParam(
            val sign: String,
            val type: Int,//会话类型 0-单聊 1-群聊
            val code: String,//当type=0时，code代表userid   当type=1时，code代表sid
            val sendeId: Long,
            val terminal: Int = 2,
            val timeStamp: Long,
            val sendTime: Long,
            val msg: String,
            val words: String
    )

    //创建投票请求参数
    data class CreateVoteParam(
            var title: String,
            var sid: String,
            var createUserId: Long,
            var invalidTime: Long,
            var options: List<String>,
            var sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //获取投票列表参数
    data class GetVoteListParam(
            var account: String,
            var sid: String,
            var pageNo: Int,
            var pageSize: Int = 10,
            var sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //获取投票详情参数
    data class GetVoteInfoParam(
            var voteId: Long,
            val sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //参与投票参数
    data class ToVoteParam(
            var sid: String,
            var userId: Long,
            var voteId: Long,
            var option: String,
            val sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //删除投票参数
    data class DeleteVoteParam(
            var voteId: Long,
            val sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //保存接龙参数
    data class SaveChainsParam(
            var chainsDtoHeader: SolitaireTitle,
            var chainsDtoBody: List<ContentItem>,
            val sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //接龙返回数据
    data class ChainsResult(
            val chains: SolitaireTitle,
            val itemList: List<ContentItem>
    )

    //获取接龙详情
    data class GetChainsInfoParam(
            var chainsDtoHeader: SolitaireTitle,
            val sign: String,
            val terminal: Int = 2,
            val timeStamp: Long
    )

    //公众号消息已读同步参数
    data class OfficialAccountsMsgReadParam(
            val graphicIds:MutableList<Long>,//图文id
            val subscriptionId:Long,//公众号主键id
            val userId:Long,//用户id
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp:Long,//时间戳（格式为long，毫秒）
            val sign: String//必填，请求签名
    )

    //同步公众号数据详情参数
    data class OfficialAccountsGetBySidParam(
            val sids: MutableList<String>
    )
}