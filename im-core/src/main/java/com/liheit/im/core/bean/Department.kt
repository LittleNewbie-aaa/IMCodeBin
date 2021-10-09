package com.liheit.im.core.bean

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey


/**
 * 部门表
 */
@Entity
data class Department(
    @PrimaryKey var id: Long = 0,//部门id
    @ColumnInfo var pid: Long = 0,//上级部门地址
    @ColumnInfo var cname: String = "",//
    @ColumnInfo var ename: String = "",
    @ColumnInfo var t: Long = 0,
    @ColumnInfo var type: Int = 0,//部门类型  1.创建  2.修改  3.删除
    @ColumnInfo var sort: Int = 0,
    @ColumnInfo var remark: String? = null,
    @ColumnInfo var visible: Boolean = true,
    @Ignore var users: MutableList<UserDepartment>? = null
) {
    val name: String
        get() = cname
}

enum class EditAction(var action: Int) {
    CREATE(1), EDIT(2), DELETE(3)
}
