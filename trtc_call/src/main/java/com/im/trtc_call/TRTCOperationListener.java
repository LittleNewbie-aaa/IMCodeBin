package com.im.trtc_call;

import com.tencent.rtmp.ui.TXCloudVideoView;

import java.util.List;

/**
 * 对聊天房间操作通知接口(主要通知TRRCService)
 */
public interface TRTCOperationListener {

    /**
     * 进入房间
     */
    void enterTRTCRoom();

    /**
     * 退出房间
     */
    void exitTRTCRoom();

    /**
     * 展示悬浮窗
     */
    void showFloatingWindow();

    /**
     * 隐藏悬浮窗
     */
    void hintFloatingWindow();

    /**
     * 打开关闭麦克风
     *
     * @param enable
     */
    void enableMic(Boolean enable);

    /**
     * 打开扬声器（耳机）
     */
    void enableHandfree(Boolean isUseHandsfree);

    /**
     * 添加用户加入聊天
     */
    void addChooseUser(List<Long> chooseUsers, long callTime);

    /**
     * 您可以调用该函数开启摄像头，并渲染在指定的TXCloudVideoView中
     * 处于通话中的用户会收到 {@link} 回调
     *
     * @param isFrontCamera    是否开启前置摄像头
     * @param txCloudVideoView 摄像头的数据将渲染到该view中
     */
    void openCamera(boolean isFrontCamera, TXCloudVideoView txCloudVideoView);

    /**
     * 您可以调用该函数关闭摄像头
     */
    void closeCamera();

    /**
     * 当您收到 onUserVideoAvailable 回调时，可以调用该函数将远端用户的摄像头数据渲染到指定的TXCloudVideoView中
     *
     * @param userId           远端用户id
     * @param txCloudVideoView 远端用户数据将渲染到该view中
     */
    void startRemoteView(String userId, TXCloudVideoView txCloudVideoView);

    /**
     * 当您收到 onUserVideoAvailable 回调为false时，可以停止渲染数据
     *
     * @param userId 远端用户id
     */
    void stopRemoteView(String userId);

    /**
     * 切换到音频通话
     *
     */
    void switchVideoCall();
}
