package com.dagger.baselib.base

import me.drakeet.multitype.MultiTypeAdapter

/**
 * Created by daixun on 2018/3/17.
 */

open class BaseAdapter : MultiTypeAdapter() {
    var mData = mutableListOf<Any>()

    init {
        items = mData
    }

}
