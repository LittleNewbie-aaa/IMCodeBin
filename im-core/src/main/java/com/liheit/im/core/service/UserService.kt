package com.liheit.im.core.service

import com.liheit.im.core.Constants
import com.liheit.im.core.bean.EditAction
import com.liheit.im.core.bean.User
import com.liheit.im.core.dao.DbUtils
import com.liheit.im.utils.Log
import com.liheit.im.utils.forEachBlock
import com.liheit.im.utils.json.gson
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Created by daixun on 2018/10/27.
 */

object UserService {

    private val userDao get() = DbUtils.currentDB.userDao()
    private val uDepDao get() = DbUtils.currentDB.uDepDao()
    private val uMsgDao get() = DbUtils.currentDB.msgDao()

    fun saveBatch(users: List<User>) {
        var time = measureNanoTime {
            users.forEach { it.buildSearchInfo() }
        }
        Log.v("process searchInfo:${time} nanoTime,count:${users.size}")
        userDao.saveAll(users)

       var userid =  userDao.findUserByDepartmentId(1001);
        Log.v("userDao.saveAll : ${users.size}")
    }

    fun findByDepId(pid: Long): MutableList<User> {
        return userDao.findUserByDepartmentId(pid)
    }

    fun findById(i: Long): User? {
        return userDao.findById(i)
    }
//    fun findById(i: Long): User? {
//        return userDao.findById(i)
//    }
    fun findByIds(ids: List<Long>): MutableList<User> {
        val users = mutableListOf<User>()
        ids.toLongArray().forEachBlock(Constants.MAX_SQL_PARAM_SIZE) {
            users.addAll(userDao.findByIds(it.toMutableList()))
        }
        return users

        /*if (ids.size < Constants.MAX_SQL_PARAM_SIZE) {
            return userDao.findByIds(ids)
        } else {
            val users = mutableListOf<User>()
            var start = 0
            var sourceIds = ids.toLongArray()
            var idArray = longArrayOf(Constants.MAX_SQL_PARAM_SIZE.toLong())
            do {
                if (start + Constants.MAX_SQL_PARAM_SIZE <= ids.size) {
                    System.arraycopy(sourceIds, start, idArray, 0, idArray.size)
                    users.addAll(userDao.findByIds(idArray.toList()))
                } else {
                    idArray = LongArray(ids.size - start)
                    System.arraycopy(sourceIds, start, idArray, 0, idArray.size)
                    users.addAll(userDao.findByIds(idArray.toList()))
                    break
                }
                start += Constants.MAX_SQL_PARAM_SIZE
            } while (true)

            return users
        }*/
    }

    fun deleteByType(type: EditAction) {
        userDao.deleteByType(type.action)
    }

    fun findByNameLike(keyword: String): MutableList<User> {
        return userDao.findUserLikeName(keyword)
    }

    fun searchUser(keyword: String, pageSize: Int, offset: Long,permission: IntArray): MutableList<User> {
        val users = userDao.searchUser(keyword, pageSize, offset,permission)
        Log.e("aaa users=${gson.toJson(users)}")
        Log.e("aaa users2=${users.filter { DepartmentService.getUserDepartment(it.id).isNotEmpty() }.toMutableList()}")
        return users.filter { DepartmentService.getUserDepartment(it.id).isNotEmpty() }.toMutableList()
    }

    fun findBySessionId(sid: String, pageSize: Int, offset: Long): List<User> {
        return userDao.findBySession(sid, pageSize, offset)
    }

    fun updateVisible(depIds: LongArray, visible: Boolean) {
        val time = measureTimeMillis {
            uMsgDao.runOnTransaction {
                depIds.forEachBlock(Constants.MAX_SQL_PARAM_SIZE) {
                    userDao.updateUserVisible(it, visible)
                }
            }
        }
        Log.e("updateVisible:${time}")
    }

    fun updateAllVisible(visible: Boolean): Long {
        return userDao.updateAllUserVisible(visible)
    }

    fun findByPhone(phone: String): List<User> {
        return userDao.findByPhone("%${phone}%")
    }

    fun updateLogo(uid: Long, logo: Long): Long {
        return userDao.updateLogo(logo, uid)
    }

    fun findUserPinyinIsNoll(): List<User> {
        return userDao.findUserPinyinIsNoll()
    }

    fun updateUserNamePinyin(uid: Long , pinyin:String): Long {
        return userDao.updateUserNamePinyin(uid,pinyin)
    }
}
