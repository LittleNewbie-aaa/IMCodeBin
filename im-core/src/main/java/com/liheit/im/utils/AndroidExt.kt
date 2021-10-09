package com.liheit.im.utils

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import com.liheit.im.core.IMClient

/**
 * Created by daixun on 2018/9/24.
 */

fun ConnectivityManager.isNetConnection(): Boolean {
    val networkInfo = activeNetworkInfo
    return networkInfo != null && networkInfo!!.isConnected
}

val Context.connectivityManager: ConnectivityManager
    get():ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

val Context.versionName: String
    get() {
        try {
            val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
            return packageInfo.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }