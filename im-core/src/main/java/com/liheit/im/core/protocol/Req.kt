package com.liheit.im.core.protocol

/**
 * Created by daixun on 2018/6/14.
 */
open class Req(
        val term: Byte = 2,//终端类型 bit[0:3]:1 为 iOS; 2 为 Android;
        var ver: String = "1.23.423", //客户端当前的版本号
        var lang: String = "cn",//客户端语言:en,cn，默认为 cn
        var testver: Int = 20201102
)