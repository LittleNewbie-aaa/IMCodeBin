package com.daixun.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.liheit.im.utils.Log;
import com.liheit.im_core.R;

import java.io.File;

public abstract class AudioRecorderPanel implements View.OnTouchListener {
    public static boolean isChatServiceCall =false;
    private int maxDuration = 60 * 1000;
    private int minDuration = 1 * 1000;
    private int countDown = 10 * 1000;
    private boolean isRecording;
    private long startTime;
    private boolean isToCancel;
    private String currentAudioFile;

    private Context context;
    private View rootView;
    private View button;
    private AudioRecorder recorder;
    private OnRecordListener recordListener;
    private Handler handler;

    private TextView countDownTextView;
    private TextView stateTextView;
    private ImageView stateImageView;
    private PopupWindow recordingWindow;

    private boolean isCountDownTime = false;

    public AudioRecorderPanel(Context context) {
        this.context = context;
    }

    /**
     * @param maxDuration 最长录音时间，单位：秒
     */
    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration * 1000;
    }

    /**
     * @param minDuration 最短录音时间，单位：秒
     */
    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration * 1000;
    }

    /**
     * @param countDown 录音剩余多少秒时开始倒计时，单位：秒
     */
    public void setCountDown(int countDown) {
        this.countDown = countDown * 1000;
    }

    /**
     * 将{@link AudioRecorderPanel}附加到button上面
     *
     * @param rootView 录音界面显示的rootView
     * @param button   长按触发录音的按钮
     */
    public void attach(View rootView, View button) {
        this.rootView = rootView;
        this.button = button;
        this.button.setOnTouchListener(this);
    }


    public void setRecordListener(OnRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(isChatServiceCall){
            Toast.makeText(context, "麦克风已被占用", Toast.LENGTH_SHORT).show();
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("aaa MotionEvent.ACTION_DOWN");
                startRecord();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("aaa MotionEvent.ACTION_MOVE");
                if (isRecording) {
                    if(recordingWindow == null || !recordingWindow.isShowing()){
                        showRecording();
                    }
                    isToCancel = isCancelled(v, event);
                    if (isToCancel) {
                        if (recordListener != null) {
                            recordListener.onRecordStateChanged(RecordState.TO_CANCEL);
                        }
                        showCancelTip();
                    } else {
                        hideCancelTip();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.e("aaa MotionEvent.ACTION_UP");
                if (isRecording) {
                    if (isToCancel) {
                        cancelRecord();
                    } else if (isRecording) {
                        stopRecord();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void startRecord() {
        // FIXME: 2018/10/10 权限是否有权限，没权限提示错误，并返回
        if (grantPermission()) {
            isRecording = true;
            if (recorder == null) {
                recorder = new AudioRecorder(context);
                handler = new Handler();
            }
            currentAudioFile = genAudioFile();
            recorder.startRecord(currentAudioFile);
            if (recordListener != null) {
                recordListener.onRecordStateChanged(RecordState.START);
            }

            startTime = System.currentTimeMillis();
            showRecording();
            tick();
            updateVolume();
        } else {
            requestPermission();
        }
    }

    public abstract void requestPermission();

    public abstract boolean grantPermission();

    private void stopRecord() {
        if (!isRecording) {
            return;
        }
        if (recorder != null) {
            recorder.stopRecord();
        }
        if (recordListener != null) {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > minDuration) {
                recordListener.onRecordSuccess(currentAudioFile, (int) duration / 1000);
                hideRecording();
            } else {
                recordListener.onRecordFail("too short");
                showTooShortTip();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!isRecording){
                            hideRecording();
                        }
                    }
                }, 1000);
            }
        } else {
            hideRecording();
        }

        isToCancel = false;
        isRecording = false;
        isCountDownTime = false;
    }

    private void cancelRecord() {
        if (recorder != null) {
            recorder.stopRecord();
        }
        if (recordListener != null) {
            recordListener.onRecordFail("user canceled");
        }
        hideRecording();

        isToCancel = false;
        isRecording = false;
        isCountDownTime = false;
    }

    private void showRecording() {
        if (recordingWindow == null) {
            View view = View.inflate(context, R.layout.im_record_audio_popup_wi_vo, null);
            stateImageView = view.findViewById(R.id.rc_audio_state_image);
            stateTextView = view.findViewById(R.id.rc_audio_state_text);
            countDownTextView = view.findViewById(R.id.rc_audio_timer);
            recordingWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            recordingWindow.setFocusable(true);
            recordingWindow.setOutsideTouchable(false);
            recordingWindow.setTouchable(false);
        }

        recordingWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        stateImageView.setImageResource(R.drawable.ic_volume_1);
        stateImageView.setVisibility(View.VISIBLE);
        stateTextView.setText(R.string.im_record_voice_rec);
        stateTextView.setBackgroundResource(R.drawable.im_record_bg_voice_record);
        countDownTextView.setVisibility(View.GONE);
    }

    private void hideRecording() {
        if (recordingWindow != null) {
            recordingWindow.dismiss();
            recordingWindow = null;
        }
    }

    private void showTooShortTip() {
        stateImageView.setImageResource(R.drawable.ic_volume_wraning);
        stateTextView.setText(R.string.im_record_voice_short);
    }

    private void showCancelTip() {
        if (isCountDownTime) {
            countDownTextView.setVisibility(View.VISIBLE);
            stateImageView.setVisibility(View.GONE);
        } else {
            countDownTextView.setVisibility(View.GONE);
            stateImageView.setVisibility(View.VISIBLE);
            stateImageView.setImageResource(R.drawable.ic_volume_cancel);
        }
        stateTextView.setText(R.string.im_record_voice_cancel);
        stateTextView.setBackgroundResource(R.drawable.im_record_corner_voice_style);
    }

    private void hideCancelTip() {
        if (isCountDownTime) {
            countDownTextView.setVisibility(View.VISIBLE);
            stateImageView.setVisibility(View.GONE);
        } else {
            countDownTextView.setVisibility(View.GONE);
            stateImageView.setVisibility(View.VISIBLE);
            stateImageView.setImageResource(R.drawable.ic_volume_1);
        }
        stateTextView.setText(R.string.im_record_voice_rec);
        stateTextView.setBackgroundResource(R.drawable.im_record_bg_voice_record);
    }

    /**
     * @param seconds
     */
    private void showCountDown(int seconds) {
        stateImageView.setVisibility(View.GONE);
        countDownTextView.setText(String.format("%s", seconds));
        countDownTextView.setVisibility(View.VISIBLE);
    }

    private void tick() {
        if (isRecording) {
            long now = System.currentTimeMillis();
            if (now - startTime > maxDuration) {
                stopRecord();
            } else if (now - startTime > (maxDuration - countDown)) {
                int tmp = (int) ((maxDuration - (now - startTime)) / 1000);
                tmp = tmp > 1 ? tmp : 1;
                showCountDown(tmp);
                if (recordListener != null) {
                    recordListener.onRecordStateChanged(RecordState.TO_TIMEOUT);
                }
                if (tmp <= countDown) {
                    isCountDownTime = true;
                } else {
                    isCountDownTime = false;
                }
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            }, 1000);
        }
    }

    private void updateVolume() {
        if (!isRecording || isToCancel) {
            return;
        }
        // refer to https://www.cnblogs.com/lqminn/archive/2012/10/10/2717904.html
        int voiceValue = recorder.getMaxAmplitude();

        if (voiceValue < 600.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_1);
        } else if (voiceValue > 600.0 && voiceValue < 1500.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_2);
        } else if (voiceValue > 1500.0 && voiceValue < 2500.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_3);
        } else if (voiceValue > 2500.0 && voiceValue < 4500.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_4);
        } else if (voiceValue > 4500.0 && voiceValue < 7500.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_5);
        } else if (voiceValue > 7500.0 && voiceValue < 9500.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_6);
        } else if (voiceValue > 9500.0 && voiceValue < 12000.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_7);
        } else if (voiceValue > 12000.0) {
            stateImageView.setImageResource(R.drawable.ic_volume_8);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateVolume();
            }
        }, 200);
    }

    private String genAudioFile() {
        File dir = new File(context.getFilesDir(), "audio");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir, System.currentTimeMillis() + "");
        return file.getAbsolutePath();
    }

    private boolean isCancelled(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (event.getRawX() < location[0] || event.getRawX() > location[0] + view.getWidth()
                || event.getRawY() < location[1] - 40) {
            return true;
        }

        return false;
    }

    public enum RecordState {
        // 开始录音
        START,
        // 录音中
        RECORDING,
        // 用户准备取消
        TO_CANCEL,
        // 最长录音时间开到
        TO_TIMEOUT,
    }

    public interface OnRecordListener {
        void onRecordSuccess(String audioFile, int duration);

        void onRecordFail(String reason);

        void onRecordStateChanged(RecordState state);
    }

    /**
     * 麦克风是否被占用
     * @return
     */
    private boolean validateMicAvailability(){
        Boolean available = true;
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try{
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
                available = false;

            }

            recorder.startRecording();
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
                recorder.stop();
                available = false;

            }
            recorder.stop();
        } finally{
            recorder.release();
            recorder = null;
        }

        return available;
    }
}