package com.liheit.im.core.service

import com.liheit.im.core.Constants
import com.liheit.im.core.bean.Department
import com.liheit.im.core.bean.EditAction
import com.liheit.im.core.bean.UserDepartment
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.Log
import com.liheit.im.utils.forEachBlock
import kotlin.system.measureTimeMillis

/**
 * 权限数据库操作管理类
 */

object DepartmentService {

    private val msgDao get() = DbUtils.currentDB.msgDao()
    private val depDao get() = DbUtils.currentDB.depDao()
    private val uDepDao get() = DbUtils.currentDB.uDepDao()

    fun saveBatch(udep: List<UserDepartment>) {
        msgDao.runOnTransaction {
            udep.forEach { dep ->
                uDepDao.insert(dep)
            }
        }

       var finddep = uDepDao.findByPid(1001);
        if (finddep.isNotEmpty()){
            Log.v("f");
        }
    }

    fun deleteByType(type: EditAction) {
        depDao.deleteByType(type.action)
    }

    fun deleteUserDepartmentByType(type: EditAction) {
        uDepDao.deleteByType(type.action)
    }

    fun deleteAll() {
        depDao.deleteAll()
    }

    fun deleteAllUserDepartment() {
        uDepDao.deleteAll()
    }

    fun saveDepartmentBatch(udep: List<Department>) {
        if (udep.isNotEmpty()) {
            var time=measureTimeMillis {depDao.saveBatch(udep)  }
            Log.v("saveDepartmentBatch $time count ${udep.size}")
        }
    }

    fun findByPid(pid: Long): MutableList<Department> {
        return depDao.findByPid(pid)
    }

    fun findById(id: Long): Department? {
        return depDao.findById(id)
    }

    fun findByIds(ids: List<Long>): MutableList<Department> {
        var deps = mutableListOf<Department>()
        ids.toLongArray().forEachBlock(Constants.MAX_SQL_PARAM_SIZE) {
            deps.addAll(depDao.findByIds(it))
        }
        return deps
    }

    fun updateVisible(ids:List<Long>,isVisible:Boolean){
        ids.toLongArray().forEachBlock(Constants.MAX_SQL_PARAM_SIZE) {
            depDao.updateVisible(it, isVisible)
        }
    }

    fun updateAllVisible(isVisible: Boolean) {
        depDao.updateAllVisible(isVisible)
    }

    fun getUserDepartment(userId: Long): List<Department> {
        return depDao.getUserDepartment(userId)
    }

    fun search(keyword: String, pageSize: Int, offset: Long): MutableList<Department> {
        return depDao.search(keyword, pageSize, offset)
    }

}
