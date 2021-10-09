package com.liheit.im.core.service

import com.liheit.im.core.bean.Subscription
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.*

/**
 * 公众号数据管理器
 */
object SubscriptionService {

    //数据库最后更新时间
    var lastSubscriptionUpdateTime: Long by DBConfigDelegates<Long>("lastSubscriptionUpdateTime", 0)

    private val subscriptionDao get() = DbUtils.currentDB.subscriptionDao()

    //插入公众号
    fun insert(s: Subscription) {
        subscriptionDao.insert(s)
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }

    //更新公众号
    fun update(s: Subscription) {
        subscriptionDao.update(s)
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }

    //根据id获取公众号
    fun findBySid(sid: String): Subscription? {
        return subscriptionDao.getSubscriptionBySid(sid)
    }

    //获取公众号列表
    fun findCollectMessage(startTime: Long, pageSize: Int): MutableList<Subscription> {
        return subscriptionDao.findSubscriptionDataPage(startTime, pageSize)
    }

    //删除公众号
    fun delete(s: Subscription) {
        subscriptionDao.delete(s)
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }

    //删除公众号
    fun deleteBySid(sid: String) {
        subscriptionDao.deleteBySid(sid)
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }

    //删除全部公众号
    fun deleteAll() {
        subscriptionDao.deleteAll()
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }

    //搜索
    fun searchCollectDataPage(startTime: Long, keyword: String, pageSize: Int): MutableList<Subscription> {
        return subscriptionDao.searchSubscriptionDataPage(startTime, keyword, pageSize)
    }

    //保存或更新公众号
    fun save(s: Subscription) {
        val cmsg = subscriptionDao.getSubscriptionById(s.id)
        if (cmsg == null) {
            insert(s)
        } else {
            update(s)
        }
        lastSubscriptionUpdateTime = TimeUtils.getServerTime()
    }
}
