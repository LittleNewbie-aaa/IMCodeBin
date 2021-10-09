package com.liheit.im.core;

import android.support.annotation.Keep;

/**
 * Created by daixun on 2018/7/2.
 */
@Keep
public interface IMCallBack {

    void onSuccess();

    void onError(Long errorCode, String errorMessage);
}
