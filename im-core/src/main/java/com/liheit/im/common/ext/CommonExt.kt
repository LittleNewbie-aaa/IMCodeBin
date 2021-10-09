package com.liheit.im.common.ext

import android.content.Context
import android.content.DialogInterface
import android.media.Ringtone
import android.media.RingtoneManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.widget.SimpleAdapter
import com.liheit.im_core.BuildConfig
import com.liheit.im_core.R
import com.qmuiteam.qmui.widget.dialog.QMUIDialog
import com.qmuiteam.qmui.widget.popup.QMUIListPopup
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by daixun on 2018/3/18.
 */
fun dp_f(dp: Float): Float {
    // 引用View的context
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Ext.ctx.resources.displayMetrics
    )
}

// 转换Int
fun dp_i(dp: Float): Int {
    return dp_f(dp).toInt()
}

/**
 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
 */
fun dip2px(dpValue: Float): Int {
    val scale = Ext.ctx.resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

/**
 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
 */
fun px2dip(pxValue: Float): Int {
    val scale = Ext.ctx.resources.displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}

fun sp2px(spValue: Float): Int {
    val fontScale = Ext.ctx.resources.displayMetrics.scaledDensity
    return (spValue * fontScale + 0.5f).toInt()
}


fun getStringEx(res: Int): String {
    return Ext.ctx.resources.getString(res)
}

fun getStringEx(res: Int, vararg formatArg: Any): String {
    //kotlin传递可变参数给java时需要使用*来传递参数
    return Ext.ctx.resources.getString(res, *formatArg)
}

fun getColorEx(res: Int): Int {
    return Ext.ctx.resources.getColor(res)
}

fun RecyclerView.ViewHolder.getDimensionPixelOffset(res: Int): Int {
    return Ext.ctx.resources.getDimensionPixelOffset(res)
}

fun getDimensionPixelOffset(res: Int): Int {
    return Ext.ctx.resources.getDimensionPixelOffset(res)
}

var time = 0L
fun View.setOnClickListenerEx(listener: (v: View) -> Unit) {
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (now - time > 300) {
            time = now
            listener.invoke(this)
        }
    }
}

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
}

fun String.removeLastNewLine(): String {
    if (this.endsWith("\n")) {
        return this.dropLast(1)
    }
    return this
}

var format2Line = SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss")
var format = SimpleDateFormat("yyyy-MM-dd")
fun Date?.format2LineString(): String {
    return format2Line.format(this)
}

fun Date?.format2String(): String {
    return format.format(this)
}

fun Int.formatToAudioTime(): String {
    var standardTime = "00:00"
    var seconds = this
    if (seconds <= 0) {
        standardTime = "00:00";
    } else if (seconds < 60) {
        standardTime = String.format(Locale.getDefault(), "00:%02d", seconds % 60);
    } else if (seconds < 3600) {
        standardTime = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    } else {
        standardTime = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                seconds / 3600,
                seconds % 3600 / 60,
                seconds % 60
        );
    }
    return standardTime
}

fun <T> List<T>.takeOrNull(index: Int): T? {
    if (this.isEmpty()) return null
    if (index < 0 || index >= this.size) return null

    return this[index]
}

val Context.versionName: String
    get() {
        try {
            val packageInfo = getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
            return packageInfo.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

fun Context.showOperationDialog(
        items: Array<String>,
        listener: (DialogInterface, Int) -> Unit
): QMUIDialog {
    return QMUIDialog.MenuDialogBuilder(this)
            .addItems(items, listener)
            .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
}

fun Context.showActionMenu(target: View, listener: (Int) -> Unit) {
    val data = mutableListOf(
            mutableMapOf<String, Any>("icon" to R.drawable.ic_action_group_chat, "name" to "发起群聊"),
            mutableMapOf<String, Any>("icon" to R.drawable.ic_action_qr_scan, "name" to "扫一扫")
    )
    val adapter = SimpleAdapter(this, data, R.layout.item_action_menu, arrayOf("icon", "name"), intArrayOf(R.id.ivIcon, R.id.tvName))
    val mListPopup = QMUIListPopup(this, QMUIPopup.DIRECTION_TOP, adapter)
    mListPopup.create(dp_i(145f), dp_i(200f)) { adapterView, view, i, l ->
        listener.invoke(i)
        mListPopup.dismiss()
    }
    mListPopup.setOnDismissListener { }

    mListPopup.setPopupLeftRightMinMargin(0)
    mListPopup.setPopupTopBottomMinMargin(0)
    mListPopup.setAnimStyle(QMUIPopup.ANIM_GROW_FROM_CENTER)
    mListPopup.setPreferredDirection(QMUIPopup.DIRECTION_TOP)
    mListPopup.show(target)
}

private var ringtone: Ringtone? = null
fun Context.defaultMediaPlayer() {
    try {
        if (ringtone == null) {
            synchronized(this) {
                var notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(this, notification)
            }
        }
        if (ringtone?.isPlaying != true) {
            ringtone?.play()
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

fun String.hidePhone(): String {
    return this.replace("(\\d{3})\\d{4}(\\d{0,10})".toRegex(), "$1****$2")
}

fun getAppName(): Int {
    return BuildConfig.AppNameFlag
}

enum class AppNameFlag(val value: Int) {
    THE_LX_FLAG(0),//励信
    THE_SD_FLAG(1),//实地
    THE_HY_FLAG(2),//华远
    THE_XY_FLAG(3),//小源
}

//IBridge发送消息类型
enum class IBridgeArg(val value: Int){
    SEARCH_USER(1001),//搜索用户
    SEARCH_USER_RETURN(1002),//搜索用户返回
    SEND_REGISTER_PUSH(1003),//发送请求注册推送
    SCROLL_SUBSCRIPTION_MSG(1004)//发送请求注册推送
}

