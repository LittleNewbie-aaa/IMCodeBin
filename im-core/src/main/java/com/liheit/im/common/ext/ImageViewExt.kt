package com.liheit.im.common.ext

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dagger.baselib.utils.RxBus
import com.liheit.im.common.glide.*
import com.liheit.im.core.Constants
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.bean.CollectMsg
import com.liheit.im.core.bean.Contact
import com.liheit.im.core.bean.User
import com.liheit.im.core.link.LinkUtil
import com.liheit.im.core.protocol.ImgBody
import com.liheit.im.core.protocol.MsgBody
import com.liheit.im.core.service.CollectService
import com.liheit.im.core.service.MessageService
import com.liheit.im_core.R
import com.pkurg.lib.model.bean.NoPic
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

/**
 * Created by daixun on 2018/3/18.
 */
fun ImageView.setUrl(url: String?) {
    GlideApp.with(this@setUrl.context)
            .load(url)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(this@setUrl)
}

fun ImageView.setUrlEx(url: String?, radius: Int = 0) {
    var roundedCorners = RoundedCorners(radius)
    var options = RequestOptions.bitmapTransform(roundedCorners)
    GlideApp.with(this.context)
            .load(Uri.fromFile(File(url)))
            .placeholder(R.color.c8cad7)
            .centerCrop()
            .apply(options)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(this)
}

fun ImageView.setUrl(url: String?, placeholder: Int) {
    GlideApp.with(this@setUrl.context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(placeholder)
            .error(placeholder)
            .into(this@setUrl)
}

fun ImageView.setUserHeader(user: User?, showRoundedCorners: Boolean = false) {
    try {
        GlideApp.with(this).clear(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (user == null) return

    var roundedCorners = RoundedCorners(5)
    var options = RequestOptions.bitmapTransform(roundedCorners)
    val accountInfo = user.toAccountInfo()
    GlideApp.with(this.context)
            .load(accountInfo).apply {
                if (showRoundedCorners) {
//                apply(options)
                }
            }
            .signature(UserHeaderSignature(accountInfo))
            .placeholder(R.color.e5e5e5)
            .into(this)
}

/**
 * 通过从华远数据库获取的个人信息，尤其是头像信息来加载头像
 * contact 从数据库获取的图片
 * user
 */
fun ImageView.setUserHeaderWithConcat(user: User?, showRoundedCorners: Boolean = false, contact: Contact, uid: Long) {
    if (user == null && uid == Constants.FILE_HELP_ID) {
        RxBus.getInstance().send(NoPic(Constants.FILE_HELP_ID, this, showRoundedCorners))
    }
    if (user == null) return
    val imageLocation = contact?.avatar?.trim()
    val gender = contact?.gender
    var drawable = if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
        R.drawable.hy_default_head
    } else {
        when (gender) {
            2 -> R.drawable.default_head_w
            else -> R.drawable.default_head_m
        }
    }
    if (!TextUtils.isEmpty(imageLocation)) {
        //获取到本地文件然后进行加载
        val path =
                Environment.getExternalStorageDirectory().absolutePath + "/Android/data/" + context.packageName + "/cache/OaCache/" + contact.avatar
        val file = File(path)
        if (!file.exists()) {
            //如果没有本地的图片
            RxBus.getInstance().send(NoPic(user.id, this, showRoundedCorners))
        } else {
            try {
                GlideApp.with(this.context)
                        .load(file)
                        .placeholder(drawable)
                        .into(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } else {
        GlideApp.with(this.context)
                .load(drawable)
                .placeholder(drawable)
                .into(this)
    }
}

/**
 * 设置展示圆形的头像，通过调用华远的个人人信息，获取到头像位置
 */
fun ImageView.setUserCircleHeaderWithConcat(user: User?, showRoundedCorners: Boolean = false, contact: Contact) {
    if (user == null) return
    val imageLocation = contact?.avatar?.trim()
    val gender = contact?.gender
    var drawable = if (getAppName() == AppNameFlag.THE_HY_FLAG.value) {
        R.drawable.hy_default_head
    } else {
        when (gender) {
            2 -> R.drawable.default_head_w
            else -> R.drawable.default_head_m
        }
    }
    if (!TextUtils.isEmpty(imageLocation)) {
        //获取到本地文件然后进行加载
        val path = Environment.getExternalStorageDirectory().absolutePath + "/Android/data/" + context.packageName + "/cache/OaCache/" + contact.avatar
        val file = File(path)
        if (!file.exists()) {
            //如果没有本地的图片
            RxBus.getInstance().send(NoPic(user.id, this, showRoundedCorners))
        } else {
            try {
//                GlideApp.with(this).clear(this)
                var roundedCorners = RoundedCorners(dip2px(12F))
                var options = RequestOptions.bitmapTransform(roundedCorners)
                GlideApp.with(this.context)
                        .load(file)
//                .apply(options)
                        .placeholder(drawable)
                        .into(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } else {
//        GlideApp.with(this).clear(this)
        var roundedCorners = RoundedCorners(dip2px(12F))
        var options = RequestOptions.bitmapTransform(roundedCorners)
        GlideApp.with(this.context)
                .load(drawable)
//            .apply(options)
                .placeholder(drawable)
                .into(this)
    }
}

fun ImageView.setCircleUserHeader(user: User?) {
    if (user == null) return
//    var roundedCorners = CenterCrop()
    var roundedCorners = RoundedCorners(1)
    var options = RequestOptions.bitmapTransform(roundedCorners)
    val accountInfo = user.toAccountInfo()
    GlideApp.with(this.context)
            .load(accountInfo)
            .apply(options)
            .signature(UserHeaderSignature(accountInfo))
            .into(this)
}

//励信项目设置图像(方形)
fun ImageView.setUserDefaultHeader(userId: Long) {
    var user = IMClient.userManager.getUserById(userId)
    if (user == null) return
//    var roundedCorners = CircleCrop()
    var roundedCorners = CenterCrop()
//    var roundedCorners = RoundedCorners(1)
    var options = RequestOptions.bitmapTransform(roundedCorners)
    val accountInfo = user.toAccountInfo()
    GlideApp.with(this.context)
            .load(accountInfo)
            .apply(options)//临时注释
            .signature(UserHeaderSignature(accountInfo))
            .into(this)
}

//励信项目设置图像(圆形)
fun ImageView.setUserRoundHeader(userId: Long) {
    var user = IMClient.userManager.getUserById(userId)
    if (user == null) return
    var roundedCorners = CircleCrop()
    var options = RequestOptions.bitmapTransform(roundedCorners)
    val accountInfo = user.toAccountInfo()
    GlideApp.with(this.context)
            .load(accountInfo)
            .apply(options)//临时注释
            .signature(UserHeaderSignature(accountInfo))
            .into(this)
}

/**
 * 聊天设置的用户头像，这里不是圆角修改为长方形
 */
fun ImageView.setCircleUserHeader(userId: Long) {
    when (getAppName()) {
        AppNameFlag.THE_LX_FLAG.value,
        AppNameFlag.THE_XY_FLAG.value -> {
            setUserRoundHeader(userId)
        }
        AppNameFlag.THE_SD_FLAG.value,
        AppNameFlag.THE_HY_FLAG.value -> {
            setCircleUserHeader(userId, false, this.context)
        }
    }
}

/**
 *添加context 调用华远（实地）用户信息的cicle 头像方法
 */
fun ImageView.setCircleUserHeader(userId: Long, roundedCorners: Boolean = false, context: Context) {
    var u = IMClient.userManager.getUserById(userId)
    val concat = LinkUtil.test(context, userId)
    this.setUserCircleHeaderWithConcat(u, roundedCorners, concat)
}

fun ImageView.setUserHeader(userId: Long, roundedCorners: Boolean = false) {
    var u = IMClient.userManager.getUserById(userId)
    this.setUserHeader(u, roundedCorners)
}

/**
 * 添加通过加入concat 获取到头像的展示的方法
 */
fun ImageView.setUserHeader(userId: Long, roundedCorners: Boolean = false, context: Context) {
    var u = IMClient.userManager.getUserById(userId)
    val concat = LinkUtil.test(context, userId)
    this.setUserHeaderWithConcat(u, roundedCorners, concat, userId)
}

fun ImageView.setImImage(msg: ChatMessage, thumbnail: Boolean = false, radius: Int = dip2px(5f)) {
    val imageBody = msg.getMessageBody() as? ImgBody
    imageBody?.let {
        if (it.isDownloaded() && it.bytes == File(it.localPath).length()) {
            this.setUrlEx((msg.getMessageBody() as? ImgBody)?.localPath, radius)
        } else {
            msg.getMessageBody()?.let { body ->
                var roundedCorners = RoundedCorners(radius)
                var options = RequestOptions.bitmapTransform(roundedCorners)
                val imImage = ImImageModelLoader.ImImage().apply {
                    this.thumbnail = thumbnail
                    this.msg = body as ImgBody
                }
                //通过子线程将图片下载下来，然后设置图片的localPath
                Observable.create(ObservableOnSubscribe<String> {
                    it.onNext(
                            IMClient.resourceManager.getMsgImage(
                                    (imImage.msg as MsgBody).mtype,
                                    imImage.msg!!, thumbnail
                            ).blockingFirst()
                    )
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            (msg.getMessageBody() as? ImgBody)?.localPath = it
                            msg.flag = msg.flag or ChatMessage.FLAG_READ
                            MessageService.update(msg)
                        }
                GlideApp.with(this.context)
                        .load(imImage)
                        .placeholder(R.color.c8cad7)
//                        .centerCrop()
//                        .apply(options)
                        .signature(ImImagesSignature(imImage))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(this)
            }
        }
    }
}

fun ImageView.setCollectionImage(msg: CollectMsg, thumbnail: Boolean = true, radius: Int = dip2px(5f)) {
    val imageBody = msg.msgs!![0] as? ImgBody
    imageBody?.let {
        if (it.isDownloaded() && it.bytes == File(it.localPath).length()) {
            this.setUrlEx(imageBody.localPath, radius)
        } else {
            imageBody.let { body ->
                var roundedCorners = RoundedCorners(radius)
                var options = RequestOptions.bitmapTransform(roundedCorners)
                val imImage = ImImageModelLoader.ImImage().apply {
                    this.thumbnail = thumbnail
                    this.msg = body as ImgBody
                }
                //通过子线程将图片下载下来，然后设置图片的localPath
                Observable.create(ObservableOnSubscribe<String> {
                    it.onNext(
                            IMClient.resourceManager.getMsgCollectionImage(
                                    (imImage.msg as MsgBody).mtype,
                                    imImage.msg!!, false
                            ).blockingFirst()
                    )
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            imageBody?.localPath = it
                            msg.msgs!![0] = imageBody
                            CollectService.save(msg)
                        }
                GlideApp.with(this.context)
                        .load(imImage)
//                        .load(Uri.fromFile(File(it)))
                        .placeholder(R.color.c8cad7)
//                        .centerCrop()
//                        .apply(options)
                        .signature(ImImagesSignature(imImage))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(this)
            }
        }
    }
}

fun ImageView.setBodyImage(imageBody: ImgBody, thumbnail: Boolean = false, radius: Int = dip2px(5f)) {
    imageBody?.let {
        if (it.isDownloaded() && it.bytes == File(it.localPath).length()) {
            this.setUrlEx(imageBody.localPath, radius)
        } else {
            imageBody.let { body ->
                val imImage = ImImageModelLoader.ImImage().apply {
                    this.thumbnail = thumbnail
                    this.msg = body as ImgBody
                }
                //通过子线程将图片下载下来，然后设置图片的localPath
                Observable.create(ObservableOnSubscribe<String> {
                    it.onNext(
                            IMClient.resourceManager.getMsgCollectionImage(
                                    (imImage.msg as MsgBody).mtype,
                                    imImage.msg!!, false
                            ).blockingFirst()
                    )
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                        }
                GlideApp.with(this.context)
                        .load(imImage)
                        .placeholder(R.color.c8cad7)
                        .signature(ImImagesSignature(imImage))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(this)
            }
        }
    }
}

fun User.toAccountInfo(): AccountInfo {
    return AccountInfo(
            account = this.account,
            id = this.id,
            logo = this.logo,
            name = this.name,
            thumb = true
    )
}