package com.liheit.im.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import com.liheit.im.core.protocol.HeartbeatReq
import com.liheit.im.core.protocol.HeartbeatRsp
import com.liheit.im.utils.Log
import com.liheit.im.utils.TimeUtils
import com.liheit.im.utils.json.fromJson
import com.liheit.im.utils.json.gson


/**
 * 心跳服务管理
 */

class HeartbeatManager(private var client: IMClient) : MsgHandler, BroadcastReceiver() {

    var TIME_INTERVAL = 5 * 60 * 1000
    override fun getHandlerType(): List<Int> {
        return mutableListOf(Cmd.ImpHeartbeatRsp)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v("sendHeartbeat")
        sendHeartbeat()
        setKeepAliveAlarm(getContext(), ACTION_HEARTBEAT, true)
    }

    override fun onMessage(data: String, packageType: Int, sendNumber: Int, cmd: Int) {
        when (cmd) {
            Cmd.ImpHeartbeatRsp -> {
//                Log.e("aaa ImpHeartbeatRsp ${gson.toJson(data)}")
                val resp = data.fromJson<HeartbeatRsp>()
                if (resp.isSuccess()) {
                    TimeUtils.syncServerTime(resp.now, resp.t)
                }
            }
        }
    }

    fun sendHeartbeat() {
        client.sendACK(Cmd.ImpHeartbeatReq, HeartbeatReq(TimeUtils.getLocalTime()))
    }

    /**
     * 开始心跳任务
     */
    fun start() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_HEARTBEAT)
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        intentFilter.addAction("android.net.wifi.STATE_CHANGE")
        getContext().registerReceiver(this, intentFilter)
        setKeepAliveAlarm(getContext(), ACTION_HEARTBEAT, true)
//        startPollingService(getContext(), HeartbeatManager::class.java, 5, ACTION_HEARTBEAT)
    }

    fun stop() {
        stopKeepAliveAlarm(getContext(), ACTION_HEARTBEAT)
        try {
            client.context.unregisterReceiver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopKeepAliveAlarm(context: Context, action: String) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent()
        intent.action = action
        val pendingIntent = PendingIntent.getBroadcast(context, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        //取消正在执行的服务
        manager.cancel(pendingIntent)
    }

    fun setKeepAliveAlarm(context: Context, action: String, interVal: Boolean) {
        // 防止4.4以下的重复执行setRepeating
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && interVal) {
            return
        }


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Intent local = new Intent(context, KeepAliveReceiver.class);
        val local = Intent()
        local.action = action
        local.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)// 表示包含未启动的App

        val pendingIntent = PendingIntent.getBroadcast(context, 999, local, PendingIntent.FLAG_UPDATE_CURRENT)

        // 此处必须使用SystemClock.elapsedRealtime，否则闹钟无法接收
        var triggerAtMillis = SystemClock.elapsedRealtime()

        // 更新开启时间
        if (interVal) {
            triggerAtMillis += TIME_INTERVAL.toLong()
        }

        // pendingIntent 为发送广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        } else {// api19以前还是可以使用setRepeating重复发送广播
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, TIME_INTERVAL.toLong(), pendingIntent)
        }
        return
    }

    private fun getContext(): Context {
        return client.context
    }

    companion object {
        const val ACTION_HEARTBEAT = "com.dx.im.ACTION_HEARTBEAT"
    }

}
