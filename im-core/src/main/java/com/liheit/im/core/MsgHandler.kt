package com.liheit.im.core

import android.support.annotation.Keep

/**
 * Created by daixun on 2018/6/22.
 */
@Keep
interface MsgHandler : MsgCallback {

    fun getHandlerType():List<Int>
}
