package com.liheit.im.common.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.liheit.im.core.IMClient
import com.liheit.im.core.protocol.FileBody
import com.liheit.im.core.protocol.ImgBody
import com.liheit.im.core.protocol.MsgBody

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by daixun on 2018/7/3.
 */

class ImImageModelLoader : ModelLoader<ImImageModelLoader.ImImage, InputStream> {

    override fun buildLoadData(imImage: ImImage, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(imImage), IMImageDataFetcher(imImage))
    }

    override fun handles(imImage: ImImage): Boolean {
        return true
    }

    class IMImageDataFetcher(private val image: ImImage) : DataFetcher<InputStream> {

        private var ins: InputStream? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) =
                try {
                    //如果图片为gif类型的不加载缩略图
                    var path = if ((image.msg as ImgBody).name.endsWith(".gif")) {
                        IMClient.resourceManager.getMsgImage((image.msg as MsgBody).mtype, image.msg!!, false).blockingFirst()
                    } else {
                        IMClient.resourceManager.getMsgImage((image.msg as MsgBody).mtype, image.msg!!, image.thumbnail).blockingFirst()
                    }
                    ins = FileInputStream(path)
                    callback.onDataReady(ins)
                } catch (e: Exception) {
                    callback.onLoadFailed(e)
                }

        override fun cleanup() {
            if (ins != null) {
                try {
                    ins!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

        override fun cancel() {

        }

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }
    }

    class ImImageModelLoaderFactory : ModelLoaderFactory<ImImage, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ImImage, InputStream> {
            return ImImageModelLoader()
        }

        override fun teardown() {

        }
    }

    class ImImage {
        var thumbnail = false
        var msg: FileBody? = null
    }
}


