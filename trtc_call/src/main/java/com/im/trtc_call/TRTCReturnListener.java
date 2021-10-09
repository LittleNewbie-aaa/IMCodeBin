package com.im.trtc_call;

import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天房间处理结果通知(TRRCService监听返回给操作者的通知)
 */
public interface TRTCReturnListener {

    /**
     * 进入房间成功
     */
    void enterTRTCRoomSucceed();

    /**
     * 进入房间失败
     */
    void enterTRTCRoomError();

    /**
     * 打开关闭麦克风结果返回
     */
    void enableMicReturn(Boolean enable);

    /**
     * 打开扬声器（耳机）结果返回
     */
    void enableHandfreeReturn(Boolean isUseHandsfree);

    /**
     * 聊天时长
     */
    void timeReturn(String time);

    /**
     * 聊天房间用户变化监听
     * @param chooseIds
     * @param joinIds
     */
    void onRoomUserChange(ArrayList<Long> chooseIds, ArrayList<Long> joinIds);

    /**
     * 聊天关闭
     */
    void onCloseVoiceChat();

    /**
     * 聊天页面关闭
     */
    void onCloseVoiceChatActivity();

    /**
     * 聊天房间用户音量监听
     * @param userVolumes
     */
    void userVolumeChange(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes);

    /**
     * 聊天房间用户设置静音监听
     */
    void onUserAudioAvailable(String userId, boolean available);

    /**
     * 远程用户打开或关闭视频
     *
     * @param userId
     */
    void onUserVideoAvailable(String userId, boolean isVideoAvailable);

    /**
     * 远程用户退出聊天房间
     *
     * @param userId
     */
    void onRemoteUserLeaveRoom(String userId);

    /**
     * 远程用户未加入聊天房间移除头像
     *
     * @param userIds
     */
    void onRemoteUserLeaveRoom(ArrayList<Long> userIds);

    /**
     * 刷新用户视频打开关闭
     */
    void refreshUserVideoView();

    /**
     * 添加用户加入聊天
     */
    void addChooseUsers(List<Long> addChooseUsers);

    /**
     * 从视频通话切换到语音通话通知
     */
    void changeToVideoCall();
}
