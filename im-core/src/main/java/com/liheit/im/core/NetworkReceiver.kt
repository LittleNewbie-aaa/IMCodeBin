package com.liheit.im.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by daixun on 2018/9/23.
 */

class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            ChatService.createNetChangeIntent(it)
        }
    }
}
