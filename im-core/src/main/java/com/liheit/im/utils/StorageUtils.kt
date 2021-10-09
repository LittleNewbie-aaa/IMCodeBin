package com.liheit.im.utils

import android.content.Context
import android.os.Environment
import android.support.v4.content.ContextCompat
import com.liheit.im.core.IMClient

import java.io.File
import java.util.*

/**
 * Created by daixun on 2018/7/25.
 */

object StorageUtils {

    fun getDiskCacheDir(context: Context): File? {
        var cachePath: File? = null
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            cachePath = ContextCompat.getExternalCacheDirs(context)[0]
        } else {
            cachePath = context.cacheDir
        }
        //        File(IMClient.context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "${IMClient.getCurrentUserId()}")
        return cachePath
    }

    fun getVideoCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "videos/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }


    fun getVoiceCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "voices/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getImageCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "images/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getImageHeadsCacheFile(filename: String): File {
        var file = File(getUserCacheDir(), "heads/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getImageCacheDir(): File {
        var file = File(getUserCacheDir(), "images")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    fun getFileCacheFile(fileToken: String,filename: String): File {
        var file = File(getUserCacheDir(), "files/${fileToken}/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getHtmlCacheFile(graphicId: String): File {
        var file = File(getUserCacheDir(), "html/${graphicId}.html")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getApkCacheFile(filename: String): File {
        var file = File(IMClient.context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "apk/${filename}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun getUserCacheDir(): File {
        return File(IMClient.context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "${IMClient.getCurrentUserId()}")
    }

    public fun getRandomTempFile(): File {
        return File(IMClient.context.cacheDir, UUID.randomUUID().toString())
    }

}
