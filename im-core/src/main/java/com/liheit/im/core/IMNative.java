package com.liheit.im.core;

import android.support.annotation.Keep;

/**
 * Created by daixun on 2018/6/14.
 */

class IMNative {

    static {
        System.loadLibrary("native-lib");
    }

    private IMNative() {

    }

    public static native void test();

    public static native int access(String ip, int port, String accessPackage, ImCallback callback);

    public static native void setToken(String token);

    //public native boolean login(String ip, int port, MsgCallback callback);
    public static native long start(String loginReq,String ip, int port, Callback callback);

    public static native void stop();

    public static native boolean isConnected();

    public static native boolean sendPackage(int packageType, int packageIndex, int command, String jsonPackage);

    @Keep
    public interface ImCallback {
        void callback(String data);
    }

    @Keep
    public interface Callback {

        /**
         * @param data        消息体
         * @param packageType 消息类型
         * @param sendNumber
         * @param cmd
         */
        void onMessage(boolean isConnect, String data, int packageType, int sendNumber, int cmd, byte[] rawData);
    }
//    public static native boolean sendPackage();
}
