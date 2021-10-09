package com.im.trtc_call;

/**
 * TRTC 语音/视频通话接口
 */
public class TRRCUtils {

    private static TRRCUtils TRRCUtils = null;
    private TRTCOperationListener oListener = null;
    private TRTCReturnListener rListener = null;

    public synchronized static TRRCUtils getInstanse() {
        if (TRRCUtils == null) {
            synchronized (TRRCUtils.class) {
                if (TRRCUtils == null) {
                    TRRCUtils = new TRRCUtils();
                }
            }
        }
        return TRRCUtils;
    }

    public void setOListener(TRTCOperationListener listener) {
        this.oListener = listener;
    }

    public TRTCOperationListener getOListener() {
        return oListener;
    }

    public void setRListener(TRTCReturnListener listener) {
        this.rListener = listener;
    }

    public TRTCReturnListener getRListener() {
        return rListener;
    }

}
