package com.liheit.im.utils

/**
 * Created by daixun on 2018/6/23.
 */

object TimeUtils {


    var timeDiff = 0L


    fun syncServerTime(serverTime: Long, sendTime: Long) {
        var now = getLocalTime()
        Integer.MAX_VALUE
        timeDiff = serverTime - (now + sendTime) / 2
    }

    fun getServerTime(): Long {
        var t=System.currentTimeMillis()
//        Log.e("aaa t=$t   /// timeDiff=$timeDiff")
        return t + timeDiff
    }

    fun getLocalTime(): Long {
        return System.currentTimeMillis()
    }

}
