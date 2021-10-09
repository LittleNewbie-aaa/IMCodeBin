package com.liheit.im.core.protocol

/**
 * Socket请求连接参数
 */
data class AccessResp(
        var servers: MutableList<Server>? = null,
        var token: String = "",//当前用户token
        var extra: String = "",//预留参数
        var ver: String = "",//服务器端 客户端版本
        var url: String = "",//升级客户端下载地址
        var upgrade: Int = 0, //登录成功时，返回升级类型:0-无升级;1-强制升级;2-可选升级
        var info: String = "",//客户端更新描述内容
        var uid: Long = 0,//当前登陆用户id
        var sex: Boolean = true,//性别 true男  false女
        var cname: String = "",//中文名
        var ename: String = ""//英文名
) : Rsp()

data class Server(
        var addr: String = "",//二次连接需要的服务器地址
        var port: Int = 0//二次连接需要的服务器端口
)

data class UpgradeInfo(
        var ver: String = "",//服务器端客户端版本
        var url: String = "",//客户端下载地址
        var force: Boolean = false, //是否强制升级 true-强制升级;false-可选升级
        var info: String = ""//客户端更新描述内容
)