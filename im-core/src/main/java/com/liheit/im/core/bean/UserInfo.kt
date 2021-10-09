package com.liheit.im.core.bean

/**
 * Created by daixun on 2018/6/14.
 */
data class UserInfo(
        var token: String = "",//当前用户token
        var ver: String = "",//客户端当前版本号
        var url: String = "",//升级客户端下载地址
        var upgrade: Int = 0, //登录成功时，返回升级类型:0-无升级;1-强制升级;2-可选升级
        var info: String = "",//客户端更新描述内容
        var uid: Long = 0,//当前登陆用户id
        var sex: Boolean = true,//性别 true男  false女
        var cname: String = "",//中文名
        var ename: String = ""//英文名
)