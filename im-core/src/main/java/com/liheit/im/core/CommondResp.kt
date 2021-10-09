package com.liheit.im.core

import com.liheit.im.core.protocol.PackageType

/**
 * Created by daixun on 2018/9/9.
 */

data class CommondResp(
        var data: String? = null,//封装后返回数据内容
        @PackageType.Type var packageType: Int = 0,//返回数据类型
        var sendNumber: Int = 0,
        var cmd: Int = 0,//返回数据命令
        var rawData: ByteArray
) {

    companion object {
        @JvmStatic
        val NULL = CommondResp(rawData = byteArrayOf())
    }
}

data class ResultData(
    var result:Long
)