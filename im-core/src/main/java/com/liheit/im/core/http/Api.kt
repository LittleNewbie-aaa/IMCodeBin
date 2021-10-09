package com.liheit.im.core.http

import com.liheit.im.core.bean.HttpResp
import com.liheit.im.core.protocol.GetDeptListRsp
import com.liheit.im.core.protocol.GetDeptShowRsp
import com.liheit.im.core.protocol.GetDeptUserListRsp
import com.liheit.im.core.protocol.GetUserInfoRsp
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


/**
 * Created by daixun on 2018/7/2.
 * 进行上传下载的
 */

interface Api {


    @POST
    fun getSign(@Url url: String, @Body() params: Map<String, String>): Observable<HttpResp>

    @Streaming
    @POST()
    fun downloadFile(
            @Url url: String, @Header("Range") range: String?, @Header("type") type: Int,
            @Header("token") fileToken: String?, @Header("size") size: Long?,
            @Header("name") filename: String?, @Header("source") source: Boolean?,
            @Header("userCode") userCode: String?, @Header("terminal") terminal: Int = 2,
            @Header("account") account: String, @Header("sign") sign: String
    ): Observable<Response<ResponseBody>>

    @Streaming
    @GET
    fun downloadFileByUrl(@Url fileUrl: String): Observable<Response<ResponseBody>>

    @POST()
    fun uploadInit(
            @Url url: String, @Header("type") type: Int,
            @Header("token") fileMD5: String? = null,
            @Header("size") fileSize: Long? = null,
            @Header("name") fileName: String? = null,
            @Header("userCode") userCode: String? = null,
            @Header("terminal") terminal: Int = 2,
            @Header("account") account: String, @Header("sign") sign: String
    ): Observable<FResp>

    @Multipart
    @POST()
    fun uploadFile(
            @Url url: String,
            @Header("type") type: Int,
            @Header("token") fileMD5: String? = null,
            @Header("size") fileSize: Long? = null,
            @Header("name") fileName: String? = null,
            @Header("userCode") userCode: String? = null,
            @Header("terminal") terminal: Int = 2,
            @Header("account") account: String,
            @Header("sign") sign: String,
            @Part file: MultipartBody.Part
    ): Observable<FResp>

    @Streaming
    @GET()
    fun download(@Url url: String): Observable<ResponseBody>

    data class FResp(
            var status: Int = 0,
            var message: String = ""
    ) {
        fun isSuccess(): Boolean {
            return status == 0
        }
    }

    /**
     * java 同步通讯录（用户数据，部门数据，部门人员数据，部门权限数据）
     */
    @POST()
    fun getContactsData(@Url url: String, @Body param: ContactsDataParam): Observable<RestResp<ContactsData>>

    /**
     * java 获取用户数据（全量/增量更新）
     */
    @POST()
    fun getContactsUserInfoData(@Url url: String, @Body param: ContactsUserDataParam): Observable<RestResp<GetUserInfoRsp>>

    /**
     * java 获取部门数据（全量/增量更新）
     */
    @POST()
    fun getContactsDeptListData(@Url url: String, @Body param: ContactsDeptListParam): Observable<RestResp<GetDeptListRsp>>

    /**
     * java 获取部门用户数据（全量/增量更新）
     */
    @POST()
    fun getContactsDeptUserListData(@Url url: String, @Body param: ContactsDeptUserListParam): Observable<RestResp<GetDeptUserListRsp>>

    /**
     * java 获取部门权限
     */
    @POST()
    fun getContactsDeptShowData(@Url url: String, @Body param: ContactsDeptShowParam): Observable<RestResp<GetDeptShowRsp>>

    class RestResp<T>(
            var message: String = "",
            var result: T? = null,
            var status: Int = 0
    ) {
        fun isSuccess(): Boolean {
            return status == 0
        }
    }

    //获取通讯录信息请求参数
    data class ContactsDataParam(
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp: Long,//必填，时间戳（秒数）
//            val account: String? = "",//可选 操作者账号
//            val password: String? = "",//可选，用户密码
            val userInfoReq: Any,//可选
            val deptListReqDto: Any,//可选
            val deptUserListReqDto: Any,//可选
            val deptShowReqDto: Any,//部门用户列表
            val sign: String//必填，请求签名
    )

    //获取用户数据请求参数
    data class ContactsUserDataParam(
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp: Long,//必填，时间戳（秒数）
            val t: Long,//t=0 全量获取 t>0 增量获取
            val ids: List<Long>? = mutableListOf(),//查询用户id集合
            val sign: String//必填，请求签名
    )

    //获取部门数据请求参数
    data class ContactsDeptListParam(
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp: Long,//必填，时间戳（秒数）
            val t: Long,//t=0 全量获取 t>0 增量获取
            val ids: List<Long>? = mutableListOf(),//查询用户id集合
            val sign: String//必填，请求签名
    )

    //获取部门用户数据请求参数
    data class ContactsDeptUserListParam(
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp: Long,//必填，时间戳（秒数）
            val t: Long,//t=0 全量获取 t>0 增量获取
            val sign: String//必填，请求签名
    )

    //获取部门权限请求参数
    data class ContactsDeptShowParam(
            val terminal: Int = 2,//必填，操作客户端（0:异构系统 1:IOS 2:Android 3:Mac 4:Windows）
            val timeStamp: Long,//必填，时间戳（秒数）
            val userId: Long,//当前登陆用户id
            val sign: String//必填，请求签名
    )
}


//获取通讯录信息返回数据
data class ContactsData(
        val userInfoResult: GetUserInfoRsp,//用户信息列表
        val deptListResult: GetDeptListRsp,//部门信息列表
        val deptUserListResult: GetDeptUserListRsp,//部门用户列表
        val deptShowListResult: GetDeptShowRsp//用户权限列表
)