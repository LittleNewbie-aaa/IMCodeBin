package com.liheit.im.core.protocol

import com.liheit.im.core.protocol.annotation.LoginStatus


/**
 * Created by daixun on 2018/6/21.
 */

data class LogoutReq(@LoginStatus var status: Int) {

    companion object {
        const val MANAGEKICKOUT = -5
        const val DISABLED = -2
        const val KICKED = -1
        const val OFFLINE = 0
        const val LOGIN = 1
        const val ONLINE = 2
        const val LEAVE = 3
        //状态:-5管理台T下线;-2-账号禁用;-1-被踢;0-退出(离线);1-登录中;2-在线;3-离开;

        /*eStatusFailed   =-4,	// login failed
        eStatusNoLogin	=-3,	// No Login
        eStatusForbidden=-2,	// Account forbidden
        eStatusKicked	=-1,	// Kicked
        eStatusExit		= 0,	// Exit/Logout
        eStatusLoging	= 1,	// Logining
        eStatusOnline	= 2,	// Online
        eStatusLeave	= 3,	// Leave*/
    }
}



