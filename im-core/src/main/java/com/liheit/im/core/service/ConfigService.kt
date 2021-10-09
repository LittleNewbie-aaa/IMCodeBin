package com.liheit.im.core.service

import com.liheit.im.core.bean.Config
import com.liheit.im.core.dao.DbUtils

/**
 * Created by daixun on 2018/10/27.
 */

object ConfigService {
    private val dao get() = DbUtils.configDB.configDao()

    fun findByKeyAndAccount(name: String, account: String): Config? {
        return dao.findByKeyAndAccount(name, account)
    }

    fun save(config: Config) {
        return dao.save(config)
    }
}
