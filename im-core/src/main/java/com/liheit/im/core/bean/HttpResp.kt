package com.liheit.im.core.bean

/**
 * Created by daixun on 2018/7/2.
 */

data class HttpResp(
    var message: String = "",
    var result: MutableList<String>? = null,
    var code: Int = 0
)


