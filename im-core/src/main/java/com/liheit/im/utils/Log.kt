package com.liheit.im.utils


/**
 * Created by daixun on 2018/6/15.
 */

class Log {

    companion object {
        val TAG = "IM"
        @JvmStatic
        fun e(msg: String?) {
            com.tencent.mars.xlog.Log.e(TAG, msg)
        }

        @JvmStatic
        fun e(msg: String, e: Throwable) {
            com.tencent.mars.xlog.Log.e(TAG, msg, e)
        }

        @JvmStatic
        fun d(msg: String) {
            com.tencent.mars.xlog.Log.d(TAG, msg)
        }

        @JvmStatic
        fun i(msg: String) {
            com.tencent.mars.xlog.Log.i(TAG, msg)
        }

        @JvmStatic
        fun w(msg: String) {
            com.tencent.mars.xlog.Log.w(TAG, msg)
        }

        @JvmStatic
        fun v(msg: String) {
            com.tencent.mars.xlog.Log.v(TAG, msg)
        }
    }


}
