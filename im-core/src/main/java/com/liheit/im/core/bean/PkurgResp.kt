package com.liheit.im.core.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 对基本返回的封装
 */
class PkurgResp<T>(
        var message: String = "",
        var result: T? = null,
        var status: Int = 0
) {
    fun isSuccess(): Boolean {
        return status == 0
    }
}

data class AppCookies(
        val key: String,
        val domain: String,
        val httpDomain: String,
        val value: String
)

@Parcelize
data class PageData(
        val key: String? = null,//	标示符（后面将推送刷新以及展示未读数量）
        val principal: String,//	数据原始ID
        val groupId: Long,//	数据分组ID
        val groupName: String,//	数据分组名称
        val title: String,//	标题
        val summary: String,//	摘要；轻应用暂无
        val logo: String,//	图标
        val pcUrl: String,//	PC端打开地址
        val appUrl: String,//	移动端打开地址
        val sort: Int,//	顺序
        val state: Int,//	状态（0:禁用 1:启用）
        val pv: Long,//	访问量；轻应用暂无
        val author: String,//	作者；轻应用暂无
        val updateTime: Long//	最后修改时间（UTC）
) : Parcelable

@Parcelize
data class HomeItem(
        val key: String? = null,//	标示符（后面将推送刷新以及展示未读数量）
        val principal: String? = null,//	数据原始ID
        val groupId: Long? = null,//	数据分组ID
        val groupName: String? = null,//	数据分组名称
        val title: String? = null,//	标题
        val summary: String? = null,//	摘要；轻应用暂无
        val logo: String? = null,//	图标
        val pcUrl: String? = null,//	PC端打开地址
        val appUrl: String? = null,//	移动端打开地址
        val type: Int = 0,// 轻应用独有 更多打开方式 0 网页 1原生
        val sort: Int? = null,//	顺序
        val state: Int? = null,//	状态（0:禁用 1:启用）
        val pv: Long? = null,//	访问量；轻应用暂无
        val author: String? = null,//	作者；轻应用暂无
        val updateTime: Long? = null,//	最后修改时间（UTC）
        //统计数据
        val ltitle: String? = null,
        val ltotal: String? = null,
        val lunit: String? = null,
        val lurl: String? = null,
        val rtitle: String? = null,
        val rtotal: String? = null,
        val runit: String? = null,
        val rurl: String? = null
) : Parcelable

data class HomeData(
        val key: String,    //标示符（后面将推送刷新以及展示未读数量）
        val type: Int,//	"类型
        /*0:卡片布局
        1:横向布局
        2:网页布局
        3:应用布局"*/
        val moreType: Int,//更多打开方式 0 网页 1原生
        val title: String,//	标题
        val summary: String,//	摘要
        val logo: String,//	图标
        val moreLogo: String? = "",
        val dataUrl: String,//	获取数据地址
        val moreUrl: String,//	更多打开地址获取数据地址
        val size: Int,//	初始页展示数量
        val sort: Int,//	顺序
        val updateTime: Long,//	最后修改时间（UTC）
        val data: List<HomeItem>    //首页数据（非网页布局才存在该节点）
)

@Parcelize
data class TaskDatas<T>(
        val beginPage: Int,
        val currentPage: Int,
        val endPage: Int,
        val result: List<TaskDatasResult>,
        val total: Int,
        val totalPage: Int,
        val usedTime: Int
) : Parcelable

@Parcelize
data class TaskDatasResult(
        val createTime: Long,
        val createUserId: String,
        val createUserName: String,
        val dbUrl: String,
        val id: Int,
        val instanceId: String,
        val isCy: Int,
        val market: Int,
        val mobileRedirectUrl: String,
        val openStatus: Int,
        val pcRedirectUrl: String,
        val receiveTime: Long,
        val receiveUserId: String,
        val receiveUserName: String,
        val status: Int,
        val systemCode: String,
        val systemId: Int,
        val systemName: String,
        val taskId: String,
        val taskStatus: Int,
        val title: String,
        val ts: Long,
        val workflowStatus: String,
        val workflowType: String
) : Parcelable

data class ConversionResultData(
        val code: Int,
        var textJson: ConversionText? = null,
        var MSG: String
)

data class ConversionText(
        var RequestId: String,
        var Result: String
)

data class ViolateText(
        var text: Set<String>
)