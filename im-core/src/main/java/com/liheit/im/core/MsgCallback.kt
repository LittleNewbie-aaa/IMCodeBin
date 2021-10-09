package com.liheit.im.core

import android.support.annotation.Keep
import com.liheit.im.core.protocol.PackageType

/**
 * Created by daixun on 2018/6/22.
 */
@Keep
interface MsgCallback {

    /**
     * @param data        消息体
     * @param packageType 消息类型
     * @param sendNumber
     * @param cmd
     */
    fun onMessage(data: String, @PackageType.Type packageType: Int, sendNumber: Int, cmd: Int)
}
