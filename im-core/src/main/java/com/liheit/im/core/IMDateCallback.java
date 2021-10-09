package com.liheit.im.core;

/**
 * Created by daixun on 2018/7/13.
 */

public interface IMDateCallback<T> {

    void onSuccess(T data);

    void onProgress(int progress);

    void onError(long errorCode, String errorMessage);
}
