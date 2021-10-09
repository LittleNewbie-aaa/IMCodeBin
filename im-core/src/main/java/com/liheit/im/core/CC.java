package com.liheit.im.core;

/**
 * Created by daixun on 2018/8/17.
 */

public interface CC {

    /**
     * @param data        消息体
     * @param packageType 消息类型
     * @param sendNumber
     * @param cmd
     */
    void onMessage(boolean isConnect, String data, int packageType, int sendNumber, int cmd, byte[] rawData);
}
