package com.liheit.im.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import com.liheit.im.core.protocol.HeartbeatReq
import com.liheit.im.utils.Log
import com.liheit.im.utils.TimeUtils
import com.liheit.im.utils.alarmManager
import com.liheit.im_core.R

/**
 * 聊天服务
 */

class ChatService : Service() {

    companion object {
        const val ACTION_CHECK_CONNECT = "checkConnection"
        const val ACTION_STOP_CHECK_CONNECT = "stopCheckConnection"
        const val ACTION_HEARTBEAT = "heartbeat"
        const val ACTION_STOP_HEARTBEAT = "StopHeartbeat"
        const val ACTION_NETWORK_CHANGE = "networkChange"

        fun createNetChangeIntent(context: Context) {
            try {
                var intent = Intent(context, ChatService::class.java)
                intent.action = ACTION_NETWORK_CHANGE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }catch (e: Exception) {
            }
        }

        fun createHeartbeatIntent(context: Context): Intent {
            var intent = Intent(context, ChatService::class.java)
            intent.action = ACTION_HEARTBEAT
            return intent
        }

        fun createCheckConnectionIntent(context: Context): Intent {
            var intent = Intent(context, ChatService::class.java)
            intent.action = ACTION_CHECK_CONNECT
            return intent
        }

        fun startHeartbeat(context: Context) {
            try {
                var intent = Intent(context, ChatService::class.java)
                intent.action = ACTION_HEARTBEAT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }

        fun stopHeartbeat(context: Context) {
            try {
                var intent = Intent(context, ChatService::class.java)
                intent.action = ACTION_STOP_HEARTBEAT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }

        fun startReconnect(context: Context) {
            try {
                var intent = Intent(context, ChatService::class.java)
                intent.action = ACTION_CHECK_CONNECT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }

        fun stopReconnect(context: Context) {
            try {
                var intent = Intent(context, ChatService::class.java)
                intent.action = ACTION_STOP_CHECK_CONNECT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }
    }

    var heartbeatPendingIntent: PendingIntent? = null
    var checkConnectionPendingIntent: PendingIntent? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    val CHANNEL_ID_STRING = "ChatService_01"
    private var myHandler = Handler()
    override fun onCreate() {
        super.onCreate()
    }

    private fun showNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var channel = NotificationChannel(
                    CHANNEL_ID_STRING,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
                var notification = Notification.Builder(this@ChatService, CHANNEL_ID_STRING).build()
                startForeground(110, notification)
                myHandler.postDelayed(Runnable { stopForeground(true) }, 5000)
            }
        } catch (e: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        Log.d("rec action :${intent?.action}")
        intent?.action.let {
            when (it) {
                ACTION_HEARTBEAT -> sendHeartbeat()
                ACTION_CHECK_CONNECT -> checkConnection()
                ACTION_NETWORK_CHANGE -> onNetWorkChange()
                ACTION_STOP_CHECK_CONNECT -> stopReconnectTask()
                ACTION_STOP_HEARTBEAT -> stopHeartbeatTask()
                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    val TIME_HEARTBEAT_INTERVAL = 1 * 30 * 1000
    val TIME_RECONNECT_INTERVAL = 5 * 60 * 1000
    private fun triggerHeartbeatTask() {
        heartbeatPendingIntent = PendingIntent.getService(
            this.applicationContext,
            999,
            createHeartbeatIntent(this@ChatService),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        setAlarm(heartbeatPendingIntent!!, TIME_HEARTBEAT_INTERVAL)
    }

    private fun stopHeartbeatTask() {
        heartbeatPendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    private fun triggerReconnectTask() {
        checkConnectionPendingIntent = PendingIntent.getService(
            this.applicationContext,
            998,
            createCheckConnectionIntent(this@ChatService),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        setAlarm(checkConnectionPendingIntent!!, TIME_RECONNECT_INTERVAL)
    }

    private fun stopReconnectTask() {
        checkConnectionPendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun onNetWorkChange() {
        Log.e("onNetWorkChange network is open ${isNetConnection()}")
        try {
            IMClient.onNetworkChange()
        } catch (e: Exception) {
        }
    }

    private fun checkConnection() {
        triggerReconnectTask()
        IMClient.checkReconnect()
    }

    private fun sendHeartbeat() {
        triggerHeartbeatTask()
        IMClient.sendACK(Cmd.ImpHeartbeatReq, HeartbeatReq(TimeUtils.getLocalTime()))
    }

    fun setAlarm(pendingIntent: PendingIntent, delay: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // 此处必须使用SystemClock.elapsedRealtime，否则闹钟无法接收
        var triggerAtMillis = SystemClock.elapsedRealtime() + delay

        // pendingIntent 为发送广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        }
        return
    }

    fun setAlarm(intent: Intent, delay: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Intent local = new Intent(context, KeepAliveReceiver.class);
//        val local = Intent()
//        local.action = action
//        local.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)// 表示包含未启动的App
        val pendingIntent =
            PendingIntent.getBroadcast(this, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // 此处必须使用SystemClock.elapsedRealtime，否则闹钟无法接收
        var triggerAtMillis = SystemClock.elapsedRealtime() + delay

        // pendingIntent 为发送广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        }
        return
    }

    private fun isNetConnection(): Boolean {
        val connectionManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager  //得到系统服务类
        val networkInfo = connectionManager.getActiveNetworkInfo()
        return networkInfo != null && networkInfo!!.isConnected
    }
}
