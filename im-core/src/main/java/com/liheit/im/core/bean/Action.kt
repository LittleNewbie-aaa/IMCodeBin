package com.pkurg.lib.model.bean

import android.widget.ImageView

/**
 * 用于rxbus进行点击事件传递的类
 */
data class  ChoiseUserClick(
    var type:Int
)
/**
用于使用rxbus通知进行下载图片
 */
data class NoPic(
    var uid:Long,
    var image:ImageView,
    var showRoundedCorners:Boolean
)

data class NoPic2(
    var uid:Long,
    var showRoundedCorners:Boolean
)
/**
 * 更新常用群组数字
 */
data class Flush(
    var num:Int
)
/**
 * 关闭聊天界面
 * */
data class CloseChat(
    var id:Int
)
/**
 *传递viewpager 图片的状态
 */
data class PicState(
    var position:Int,
    var state:Int// 0 表示未开始， 1，进行中，2，结束
)