package com.liheit.im.core.service

import com.liheit.im.core.bean.CollectMsg
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.*
import com.liheit.im.utils.json.gson

/**
 * 收藏消息管理器
 */
object CollectService {

    //数据库最后更新时间
    var lastCollectUpdateTime: Long by DBConfigDelegates<Long>("lastCollectUpdateTime", 0)

    private val collectdao get() = DbUtils.currentDB.collectDao()

    //插入收藏的消息
    fun insert(msg: CollectMsg) {
        collectdao.insert(msg)
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }

    //更新收藏的消息
    fun update(msg: CollectMsg) {
        Log.Companion.e("aaa update=${gson.toJson(msg)}")
        collectdao.update(msg)
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }

    //获取收藏消息列表
    fun findCollectMessage(startTime: Long, pageSize: Int): MutableList<CollectMsg> {
        var msgs = collectdao.findCollectDataPage(startTime, pageSize)
        return msgs
    }

    //根据id获取收藏消息
    fun findById(id: Long): CollectMsg? {
        var msgs = collectdao.getCollectDataById(id)
        return msgs
    }


    //删除收藏的消息
    fun delete(msg: CollectMsg) {
        collectdao.delete(msg)
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }

    //删除收藏的消息
    fun deleteById(id: Long) {
        collectdao.deleteById(id)
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }

    //删除全部收藏的消息
    fun deleteAll() {
        collectdao.deleteAll()
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }

    //获取分类收藏消息列表
    fun findCollectTypeDataPage(startTime: Long, type: Int, pageSize: Int): MutableList<CollectMsg> {
        var msgs = collectdao.findCollectTypeDataPage(startTime, type, pageSize)
        return msgs
    }

    //搜索
    fun searchCollectDataPage(startTime: Long, keyword: String, pageSize: Int): MutableList<CollectMsg> {
        var msgs = collectdao.searchCollectDataPage(startTime, keyword, pageSize)
        return msgs
    }

    //保存或更新收藏消息
    fun save(msg: CollectMsg) {
        val cmsg = findById(msg.id)
        if (cmsg == null) {
            insert(msg)
        } else {
            update(msg)
        }
        lastCollectUpdateTime = TimeUtils.getServerTime()
    }
}
