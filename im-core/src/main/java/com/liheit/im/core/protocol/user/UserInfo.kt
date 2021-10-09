package com.liheit.im.core.protocol.user

import com.liheit.im.core.protocol.Rsp

enum class UpdateUserInfoFlag(val value: Int) {
    // 更新用户信息Flag的位标识，定义如下：
    Logo(0x00000001), // bit0 头像
    Email(0x00000002), // bit1 邮箱
    Phone(0x00000004), // bit2 手机号码
    Tel(0x00000008), // bit3 座机号码
    Sign(0x00000010), // bit4 个性签名
    NameCN(0x00000020), // bit5 名字.中文
    NameEN(0x00000040), // bit6 名字.英文
    Job(0x00000080), // bit7 职位
    Sex(0x00000100), // bit8 性别
    Bday(0x00000200) // bit9 出生日期
}

data class UpdateUserInfoReq(
        val t: Long,//
        val flag: Int,
        val psw_old: String? = null,
        val psw_new: String? = null,
        val logo: Long? = null
)

data class UpdateUserInfoRsp(
        val t: Long,//
        val flag: Int,
        val logo: Long? = null
) : Rsp()