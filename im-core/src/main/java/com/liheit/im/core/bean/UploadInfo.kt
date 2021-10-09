package com.liheit.im.core.bean

/**
 * Created by daixun on 2018/7/4.
 */

data class UploadInfo(
        var id: String = "",
        val md5: String = "",
        val token: String = "",
        var bytes: Long = 0,
        var width: Int = 0,
        var height: Int = 0,
        var process: Int = 0
)
