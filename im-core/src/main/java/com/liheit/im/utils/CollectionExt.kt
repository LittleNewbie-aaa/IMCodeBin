package com.liheit.im.utils

/**
 * Created by daixun on 2018/11/7.
 */

fun LongArray.forEachBlock(blockSize: Int, action: (buffer: LongArray) -> Unit): Unit {
    if (this.size <= blockSize) {
        action.invoke(this)
        return
    }
    var start = 0
    var idArray = LongArray(blockSize)
    do {
        if (start + blockSize <= this.size) {
            System.arraycopy(this, start, idArray, 0, idArray.size)
            action.invoke(idArray)
        } else {
            idArray = LongArray(this.size - start)
            System.arraycopy(this, start, idArray, 0, idArray.size)
            action.invoke(idArray)
        }
        start += blockSize
        if (start >= this.size) {
            break
        }
    } while (true)
}