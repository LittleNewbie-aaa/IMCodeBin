package com.liheit.im.core

/**
 * Created by daixun on 2018/9/8.
 */

class Commond(
        var cmd: Int,
        var sn: Long,
        var cmdData: Any,
        var callback: CommondCallback
)
