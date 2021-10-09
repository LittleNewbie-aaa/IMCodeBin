package com.liheit.im.common.glide

import com.bumptech.glide.load.Key
import com.liheit.im.common.glide.ImImageModelLoader.ImImage

import java.security.MessageDigest

/**
 * Created by daixun on 2018/8/4.
 */

class ImImagesSignature(private val user: ImImage) : Key {

    override fun equals(o: Any?): Boolean {
        if (o is ImImage) {
            val other = o as ImImage?
            return user.msg!!.md5 === other!!.msg!!.md5 && user.thumbnail == other!!.thumbnail
        }
        return false
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((user.msg!!.md5 + "" + user.thumbnail).toByteArray(Key.CHARSET))
    }
}
