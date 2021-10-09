package com.liheit.im.core;

import org.jetbrains.annotations.NotNull;

/**
 * Created by daixun on 2018/7/15.
 */

public interface IMRespCallback {

    void onSuccess(@NotNull CommondResp data);

    void onProcess(@NotNull int state, @NotNull int progress);

    void onError(@NotNull IMException e);
}

