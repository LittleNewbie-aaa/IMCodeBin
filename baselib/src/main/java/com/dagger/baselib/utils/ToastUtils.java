package com.dagger.baselib.utils;

import android.content.Context;

/**
 * Created by daixun on 17-3-25.
 */

public class ToastUtils {
    private static Context mContext;

    public static void init(Context context) {
        mContext = context.getApplicationContext();
    }

    public static void showToast(String msg) {
        com.blankj.utilcode.util.ToastUtils.showShort(msg);
    }
    public static void showLongToast(String msg) {
        com.blankj.utilcode.util.ToastUtils.showLong(msg);
    }


    public static void showToast(int msg) {
        com.blankj.utilcode.util.ToastUtils.showShort(msg);
    }
}
