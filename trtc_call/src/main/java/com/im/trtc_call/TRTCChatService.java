package com.im.trtc_call;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dagger.baselib.utils.DpTools;
import com.daixun.audio.AudioRecorderPanel;
import com.google.gson.Gson;
import com.liheit.im.core.bean.VoiceChatRoomChangeMsg;
import com.liheit.im.core.manager.ChatManager;
import com.liheit.im.core.Cmd;
import com.liheit.im.core.IMClient;
import com.liheit.im.core.bean.User;
import com.liheit.im.utils.TimeUtils;
import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import org.apache.commons.lang3.ArrayUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * 腾讯实时音视频聊天服务
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class TRTCChatService extends Service implements TRTCOperationListener {
    public static boolean isVideoPaneOpen = false;//是否是点击悬浮窗进入的
    public static boolean isStarted = false;//TRRCService是否初始化
    public static boolean TRRCStarted = false;//是否进入音视频聊天房间
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View displayView = null;
    private LinearLayout llCommon;
    private TXCloudVideoView myVideoLayout, videoLayout;

    private TRTCCloud mTRTCCloud = null;//腾讯音视频聊天服务
    private TextView tv_time;//通话时长

    private String userSig = "";//腾讯音视频userSig
    private int sdkAppId = 0;//腾讯音视频sdkId
    private Long uID = 0L;//当前用户id
    private Long createrid = 0L;//音视频聊天创建者id
    private Long inviterid = 0L;//邀请人id
    public static String sid = "";//当前会话sid
    private int roomId = 0;//音视频房间id
    private int chatType = 0;//会话类型(0单人 1多人)
    private int joinType = 0;//人员角色（0：加入；1：创建 2：中间加入）
    private int TRTCType = 0;//音视频聊天类型（0：语音；1：视频）
    private long callTime = 0;//音视频通话开始时间
    public static ArrayList<Long> chooseIds = new ArrayList<>();
    private static ArrayList<Long> joinIds = new ArrayList<>();
    public static ArrayList<String> isOpenVideoIds = new ArrayList<>();//已经打开视频的用户id集合

    private Boolean isClick = true;//悬浮窗是否可点击
    private Boolean isAddFloating = false;//悬浮窗是否添加

    private Handler mHandler = null;
    private static long baseTimer = 0;//音视频聊天开始的时间
    public static boolean isEnableMic = true;//是否打开麦克风
    public static boolean isUseHandsfree = true;//是否打开免提
    public static boolean isOpenCamera = false;//是否打开摄像头
    public static boolean remoteIsOpenCamera = false;//是否打开摄像头
    public static String remoteUId = "";//单人聊天对方id
    public static boolean isFrontCamera = true;//是否是前置摄像头

    @Override
    public void onCreate() {
        super.onCreate();
        AudioRecorderPanel.isChatServiceCall = true;
        initView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isStarted) {
            this.sid = intent.getStringExtra(TRTCChatVM.SID);
            this.createrid = intent.getLongExtra(TRTCChatVM.CREATERID, 0);
            this.inviterid = intent.getLongExtra(TRTCChatVM.INVITERID, 0);
            this.roomId = intent.getIntExtra(TRTCChatVM.ROOMID, 0);
            this.userSig = intent.getStringExtra(TRTCChatVM.SUERSIG);
            this.sdkAppId = intent.getIntExtra(TRTCChatVM.SDKAPPID, 0);
            this.chatType = intent.getIntExtra(TRTCChatVM.CHAT_TYPE, 0);
            this.joinType = intent.getIntExtra(TRTCChatVM.JOIN_TYPE, 0);
            this.TRTCType = intent.getIntExtra(TRTCChatVM.TRTC_TYPE, 0);
            this.callTime = intent.getLongExtra(TRTCChatVM.CALL_TIME, 0);
            long[] ids = intent.getLongArrayExtra(TRTCChatVM.EXTRA_SELECTED_IDS);
            for (long id : ids) {
                this.chooseIds.add(id);
            }
            if (!joinIds.contains(uID)) {
                joinIds.add(uID);
            }
            if (joinType == 2) {
                enterTRTCRoom();
            }
            this.isOpenVideoIds = intent.getStringArrayListExtra(TRTCChatVM.VIDEO_IDS);
            if (this.isOpenVideoIds == null) {
                this.isOpenVideoIds = new ArrayList<String>();
            }
            isStarted = true;
            long countdown = (callTime + 1000 * 31) - TimeUtils.INSTANCE.getServerTime();
            Log.e("aaa ","$countdown="+countdown);
            final ArrayList<Long> temporaryList = new ArrayList<Long>();
            temporaryList.addAll(chooseIds);
            if (countdown > 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Long> noJoins = substract(temporaryList, joinIds);
                        Log.e("aaa","onStartCommand postDelayed list="+ new Gson().toJson(noJoins));
                        if (TRRCUtils.getInstanse().getRListener() != null) {
                            TRRCUtils.getInstanse().getRListener().onRemoteUserLeaveRoom(noJoins);
                        }
                        chooseIds.removeAll(noJoins);
                    }
                },countdown);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = DpTools.dp2px(this, 64);
        layoutParams.height = DpTools.dp2px(this, 88);
        layoutParams.x = 1000;
        layoutParams.y = DpTools.dp2px(this, 55);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        displayView = layoutInflater.inflate(R.layout.voice_chat_display, null);
        displayView.setFocusableInTouchMode(false);
        displayView.setOnTouchListener(new FloatingOnTouchListener());
        displayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClick) {
                    Intent intent = new Intent(getPackageName() + ".android.CallReceiver");
                    intent.putExtra(TRTCChatVM.SDKAPPID, sdkAppId);
                    intent.putExtra(TRTCChatVM.SUERSIG, userSig);
                    intent.putExtra(TRTCChatVM.SID, sid);
                    intent.putExtra(TRTCChatVM.CREATERID, createrid);
                    intent.putExtra(TRTCChatVM.INVITERID, inviterid);
                    intent.putExtra(TRTCChatVM.ROOMID, roomId);
                    intent.putExtra(TRTCChatVM.CHAT_TYPE, chatType);
                    intent.putExtra(TRTCChatVM.JOIN_TYPE, joinType);
                    intent.putExtra(TRTCChatVM.TRTC_TYPE, TRTCType);
                    intent.putExtra(TRTCChatVM.CALL_TIME, callTime);
                    Long[] cIDS = chooseIds.toArray(new Long[chooseIds.size()]);
                    intent.putExtra(TRTCChatVM.EXTRA_SELECTED_IDS, ArrayUtils.toPrimitive(cIDS));
                    Long[] inIDS = joinIds.toArray(new Long[joinIds.size()]);
                    intent.putExtra(TRTCChatVM.JOIN_IDS, ArrayUtils.toPrimitive(inIDS));
                    String[] videoIds = isOpenVideoIds.toArray(new String[isOpenVideoIds.size()]);
                    intent.putExtra(TRTCChatVM.VIDEO_IDS, videoIds);
                    Log.e("aaa ", "TRTCChatService userVideo=" + new Gson().toJson(isOpenVideoIds));
                    intent.setPackage(getPackageName());
                    sendBroadcast(intent);
                }
            }
        });
        tv_time = displayView.findViewById(R.id.tv_time);
        uID = IMClient.INSTANCE.getCurrentUserId();

        llCommon = displayView.findViewById(R.id.llCommon);
        myVideoLayout = displayView.findViewById(R.id.myVideo);
        videoLayout = displayView.findViewById(R.id.video);

        mHandler = new MyHandler(tv_time);
        if (Settings.canDrawOverlays(this)) {
            isAddFloating = true;
            windowManager.addView(displayView, layoutParams);
        }
        displayView.setVisibility(View.GONE);
        initTRRC();
        addCmdMessageListener();
        addUserStateListener();

        if (joinType != 2) {
            mHandler.sendEmptyMessageDelayed(4, 1000 * 30);
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //初始化语音聊天服务
    private void initTRRC() {
        TRRCUtils.getInstanse().setOListener(this);
        mTRTCCloud = TRTCCloud.sharedInstance(this);

        // 开启基础美颜
        TXBeautyManager txBeautyManager = mTRTCCloud.getBeautyManager();
        // 自然美颜
        txBeautyManager.setBeautyStyle(1);
        txBeautyManager.setBeautyLevel(6);
        // 进房前需要设置一下关键参数
        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540;
        encParam.videoFps = 15;
        encParam.videoBitrate = 1000;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        encParam.enableAdjustRes = true;
        mTRTCCloud.setVideoEncoderParam(encParam);
        mTRTCCloud.enableAudioVolumeEvaluation(800);
        mTRTCCloud.setListener(mChatRoomTRTCListener);
        int audioRoute;
        if (isUseHandsfree) {
            audioRoute = TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER;
        } else {
            audioRoute = TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE;
        }
        mTRTCCloud.setAudioRoute(audioRoute);
        mTRTCCloud.startLocalAudio();
    }

    @SuppressLint("CheckResult")
    public void addUserStateListener() {
        IMClient.INSTANCE.userStateListener()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<IMClient.UserState>() {
                    @Override
                    public void accept(IMClient.UserState userState) throws Exception {
                        if (userState == IMClient.UserState.KICKED_OUT) {
                            mHandler.sendEmptyMessage(3);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }

    @Override
    public void addChooseUser(List<Long> addChooseUsers, long callTime) {
        Log.e("aaa", "addChooseUsers=" + new Gson().toJson(addChooseUsers));
        for (long id : addChooseUsers) {
            if (!chooseIds.contains(id)) {
                chooseIds.add(id);
            }
        }
        if (TRRCUtils.getInstanse().getRListener() != null) {
            TRRCUtils.getInstanse().getRListener().addChooseUsers(chooseIds);
        }
        final ArrayList<Long> temporaryList = new ArrayList<Long>();
        temporaryList.addAll(addChooseUsers);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<Long> noJoins = substract(temporaryList, joinIds);
                Log.e("aaa","postDelayed list="+ new Gson().toJson(noJoins));
                chooseIds.removeAll(noJoins);
                if (TRRCUtils.getInstanse().getRListener() != null) {
                    TRRCUtils.getInstanse().getRListener().onRemoteUserLeaveRoom(noJoins);
                }
            }
        },30*1000);
    }

    private ChatManager.CmdMessageListener msgListener;

    //音视频通话房间通知监听
    private void addCmdMessageListener() {
        msgListener = new ChatManager.CmdMessageListener() {
            @Override
            public void onMessageReceived(int cmd, String data) {
                Log.e("aaa", "addCmdMessageListener " + data);
                if (cmd == Cmd.ImpVoiceCallNotice) {
                    VoiceChatRoomChangeMsg callMsg = new Gson().fromJson(data, VoiceChatRoomChangeMsg.class);
                    disposeCallMsg(callMsg);
                } else if (cmd == Cmd.ImpVideoSwitchVoice) {
                    switchVideoCall();
                }
            }
        };

        IMClient.INSTANCE.getChatManager().addCmdMessageListener(msgListener);
    }

    /**
     * 处理音视频通话房间通知
     *
     * @param callMsg
     */
    private void disposeCallMsg(VoiceChatRoomChangeMsg callMsg) {
        if (callMsg.getSid().equals(sid)) {
            //自己在其他客户端处理
            if (callMsg.getUserid().equals(uID) && callMsg.getTerm() != 2) {
                Message message = new Message();
                message.what = 5;
                message.obj = "您已在其他客户端处理";
                mHandler.sendMessage(message);
                if (TRRCUtils.getInstanse().getRListener() != null) {
                    TRRCUtils.getInstanse().getRListener().onCloseVoiceChatActivity();
                }
                if (isAddFloating) {
                    windowManager.removeView(displayView);
                }
                stopService(new Intent(getApplicationContext(), TRTCChatService.class));
                return;
            }
            //有人加入聊天房间
            if (callMsg.getStatus() == 1) {
                if (!TRRCStarted && !callMsg.getUserid().equals(uID) && joinType == 1) {
                    enterTRTCRoom();
                }
                if (!chooseIds.contains(callMsg.getUserid())) {
                    chooseIds.add(callMsg.getUserid());
                }
                if (!joinIds.contains(callMsg.getUserid())) {
                    joinIds.add(callMsg.getUserid());
                }
            } else {//有人拒绝或退出聊天房间
                chooseIds.remove(callMsg.getUserid());
                joinIds.remove(callMsg.getUserid());
                if (TRRCUtils.getInstanse().getRListener() != null) {
                    TRRCUtils.getInstanse().getRListener().onRemoteUserLeaveRoom(callMsg.getUserid().toString());
                }
                Log.e("aaa chooseIds=", new Gson().toJson(chooseIds));
                Log.e("aaa joinIds=", new Gson().toJson(joinIds));
                if (joinIds.size() == 1) {
                    mHandler.sendEmptyMessageDelayed(4, 1000 * 30);
                }
                if (callMsg.getStatus() == 2 && callMsg.getInviterid().equals(IMClient.INSTANCE.getCurrentUserId())) {
                    Log.e("aaa","callMsg.getInviterid()="+callMsg.getInviterid());
                    User user = IMClient.INSTANCE.getUserManager().getUserById(callMsg.getUserid());
                    Message message = new Message();
                    message.what = 5;
                    message.obj = user.getCname() + "已拒绝您的通话请求";
                    mHandler.sendMessage(message);
                }
                Log.e("aaa", "createrid=" + createrid);
                if (callMsg.getUserid().equals(createrid) && !TRRCStarted) {
                    if (joinIds.size() == 1) {
                        if (TRRCUtils.getInstanse().getRListener() != null) {
                            TRRCUtils.getInstanse().getRListener().onCloseVoiceChat();
                        } else {
                            mHandler.sendEmptyMessage(3);
                        }
                    }
                }
            }
            if (TRRCUtils.getInstanse().getRListener() != null) {
                if (chooseIds.size() == 0 || chooseIds.size() == 1 && chooseIds.get(0).equals(uID)) {
                    if (TRRCUtils.getInstanse().getRListener() != null) {
                        TRRCUtils.getInstanse().getRListener().onCloseVoiceChat();
                    }
                }
            } else {
                if (chooseIds.size() == 0 || chooseIds.size() == 1 && chooseIds.get(0).equals(uID)) {
                    mHandler.sendEmptyMessage(3);
                }
            }
        }
    }

    //进入语音聊天房间
    @Override
    public void enterTRTCRoom() {
        Log.e("aaa", "TRTCType=" + TRTCType);
        // 拼接进房参数
        TRTCCloudDef.TRTCParams params = new TRTCCloudDef.TRTCParams();
        params.userSig = userSig;
        params.sdkAppId = sdkAppId;
        params.roomId = roomId;
        params.role = TRTCCloudDef.TRTCRoleAnchor;
        params.userId = uID.toString();
        mTRTCCloud.enterRoom(params, TRTCType == 1 ? TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL : TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL);
        mHandler.removeMessages(4);
    }

    //开始本地视频渲染
    @Override
    public void openCamera(boolean isFrontCamera, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        TRTCChatService.isFrontCamera = isFrontCamera;
        if (isOpenCamera) {
            mTRTCCloud.stopLocalPreview();
        }
        Log.e("aaa", " openCamera");
        mTRTCCloud.startLocalPreview(isFrontCamera, txCloudVideoView);
        isOpenCamera = true;
    }

    @Override
    public void closeCamera() {
        mTRTCCloud.stopLocalPreview();
        isOpenCamera = false;
    }

    //开始拉取远端用户视频渲染
    @Override
    public void startRemoteView(String userId, TXCloudVideoView txCloudVideoView) {
        if (txCloudVideoView == null) {
            return;
        }
        mTRTCCloud.startRemoteView(userId, txCloudVideoView);
    }

    //结束用户视频渲染
    @Override
    public void stopRemoteView(String userId) {
        mTRTCCloud.stopRemoteView(userId);
    }

    @Override
    public void switchVideoCall() {
        if (TRTCType == 1) {
            closeCamera();
            mTRTCCloud.stopAllRemoteView();
            TRTCType = 0;
            if (TRRCUtils.getInstanse().getRListener() != null) {
                TRRCUtils.getInstanse().getRListener().changeToVideoCall();
            }
            Objects.requireNonNull(IMClient.INSTANCE.getChatManager()
                    .switchVideoCall(roomId).toObservable().subscribe());
        }
    }

    //退出语音聊天房间
    @Override
    public void exitTRTCRoom() {
        if (isAddFloating) {
            windowManager.removeView(displayView);
        }
        if (isStarted) {
            if (joinType == 0) {//0为被邀请者 1为创建者 2为中途加入
                if (TRRCStarted) {
                    Objects.requireNonNull(IMClient.INSTANCE.getChatManager()
                            .joinVoiceCall(sid, roomId, uID, 0, inviterid)).toObservable().subscribe();
                } else {
                    Objects.requireNonNull(IMClient.INSTANCE.getChatManager()
                            .joinVoiceCall(sid, roomId, uID, 2, inviterid)).toObservable().subscribe();
                }
            } else {
                Objects.requireNonNull(IMClient.INSTANCE.getChatManager()
                        .joinVoiceCall(sid, roomId, uID, 0, inviterid)).toObservable().subscribe();
            }
        }
        if (TRRCStarted) {
            mTRTCCloud.exitRoom();
        }
        stopService(new Intent(getApplicationContext(), TRTCChatService.class));
    }

    //麦克风设置
    @Override
    public void enableMic(Boolean enable) {
        this.isEnableMic = enable;
        if (TRRCUtils.getInstanse().getRListener() != null) {
            TRRCUtils.getInstanse().getRListener().enableMicReturn(enable);
        }
        if (enable) {
            mTRTCCloud.startLocalAudio();
        } else {
            mTRTCCloud.stopLocalAudio();
        }
    }

    //扬声器（耳机）
    @Override
    public void enableHandfree(Boolean isUseHandsfree) {
        this.isUseHandsfree = isUseHandsfree;
        if (TRRCUtils.getInstanse().getRListener() != null) {
            TRRCUtils.getInstanse().getRListener().enableHandfreeReturn(isUseHandsfree);
        }
        int audioRoute;
        if (isUseHandsfree) {
            audioRoute = TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER;
        } else {
            audioRoute = TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE;
        }
        mTRTCCloud.setAudioRoute(audioRoute);
    }

    //展示悬浮窗
    @Override
    public void showFloatingWindow() {
        Log.e("aaa ", "showFloatingWindow");
        isVideoPaneOpen = true;
        if (!isAddFloating) {
            Log.e("aaa", "添加成功");
            isAddFloating = true;
            windowManager.addView(displayView, layoutParams);
        }
        displayView.setVisibility(View.VISIBLE);
        if (isOpenCamera && TRRCStarted) {
            mTRTCCloud.stopLocalPreview();
            mTRTCCloud.startLocalPreview(isFrontCamera, myVideoLayout);
            if (TRTCType == 1 && chatType == 0 && remoteIsOpenCamera) {
                llCommon.setVisibility(View.GONE);
                videoLayout.setVisibility(View.VISIBLE);
                mTRTCCloud.startRemoteView(remoteUId, videoLayout);
            }
        }
    }

    //隐藏悬浮窗
    @Override
    public void hintFloatingWindow() {
        isVideoPaneOpen = false;
        displayView.setVisibility(View.GONE);
        videoLayout.setVisibility(View.GONE);
    }

    /**
     * 悬浮窗移动监听
     */
    private long downTime;

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    downTime = System.currentTimeMillis();
                    isClick = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    if (System.currentTimeMillis() - downTime < 150) {
                        isClick = true;
                    } else {
                        isClick = false;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    /**
     * 用于监听TRTC事件
     */
    private TRTCCloudListener mChatRoomTRTCListener = new TRTCCloudListener() {
        @Override
        public void onEnterRoom(long result) {
            if (result > 0) {
                if (TRRCStarted == false) {
                    TRRCStarted = true;
                    baseTimer = System.currentTimeMillis();
                    Message message = mHandler.obtainMessage(1);
                    mHandler.sendMessage(message);
                    if (!joinIds.contains(uID)) {
                        joinIds.add(uID);
                    }
                    if (!chooseIds.contains(uID)) {
                        chooseIds.add(uID);
                    }
                    if (TRRCUtils.getInstanse().getRListener() != null) {
                        TRRCUtils.getInstanse().getRListener().enterTRTCRoomSucceed();
                    }
                    //在小窗下对方接受邀请需要打开本地视频推流
                    if (isVideoPaneOpen && TRTCType == 1 && chatType == 0) {
                        mTRTCCloud.stopLocalPreview();
                        mTRTCCloud.startLocalPreview(isFrontCamera, myVideoLayout);
                        //拉取对方视频流
                        for (Long id : chooseIds) {
                            if (id != uID) {
                                llCommon.setVisibility(View.GONE);
                                videoLayout.setVisibility(View.VISIBLE);
                                mTRTCCloud.startRemoteView(id + "", videoLayout);
                            }
                            return;
                        }
                    }
                }
            }
        }

        @Override
        public void onRemoteUserEnterRoom(String userId) {
            if (TRRCUtils.getInstanse().getRListener() != null) {
                TRRCUtils.getInstanse().getRListener().onUserVideoAvailable(userId, false);
            }
            if (!joinIds.contains(Long.valueOf(userId))) {
                joinIds.add(Long.valueOf(userId));
            }
            if (!chooseIds.contains(Long.valueOf(userId))) {
                chooseIds.add(Long.valueOf(userId));
            }
            if(TRRCUtils.getInstanse().getRListener() != null){
                TRRCUtils.getInstanse().getRListener().onRoomUserChange(chooseIds,joinIds);
            }
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId, int i) {
            if (TRRCUtils.getInstanse().getRListener() != null) {
                TRRCUtils.getInstanse().getRListener().onRemoteUserLeaveRoom(userId);
            }
            chooseIds.remove(Long.valueOf(userId));
            joinIds.remove(Long.valueOf(userId));
            isOpenVideoIds.remove(userId);
            if(TRRCUtils.getInstanse().getRListener() != null){
                TRRCUtils.getInstanse().getRListener().onRoomUserChange(chooseIds,joinIds);
            }
        }

        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.e("aaa", "进房失败: " + errCode + "/" + extraInfo.toString());
            if (TRRCUtils.getInstanse().getRListener() != null) {
                TRRCUtils.getInstanse().getRListener().enterTRTCRoomError();
            }
        }

        //监听静音设置
        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            super.onUserAudioAvailable(userId, available);
            if (TRRCUtils.getInstanse().getRListener() != null) {
                if (userId == null) {
                    userId = uID.toString();
                }
                TRRCUtils.getInstanse().getRListener().onUserAudioAvailable(userId, available);
            }
        }

        //监听说话音量
        @Override
        public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
            if (TRRCUtils.getInstanse().getRListener() != null) {
                TRRCUtils.getInstanse().getRListener().userVolumeChange(userVolumes);
            }
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean isVideoAvailable) {
            if (chatType == 0) {
                remoteIsOpenCamera = isVideoAvailable;
                remoteUId = userId;
            }
            if (joinIds.contains(Long.parseLong(userId))) {
                if (isVideoAvailable) {
                    if (!isOpenVideoIds.contains(userId)) {
                        isOpenVideoIds.add(userId);
                    }
                } else {
                    isOpenVideoIds.remove(userId);
                }
                if (TRRCUtils.getInstanse().getRListener() != null) {
                    TRRCUtils.getInstanse().getRListener().onUserVideoAvailable(userId, isVideoAvailable);
                }
            }
        }

    };

    //计时器
    private class MyHandler extends Handler {
        TextView tv_time;

        public MyHandler(TextView tv_time) {
            this.tv_time = tv_time;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tv_time.setText(getTime());
                    if (TRRCUtils.getInstanse().getRListener() != null) {
                        TRRCUtils.getInstanse().getRListener().timeReturn(getTime());
                    }
                    Message message = this.obtainMessage(1);
                    this.sendMessageDelayed(message, 1000);
                    break;
                case 2:
                    this.removeMessages(1);
                    break;
                case 3:
                    TRRCUtils.getInstanse().getOListener().exitTRTCRoom();
                    break;
                case 4:
                    if (!TRRCStarted || (joinIds.size() == 1 && joinIds.get(0).equals(uID))) {
                        if (TRRCUtils.getInstanse().getRListener() != null) {
                            TRRCUtils.getInstanse().getRListener().onCloseVoiceChat();
                        } else {
                            this.sendEmptyMessage(3);
                        }
                    }
                    break;
                case 5:
                    ToastUtils.showShort(msg.obj.toString());
                    break;
            }
        }

    }

    //求两个数组的差集
    public static ArrayList<Long> substract(ArrayList<Long> arr1, ArrayList<Long> arr2) {
        ArrayList<Long> list = new ArrayList<Long>();
        list.addAll(arr1);
        for (Long str : arr2) {
            if (list.contains(str)) {
                list.remove(str);
            }
        }
        return list;
    }

    //求两个数组的交集
    public static ArrayList<Long> intersect(ArrayList<Long> arr1, ArrayList<Long> arr2) {
        ArrayList<Long> list = new ArrayList<Long>();
        list.addAll(arr1);
        ArrayList<Long> list2 = new ArrayList<Long>();
        for (Long str : arr2) {
            if (list.contains(str)) {
                list2.add(str);
            }
        }
        return list2;
    }

    //获取当前计时时间
    private static String getTime() {
        long time = (System.currentTimeMillis() - baseTimer) / 1000;
        String hh = new DecimalFormat("00").format(time / 3600);
        String mm = new DecimalFormat("00").format(time % 3600 / 60);
        String ss = new DecimalFormat("00").format(time % 60);
        String timeFormat = new String(mm + ":" + ss);
        return timeFormat;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isVideoPaneOpen = false;
        isStarted = false;
        TRRCStarted = false;

        chooseIds.clear();
        joinIds.clear();
        isOpenVideoIds.clear();

        isEnableMic = true;//是否打开麦克风
        isUseHandsfree = true;//是否打开免提
        isOpenCamera = false;//是否打开摄像头
        remoteIsOpenCamera = false;
        isFrontCamera = true;//是否是前置摄像头
        if (mHandler != null) {
            mHandler.sendEmptyMessage(1);
            mHandler.removeCallbacksAndMessages(null);
        }
        TRRCUtils.getInstanse().setOListener(null);
        IMClient.INSTANCE.getChatManager().removeCmdMessageListener(msgListener);
        AudioRecorderPanel.isChatServiceCall = false;

        mTRTCCloud.stopLocalPreview();
        mTRTCCloud.stopAllRemoteView();
        mTRTCCloud.stopAudioRecording();
    }

}
