package com.liheit.im.core.protocol

class CreateVoiceCallReqMsg(var sid: String? = null,//会话id
                            var bvideo: Boolean//是否为语音通话
                            )

class CreateVoiceCallRspMsg(
        var sid: String,//会话id
        var roomid: Int,//房间id
        var states: String = "",//请求状态
        var sdkappid: Int,//腾讯语音聊天id
        var sdkusersig: String = ""//腾讯语音聊天Sig
) : Rsp()

enum class const(val value: String) {
    VOICECALLTRUE("true"),//创建成功
    VOICECALLUSING("using"),//本SID正在通话中
    VOICECALLTALKING("talking"),//您正在语音聊天中
    VOICECALLFALSE("false"),//未知错误
}

class JoinVoiceCallReqMsg(
        var sid: String? = null,
        var roomid: Int,//聊天房间id
        var userid: Long,
        var status: Int,//0退出  1加入 2拒绝
        var inviterid: Long //邀请人id
)

class JoinVoiceCallRspMsg(
        var sid: String,//会话id
        var roomid: Int,//房间id
        var states: String = "",//请求状态 true  false
        var ids: MutableList<Long>?,//房间当前成员
        var allids: MutableList<Long>?,//房间原本成员列表
        var leaveids: MutableList<Long>?,//离开房间或拒绝进入成员列表
        var videoids: MutableList<String>?,//打开视频人员的id
        var dismissed: Boolean//房间是否存在
) : Rsp()

class GetVoiceTokenRspMsg(
        var sdkappid: Int,//腾讯语音聊天id
        var sdkusersig: String = ""//腾讯语音聊天Sig
) : Rsp()

class VoiceRoomMemberReqMsg(var roomid: Int)//房间id

class VoiceRoomMemberRspMsg(
        var roomid: Int,//房间id
        var membernum: Int,//房间成员数量
        var ids: MutableList<Long>?,//房间当前成员列表
        var allids: MutableList<Long>?,//房间原本成员列表
        var leaveids: MutableList<Long>?,//离开房间或拒绝进入成员列表
        var videoids: MutableList<String>?,//打开视频人员的id
        var dismissed: Boolean//房间是否存在
) : Rsp() {
    //获取等待中的人员列表
    fun getWaitIds(): MutableList<Long> {
        var temporary = mutableListOf<Long>()
        leaveids?.let { temporary.addAll(it) }
        return allids!!.subtract(temporary).toMutableList()
    }

    //获取已经进入房间中的人员列表
    fun getNowIds(): MutableList<Long> {
        return ids!!.union(getWaitIds()).toMutableList()
    }

    //获取房间中全部的人员（包括已经进入的和正在通话中的人员）
    fun getAllIds(): MutableList<Long> {
        return getNowIds().union(getWaitIds()).toMutableList()
    }

    fun getVideoIds(): MutableList<String> {
        return if (videoids.isNullOrEmpty()) {
            mutableListOf<String>()
        } else {
            videoids!!.toMutableList()
        }
    }
}

class GetVoiceStateRspMsg(
        var talking: Boolean,//是否正在通话
        var roomid: Int,//房间id
        var term: Int//终端类型
) : Rsp()

class SetVoiceRoomMemberReq(
        var sid: String? = null,//会话id
        var roomid: Int,//房间id
        var ids: MutableList<Long>?
)//房间当前成员

class SetVoiceRoomMemberRsp(
        var state: Boolean
) : Rsp()

class VoiceCallOpenVideoReq(
        var roomid: Int,//房间id
        var open: Boolean //是否开启 1开启 0关闭
)

class VoiceCallOpenVideoRsp(
        var roomid: Int,//房间id
        var open: Boolean //是否开启 1开启 0关闭
) : Rsp()

//切换到语音聊天请求参数
class SwitchVideoReq(
    var roomid: Int//房间id
)