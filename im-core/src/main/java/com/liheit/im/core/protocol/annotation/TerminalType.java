package com.liheit.im.core.protocol.annotation;

import android.support.annotation.IntDef;

import com.liheit.im.core.protocol.Terminal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by daixun on 2018/6/21.
 */
@IntDef({Terminal.ANDROID, Terminal.IOS, Terminal.MAC, Terminal.WINDOWS})
@Retention(RetentionPolicy.SOURCE)
public @interface TerminalType {
}
