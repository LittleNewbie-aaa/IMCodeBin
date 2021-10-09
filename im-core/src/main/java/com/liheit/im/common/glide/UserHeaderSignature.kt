package com.liheit.im.common.glide

import com.bumptech.glide.load.Key
import java.security.MessageDigest

/**
 * Created by daixun on 2018/8/4.
 */

class UserHeaderSignature(private val user: AccountInfo) : Key {

    override fun equals(o: Any?): Boolean {
        if (o is AccountInfo) {
            val other = o as AccountInfo?
            return user.logo == other!!.logo && user.account == other.account && user.thumb == other.thumb
        }
        return false
    }

    override fun hashCode(): Int {
        return user.logo.toInt()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((user.id.toString() + "-user-header-${user.thumb}-" + user.logo).toByteArray(Key.CHARSET))
    }
}
