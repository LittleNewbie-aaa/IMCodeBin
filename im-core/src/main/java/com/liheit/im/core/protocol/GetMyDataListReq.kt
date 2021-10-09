package com.liheit.im.core.protocol

import android.support.annotation.IntDef

/**
 * Created by daixun on 2018/11/7.
 */


/*所有的个人设置，都无增量修改，每次都必须更新整条数据。
0、常用群:key 为 sid，value 默认为:{}
1、常用部门:key 为 did，value 默认为:{}
2、常用联系人:key 为 uid，value 默认为:{}
3、自定义组:key 为 guid，value 为:{\"ids\":[1,3,5,6],\"title\":\"My group\"}
4、群设置同步:key 为 sid，value 为:{\"top\":1,\"shield\":1}
20、收藏:key 为 mid，value 为消息 json 字符串，收藏无即时同步到另一端*/

const val MY_DATA_MOST_USE_SESSION = 0
const val MY_DATA_MOST_USE_DEPARTMENT = 1
const val MY_DATA_MOST_USE_USER = 2
const val MY_DATA_MOST_CUSTOM_GROUP = 3
const val MY_DATA_SESSION_SETTING = 4
const val MY_DATA_COLLECT = 20

@IntDef(value = [
    MY_DATA_COLLECT,
    MY_DATA_SESSION_SETTING,
    MY_DATA_MOST_CUSTOM_GROUP,
    MY_DATA_MOST_USE_DEPARTMENT,
    MY_DATA_MOST_USE_USER,
    MY_DATA_MOST_USE_SESSION])
@Retention(AnnotationRetention.SOURCE)
annotation class UserSettingType

data class GetMyDataListReq(var t: Long, @UserSettingType var type: Int)

data class MyData(@UserSettingType var type: Int,
                  var del: Boolean? = null,
                  var key: String = "",
                  var values: Map<String, Any>?,
                  var value: String = ""

) : Rsp()

data class ModifyMyDataReq(@UserSettingType var type: Int,//设置修改类型
                           var del: Boolean? = null,//是否删除
                           var key: String = "",
                           var value: Any? = null)

data class ModifyMyDataRsp(@UserSettingType var type: Int,
                           var del: Boolean? = null,
                           var key: String = "",
                           var value: Any? = null) : Rsp()

data class SessionSetting(
        val top: Int = 0,  //置顶
        val shield: Int = 0, //消息免打扰
        var noticetime: Long = 0L //已读群公告时间戳
) {
    val isNotification: Boolean
        get() = shield == 0

    val isTop: Boolean
        get() = top == 1
}

data class SessionOfterSetting(
        val ofter: Boolean = false// 常用群组
)

