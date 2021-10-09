package com.liheit.im.utils

import com.liheit.im.core.IMClient.getAndUpdateKt
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by daixun on 2018/7/15.
 */

object SnUtil {
    private var packageIndex = AtomicInteger(1)
    public fun genIndex(): Int {
        return packageIndex.getAndUpdateKt { return@getAndUpdateKt if (it > 65535) 0 else it + 1 }
    }

    /*fun reset() {
        packageIndex.set(1)
    }*/
}
