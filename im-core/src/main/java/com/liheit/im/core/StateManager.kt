package com.liheit.im.core

/**
 * Created by daixun on 2018/7/1.
 */

object StateManager {

    private var netConnected = false
    private var socketConnected = false


    fun isSocketConnected(): Boolean {
        return socketConnected
    }

    fun notifyNetState(isNetConnected: Boolean) {
        netConnected = isNetConnected
    }

    fun notifySocketState(isSocketConnected: Boolean) {
        socketConnected = isSocketConnected

    }

    fun isConnected(): Boolean {
        return netConnected && socketConnected
    }

    fun addStateListener() {

    }


    interface OnStateChangeListener {
        fun onStateChange(isConnected: Boolean) {

        }
    }

}
