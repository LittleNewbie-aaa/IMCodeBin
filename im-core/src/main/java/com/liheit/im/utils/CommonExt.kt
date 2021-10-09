package com.liheit.im.utils

import java.io.InputStream
import java.io.OutputStream

/**
 * Created by daixun on 2018/11/25.
 */


fun <T> List<T>.takeOrNull(index: Int): T? {
    if (this.isEmpty()) return null
    if (index < 0 || index >= this.size) return null

    return this[index]
}

fun InputStream.copyTo(out: OutputStream, bufferSize: Int = 4096, callback: (bytesCopied: Long) -> Unit): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        callback.invoke(bytesCopied)
        bytes = read(buffer)
    }
    return bytesCopied
}