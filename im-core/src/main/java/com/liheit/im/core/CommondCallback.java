package com.liheit.im.core;

/**
 * Created by daixun on 2018/9/8.
 */

public interface CommondCallback {

    void onSuccess(int packageType, int cmd, int sendNumber, String data);

    void onProcess();

    void onError(long code, String errorMsg);
}
