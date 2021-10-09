package com.liheit.im.core;

import java.util.HashMap;
import java.util.Map;

/**
 * IM通讯协议
 */

public class Cmd {

    public static final int ImpNone = 0; // 未知命令

    // 协议模式类型定义
    public static final int typeReq = 0x1000; // 请求
    public static final int typeRsp = 0x2000; // 响应
    public static final int typeNtc = 0x4000; // 通知
    public static final int typeAck = 0x8000; // 应答
    // 请求 <==> 响应
    public static final int ImpHeartbeatReq = typeReq + 0x00; // 心跳请求
    public static final int ImpHeartbeatRsp = typeRsp + 0x00; // 心跳响应
    public static final int ImpAccessReq = typeReq + 0x01; // 接入请求
    public static final int ImpAccessRsp = typeRsp + 0x01; // 接入响应
    public static final int ImpLoginReq = typeReq + 0x02; // 登录请求
    public static final int ImpLoginRsp = typeRsp + 0x02; // 登录响应
    public static final int ImpLogoutReq = typeReq + 0x03; // 登出请求
    public static final int ImpLogoutRsp = typeRsp + 0x03; // 登出响应
    public static final int ImpSendMsgReq = typeReq + 0x04; // 发送消息请求
    public static final int ImpSendMsgRsp = typeRsp + 0x04; // 发送消息响应
    public static final int ImpUpdateUserInfoReq = typeReq + 0x07; // 更新用户信息请求
    public static final int ImpUpdateUserInfoRsp = typeRsp + 0x07; // 更新用户信息响应
    public static final int ImpGetUserInfoReq = typeReq + 0x08; // 获取用户信息请求
    public static final int ImpGetUserInfoRsp = typeRsp + 0x08; // 获取用户信息响应
    public static final int ImpGetSessionMemberReq = typeReq + 0x09; // 获取会话成员请求
    public static final int ImpGetSessionMemberRsp = typeRsp + 0x09; // 获取会话成员响应
    public static final int ImpGetDeptListReq = typeReq + 0x0a; // 获取部门列表请求
    public static final int ImpGetDeptListRsp = typeRsp + 0x0a; // 获取部门列表响应
    public static final int ImpGetDeptUserListReq = typeReq + 0x0b; // 获取部门用户列表请求
    public static final int ImpGetDeptUserListRsp = typeRsp + 0x0b; // 获取部门用户列表响应
    public static final int ImpGetOfflineMsgReq = typeReq + 0x0c; // 获取离线消息请求(当客户端登录后，初始化完成环境，再需要请求)
    public static final int ImpGetOfflineMsgRsp = typeRsp + 0x0c; // 获取离线消息应答
    public static final int ImpGetSessionListReq = typeReq + 0x0d; // 获取会话列表请求
    public static final int ImpGetSessionListRsp = typeRsp + 0x0d; // 获取会话列表响应
    public static final int ImpSyncMsgReq = typeReq + 0x0e; // 消息已同步请求(客户端已经接收[离线消息]；客户端已经查看了新消息)
    public static final int ImpSyncMsgRsp = typeRsp + 0x0e; // 消息已同步响应(服务器已经收到)
    public static final int ImpCreateSessionReq = typeReq + 0x0f; // 创建群请求
    public static final int ImpCreateSessionRsp = typeRsp + 0x0f; // 创建群响应
    public static final int ImpModifySessionReq = typeReq + 0x10; // 修改群信息请求
    public static final int ImpModifySessionRsp = typeRsp + 0x10; // 修改群信息响应
    public static final int ImpGetMessageReq = typeReq + 0x11; // 获取指定会话所给定时间前的N条消息请求
    public static final int ImpGetMessageRsp = typeRsp + 0x11; // 获取指定会话所给定时间前的N条消息响应
//        public static final int ImpReceiptMsgReq       = typeReq + 0x12; // 消息回执请求
//        public static final int ImpReceiptMsgRsp       = typeRsp + 0x12; // 消息回执响应
//        public static final int ImpRecallMsgReq        = typeReq + 0x13; // 撤回消息请求
//        public static final int ImpRecallMsgRsp        = typeRsp + 0x13; // 撤回消息响应
    public static final int ImpGetUserStatusReq = typeReq + 0x14; // 获取员工在线状态请求
    public static final int ImpGetUserStatusRsp = typeRsp + 0x14; // 获取员工在线状态响应
    public static final int ImpModifyMyDataReq = typeReq + 0x15; // 修改常用信息请求【群、部门、联系人】
    public static final int ImpModifyMyDataRsp = typeRsp + 0x15; // 修改常用信息响应
    public static final int ImpGetMyDataListReq = typeReq + 0x16; // 获取常用信息列表请求
    public static final int ImpGetMyDataListRsp = typeRsp + 0x16; // 获取常用信息列表响应
    public static final int ImpUpdateDevTokenReq = typeReq + 0x17; // 更新离线推送设备Token
    public static final int ImpUpdateDevTokenRsp = typeRsp + 0x17; // 更新离线推送设备Token响应
    public static final int ImpGetDeptShowReq = typeReq + 0x18; // 更新部门权限请求
    public static final int ImpGetDeptShowRsp = typeRsp + 0x18; // 更新部门权限响应
    public static final int ImpGetCreateVoiceCallReq = typeReq + 0x19; // 语音通话房间创建请求
    public static final int ImpGetCreateVoiceCallRsp = typeRsp + 0x19; // 语音通话房间创建请求响应
    public static final int ImpJoinVoiceCallReq = typeReq + 0x20; // 加入退出语音通话房间
    public static final int ImpJoinVoiceCallRsp = typeRsp + 0x20; // 加入退出语音通话房间响应
    public static final int ImpGetVoiceRoomMemberReq = typeReq + 0x21; // 请求语音房间信息
    public static final int ImpGetVoiceRoomMemberRsp = typeRsp + 0x21; // 返回语音房间信息响应
    public static final int ImpGetVoiceTokenReq = typeReq + 0x22; // 请求语音初始化值
    public static final int ImpGetVoiceTokenRsp = typeRsp + 0x22; // 返回语音初始化值响应
    public static final int ImpGetVoiceStateReq = typeReq + 0x23; // 请求语音通话状态
    public static final int ImpGetVoiceStateRsp = typeRsp + 0x23; // 返回语音通话状态响应
    public static final int ImpSetVoiceRoomMemberReq = typeReq + 0x24; // 设置通话房间人员列表
    public static final int ImpSetVoiceRoomMemberRsp = typeRsp + 0x24; // 返回设置通话房间人员列表响应
    public static final int ImpJoinLeaveMeetingRoomReq = typeReq + 0x25; // 向会议房间添加或删除一人
    public static final int ImpJoinLeaveMeetingRoomRsp = typeRsp + 0x25; // 返回当前房间人数及信息
    public static final int ImpGetMeetingRoomInfoReq = typeReq + 0x26; // 请求会议房间信息
    public static final int ImpGetMeetingRoomInfoRsp = typeRsp + 0x26; // 返回会议房间信息
    public static final int ImpGetReadStateReq = typeReq + 0x27; //请求会话列表已读同步
    public static final int ImpGetReadStateRsp = typeRsp + 0x27; //返回会话列表已读同步响应
    public static final int ImpGetDeleteMsg = typeReq + 0x28; // 删除一条消息
    public static final int ImpGetDeleteMsgRsp = typeRsp + 0x28; //删除一条消息响应
    public static final int ImpGetCheckMsgExist = typeReq + 0x29; // 检查消息是否存在
    public static final int ImpGetCheckMsgExistRsp = typeRsp + 0x29; // 检查消息是否存在响应
    public static final int ImpVoiceCallOpenVideoReq = typeReq + 0x30; // 打开或关闭摄像头
    public static final int ImpVoiceCallOpenVideoRsp = typeRsp + 0x30; // 打开或关闭摄像头响应
    public static final int ImpVideoSwitchReq = typeReq + 0x31; // 单人视频通话切换成语音通话
    public static final int ImpVideoSwitchRsp = typeRsp + 0x31; // 单人视频通话切换成语音通话响应

    // 通知 <==> 应答
    public static final int ImpContactNotice = typeNtc + 0x02; // 通讯录状态更新通知
    public static final int ImpContactNoticeAck = typeAck + 0x02; // 通讯录状态更新通知应答
    public static final int ImpMsgNotice = typeNtc + 0x04; // 消息通知
    public static final int ImpMsgNoticeAck = typeAck + 0x04; // 消息通知应答
    public static final int ImpSyncMsgNotice = typeNtc + 0x0e; // 通知客户端新消息已读(多端状态同步)
    public static final int ImpSyncMsgNoticeAck = typeAck + 0x0e; // 报告服务器已经收到了消息已读同步
    public static final int ImpCreateSessionNotice = typeNtc + 0x0f; // 群创建通知
    public static final int ImpCreateSessionNoticeAck = typeAck + 0x0f; // 群创建通知应答
    public static final int ImpModifySessionNotice = typeNtc + 0x10; // 修改群信息通知
    public static final int ImpModifySessionNoticeAck = typeAck + 0x10; // 修改群信息通知应答
    //        public static final int ImpReceiptMgsNotice       = typeNtc + 0x12; // 消息回执通知
//        public static final int ImpReceiptMsgNoticeAck    = typeAck + 0x12; // 消息回执通知应答
//        public static final int ImpRecallMsgNotice        = typeNtc + 0x13; // 撤回消息通知
//        public static final int ImpRecallMsgNoticeAck     = typeAck + 0x13; // 撤回消息通知应答
    public static final int ImpModifyMyDataNotice = typeNtc + 0x14; // 修改常用信息通知
    public static final int ImpModifyMyDataNoticeAck = typeAck + 0x14; // 修改常用信息通知应答
    public static final int ImpVoiceCallNotice = typeNtc + 0x15; // 语音通话房间通知
    public static final int ImpVoiceCallNoticeAck = typeAck + 0x15; // 语音通话房间通知应答
    public static final int ImpMeetingNotice = typeNtc + 0x16; // 会议通话房间通知
    public static final int ImpMeetingNoticeAck = typeAck + 0x16; // 会议通话房间通知应答
    public static final int ImpSelfOnlineNotice = typeNtc + 0x17; // 同一账号在线状态通知，登陆或pc离线时会将在线状态发给PC或手机，登陆手机时如此
    public static final int ImpSelfOnlineNoticeAck = typeAck + 0x17; // 同一账号在线状态通知应答
    public static final int ImpMsgDeletenotice = typeNtc + 0x18; // 阅后即焚消息删除通知
    public static final int ImpMsgDeletenoticeAck = typeAck + 0x18; // 阅后即焚消息删除通知通知应答
    public static final int ImpVideoSwitchVoice = typeNtc + 0x19; // 单人视频通话切换成语音聊天通知响应
    public static final int ImpVideoSwitchVoiceAck = typeAck + 0x19; // 单人视频通话切换成语音聊天通知应答

    private static Map<Integer, String> cmdMapping = new HashMap<>();

    static {
        cmdMapping.put(ImpHeartbeatReq, "心跳请求");
        cmdMapping.put(ImpHeartbeatRsp, "心跳响应");
        cmdMapping.put(ImpAccessReq, "接入请求");
        cmdMapping.put(ImpAccessRsp, "接入响应");
        cmdMapping.put(ImpLoginReq, "登录请求");
        cmdMapping.put(ImpLoginRsp, "登录响应");
        cmdMapping.put(ImpLogoutReq, "登出请求");
        cmdMapping.put(ImpLogoutRsp, "登出响应");
        cmdMapping.put(ImpSendMsgReq, "发送消息请求");
        cmdMapping.put(ImpSendMsgRsp, "发送消息响应");
        cmdMapping.put(ImpUpdateUserInfoReq, "更新用户信息请求");
        cmdMapping.put(ImpUpdateUserInfoRsp, "更新用户信息响应");
        cmdMapping.put(ImpGetUserInfoReq, "获取用户信息请求");
        cmdMapping.put(ImpGetUserInfoRsp, "获取用户信息响应");
        cmdMapping.put(ImpGetSessionMemberReq, "获取会话成员请求");
        cmdMapping.put(ImpGetSessionMemberRsp, "获取会话成员响应");
        cmdMapping.put(ImpGetDeptListReq, "获取部门列表请求");
        cmdMapping.put(ImpGetDeptListRsp, "获取部门列表响应");
        cmdMapping.put(ImpGetDeptUserListReq, "获取部门用户列表请求");
        cmdMapping.put(ImpGetDeptUserListRsp, "获取部门用户列表响应");
        cmdMapping.put(ImpGetOfflineMsgReq, "获取离线消息请求(当客户端登录后，初始化完成环境，再需要请求)");
        cmdMapping.put(ImpGetOfflineMsgRsp, "获取离线消息应答");
        cmdMapping.put(ImpGetSessionListReq, "获取会话列表请求");
        cmdMapping.put(ImpGetSessionListRsp, "获取会话列表响应");
        cmdMapping.put(ImpSyncMsgReq, "消息已同步请求(客户端已经接收[离线消息]；客户端已经查看了新消息)");
        cmdMapping.put(ImpSyncMsgRsp, "消息已同步响应(服务器已经收到)");
        cmdMapping.put(ImpCreateSessionReq, "创建群请求");
        cmdMapping.put(ImpCreateSessionRsp, "创建群响应");
        cmdMapping.put(ImpModifySessionReq, "修改群信息请求");
        cmdMapping.put(ImpModifySessionRsp, "修改群信息响应");
        cmdMapping.put(ImpGetMessageReq, "获取指定会话所给定时间前的N条消息请求");
        cmdMapping.put(ImpGetMessageRsp, "获取指定会话所给定时间前的N条消息响应");
//        cmdMapping.put(ImpReceiptMsgReq       ,"消息回执请求");
//        cmdMapping.put(ImpReceiptMsgRsp       ,"消息回执响应");
//        cmdMapping.put(ImpRecallMsgReq        ,"撤回消息请求");
//        cmdMapping.put(ImpRecallMsgRsp        ,"撤回消息响应");
        cmdMapping.put(ImpGetUserStatusReq, "获取员工在线状态请求");
        cmdMapping.put(ImpGetUserStatusRsp, "获取员工在线状态响应");
        cmdMapping.put(ImpModifyMyDataReq, "修改常用信息请求【群、部门、联系人");
        cmdMapping.put(ImpModifyMyDataRsp, "修改常用信息响应");
        cmdMapping.put(ImpGetMyDataListReq, "获取常用信息列表请求");
        cmdMapping.put(ImpGetMyDataListRsp, "获取常用信息列表响应");
        cmdMapping.put(ImpUpdateDevTokenReq, "更新离线推送设备Token");
        cmdMapping.put(ImpUpdateDevTokenRsp, "更新离线推送设备Token响应");
        cmdMapping.put(ImpGetDeptShowReq, "更新部门权限请求");
        cmdMapping.put(ImpGetDeptShowRsp, "更新部门权限响应");
        cmdMapping.put(ImpGetCreateVoiceCallReq, "语音通话房间创建请求");
        cmdMapping.put(ImpGetCreateVoiceCallRsp, "语音通话房间创建请求响应");
        cmdMapping.put(ImpJoinVoiceCallReq, "加入语音通话房间");
        cmdMapping.put(ImpJoinVoiceCallRsp, "加入语音通话房间响应");
        cmdMapping.put(ImpGetVoiceRoomMemberReq, "请求语音房间信息");
        cmdMapping.put(ImpGetVoiceRoomMemberRsp, "返回语音房间信息响应");
        cmdMapping.put(ImpGetVoiceTokenReq, "请求语音初始化值");
        cmdMapping.put(ImpGetVoiceTokenRsp, "返回语音初始化值响应");
        cmdMapping.put(ImpGetVoiceStateReq, "请求语音通话状态");
        cmdMapping.put(ImpGetVoiceStateRsp, "返回语音通话状态响应");
        cmdMapping.put(ImpSetVoiceRoomMemberReq, "设置通话房间人员列表");
        cmdMapping.put(ImpSetVoiceRoomMemberRsp, "返回设置通话房间人员列表响应");
        cmdMapping.put(ImpJoinLeaveMeetingRoomReq, "向会议房间添加或删除一人");
        cmdMapping.put(ImpJoinLeaveMeetingRoomRsp, "返回当前房间人数及信息");
        cmdMapping.put(ImpGetMeetingRoomInfoReq, "请求会议房间信息");
        cmdMapping.put(ImpGetMeetingRoomInfoRsp, "返回会议房间信息");
        cmdMapping.put(ImpGetReadStateReq, "请求会话列表已读同步");
        cmdMapping.put(ImpGetReadStateRsp, "返回会话列表已读同步响应");
        cmdMapping.put(ImpGetDeleteMsg, "删除一条消息");
        cmdMapping.put(ImpGetDeleteMsgRsp, "删除一条消息响应");
        cmdMapping.put(ImpGetCheckMsgExist, "检查消息是否存在");
        cmdMapping.put(ImpGetCheckMsgExistRsp, "检查消息是否存在响应");
        cmdMapping.put(ImpVoiceCallOpenVideoReq, "打开或关闭摄像头");
        cmdMapping.put(ImpVoiceCallOpenVideoRsp, "打开或关闭摄像头响应");
        cmdMapping.put(ImpVideoSwitchReq, "单人视频通话切换成语音通话");
        cmdMapping.put(ImpVideoSwitchRsp, "单人视频通话切换成语音通话响应");

        // 通知 <==> 应答
        cmdMapping.put(ImpContactNotice, "通讯录状态更新通知");
        cmdMapping.put(ImpContactNoticeAck, "通讯录状态更新通知应答");
        cmdMapping.put(ImpMsgNotice, "消息通知");
        cmdMapping.put(ImpMsgNoticeAck, "消息通知应答");
        cmdMapping.put(ImpSyncMsgNotice, "通知客户端新消息已读(多端状态同步)");
        cmdMapping.put(ImpSyncMsgNoticeAck, "报告服务器已经收到了消息已读同步");
        cmdMapping.put(ImpCreateSessionNotice, "群创建通知");
        cmdMapping.put(ImpCreateSessionNoticeAck, "群创建通知应答");
        cmdMapping.put(ImpModifySessionNotice, "修改群信息通知");
        cmdMapping.put(ImpModifySessionNoticeAck, "修改群信息通知应答");
//        cmdMapping.put(ImpReceiptMgsNotice       ,"消息回执通知");
//        cmdMapping.put(ImpReceiptMsgNoticeAck    ,"消息回执通知应答");
//        cmdMapping.put(ImpRecallMsgNotice        ,"撤回消息通知");
//        cmdMapping.put(ImpRecallMsgNoticeAck     ,"撤回消息通知应答");
        cmdMapping.put(ImpModifyMyDataNotice, "修改常用信息通知");
        cmdMapping.put(ImpModifyMyDataNoticeAck, "修改常用信息通知应答");
        cmdMapping.put(ImpVoiceCallNotice, "语音通话房间通知");
        cmdMapping.put(ImpVoiceCallNoticeAck, "语音通话房间通知应答");
        cmdMapping.put(ImpMeetingNotice, "会议通话房间通知");
        cmdMapping.put(ImpMeetingNoticeAck, "会议通话房间通知应答");
        cmdMapping.put(ImpSelfOnlineNotice, "同一账号在线状态通知");
        cmdMapping.put(ImpSelfOnlineNoticeAck, "同一账号在线状态通知应答");
        cmdMapping.put(ImpMsgDeletenotice, "阅后即焚消息删除通知");
        cmdMapping.put(ImpMsgDeletenoticeAck, "阅后即焚消息删除通知通知应答");
        cmdMapping.put(ImpVideoSwitchVoice, "单人视频通话切换成语音聊天通知响应");
        cmdMapping.put(ImpVideoSwitchVoiceAck, "单人视频通话切换成语音聊天通知应答");
    }

    public static String getCmdName(int cmd) {
        String name = cmdMapping.get(cmd);
        if (name == null) {
            return "未知命令";
        } else {
            return name;
        }
    }
}
