package com.liheit.im.core.bean

/**
 * Created by daixun on 2018/7/8.
 */

data class UploadParam(
        var type: Int=0,
        var token: String? = null,
        var fileSize: Long? = null,
        var fileName: String? = null,
        var userCode: String? = null,
        var filePath:String? =null,
        var terminal: Int = 2,
        var sign:String="",
        var account:String=""
)
