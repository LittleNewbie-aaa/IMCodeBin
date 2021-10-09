package com.liheit.im.core.protocol

/**
 * Socket请求连接参数
 */
data class AccessReq(
        val term: Byte = 2,//终端类型 bit[0:3]:1 为 iOS; 2 为 Android;
        var account: String = "",//当前登陆账号
        var psw: String = "",//登录密码，需要 BASE64(AES(psw))，Public Key: xxx
        var ver: String = "", //客户端当前的版本号
        var lang: String = "cn"//客户端语言:en,cn，默认为 cn
)