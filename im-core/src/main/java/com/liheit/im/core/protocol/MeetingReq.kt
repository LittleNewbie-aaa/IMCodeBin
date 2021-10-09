package com.liheit.im.core.protocol

//加入离开会议房间
class MeetingJoinLeaveRoomMemberReq(var userid: Long,//用户Id
                                    var sid: String,// 会话ID
                                    var status: Int)// 0 退出 1 加入

//加入离开会议房间响应
class MeetingJoinLeaveRoomMemberRsp(var state: Boolean): Rsp()

//请求语音房间信息
class MeetingRoomInfoReq(var sid: String)

//请求语音房间信息响应
class MeetingRoomInfoRsp( var sid: String,// 会话ID
                          var membernum: Int,// 房间成员数量
                          var ids: MutableList<Long>,// 房间当前成员ID列表
                          var term: Int): Rsp()

//会议房间状态通知
class MeetingCallNotice(var userid: Long,// 加入或者退出的用户id
                        var sid: String,// 会话ID
                        var status: Int,// 0 退出 1 加入
                        var membernum: Int,// 房间成员数量
                        var ids: MutableList<Long>,// 房间当前成员ID列表
                        var term: Int)// 终端类型 1 ios 2 android 3 mac 4