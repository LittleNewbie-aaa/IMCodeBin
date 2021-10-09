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
import com.liheit.im.core.protocol.MsgBody
import com.liheit.im.utils.IDUtil

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by daixun on 2018/7/3.
 */

class SessionIconModelLoader : ModelLoader<SessionIconModelLoader.SessionImage, InputStream> {


    override fun buildLoadData(imImage: SessionImage, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(imImage), SessionImageDataFetcher(imImage))
    }

    override fun handles(imImage: SessionImage): Boolean {
        return true
    }


    class SessionImageDataFetcher(private val session: SessionImage) : DataFetcher<InputStream> {

        private var ins: InputStream? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            try {
                val conversation = IMClient.chatManager.getConversation(session.sid)!!
                when (conversation?.type) {
                    com.liheit.im.core.bean.SessionType.SESSION_P2P.value -> {
                        val targetId = IDUtil.parseTargetId(IMClient.getCurrentUserId(), conversation.sid)
                        val account = IMClient.userManager.getUserById(targetId)?.account
                        var filePath = IMClient.resourceManager.getUserHeaderImg(account!!).blockingFirst()
                        ins = FileInputStream(filePath)
                        callback.onDataReady(ins)
                    }
                    else -> {

                    }
                }
                //IMClient.resourceManager.getUserHeaderImg(account.account.toString()).blockingFirst()

                val path = IMClient.resourceManager.getMsgImage((session.msg as MsgBody).mtype, session.msg!!, session.thumbnail).blockingFirst()
                ins = FileInputStream(path)
                callback.onDataReady(ins)
            } catch (e: Exception) {
                callback.onLoadFailed(e)
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

    class SessionImageModelLoaderFactory : ModelLoaderFactory<SessionImage, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<SessionImage, InputStream> {
            return SessionIconModelLoader()
        }

        override fun teardown() {

        }
    }

    class SessionImage {
        var sid:String=""
        var thumbnail = false
        var msg: FileBody? = null
    }
}


