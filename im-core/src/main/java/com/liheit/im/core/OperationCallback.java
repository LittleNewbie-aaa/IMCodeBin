package com.liheit.im.core;

/**
 * Created by daixun on 2018/7/15.
 */

public interface OperationCallback {

    void onSuccess();

    void onError(long code, String errorMsg);
}
