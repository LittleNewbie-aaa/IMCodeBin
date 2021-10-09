package com.liheit.im.common.glide

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.liheit.im.core.IMClient
import com.liheit.im.utils.Log
import com.liheit.im.utils.UserHeadUtil
import java.io.*

/**
 * 头像处理类
 */
class AccountImageModelLoader : ModelLoader<AccountInfo, InputStream> {

    override fun buildLoadData(imImage: AccountInfo, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(imImage), IMImageDataFetcher(imImage))
    }

    override fun handles(account: AccountInfo): Boolean {
        return true
    }

    class IMImageDataFetcher(private val account: AccountInfo) : DataFetcher<InputStream> {

        private var ins: InputStream? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            Log.e("aaa account.logo=${account.logo}")
            if (account.logo == 0L) {
                val bitmap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888)
                val textDrawable = UserHeadUtil.genDefaultHead(account.name, account.id)
                textDrawable.setBounds(0, 0, 180, 180)
                val canvas = Canvas(bitmap)
                textDrawable.draw(canvas)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                callback.onDataReady(ByteArrayInputStream(outputStream.toByteArray()))
            } else {
                try {
                    val path = IMClient.resourceManager.getUserHeaderImg(account.account, account.thumb).blockingFirst()
                    ins = FileInputStream(path)
                    callback.onDataReady(ins)
                } catch (e: Exception) {
                    val bitmap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888)
                    val textDrawable = UserHeadUtil.genDefaultHead(account.name, account.id)
                    textDrawable.setBounds(0, 0, 180, 180)
                    val canvas = Canvas(bitmap)
                    textDrawable.draw(canvas)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    callback.onDataReady(ByteArrayInputStream(outputStream.toByteArray()))
                }
            }
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

    class AccountImageModelLoaderFactory : ModelLoaderFactory<AccountInfo, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AccountInfo, InputStream> {
            return AccountImageModelLoader()
        }

        override fun teardown() {

        }
    }
}

data class AccountInfo(
        var account: String,
        val id: Long,
        val name: String,
        val logo: Long,
        val thumb: Boolean = true
)


