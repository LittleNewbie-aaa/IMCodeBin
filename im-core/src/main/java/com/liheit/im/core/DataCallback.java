package com.liheit.im.core;

/**
 * Created by daixun on 2018/7/15.
 */

public interface DataCallback<T> {

    void onSuccess(T data);

    void onError(long code, String errorMsg);
}
