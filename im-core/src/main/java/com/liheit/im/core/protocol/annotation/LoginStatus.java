package com.liheit.im.core.protocol.annotation;

import android.support.annotation.IntDef;

import com.liheit.im.core.protocol.LogoutReq;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by daixun on 2018/6/21.
 */
@IntDef({LogoutReq.DISABLED, LogoutReq.KICKED, LogoutReq.LEAVE, LogoutReq.LOGIN, LogoutReq.OFFLINE, LogoutReq.ONLINE})
@Retention(RetentionPolicy.SOURCE)
public @interface LoginStatus {
}
