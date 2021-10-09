package com.liheit.im.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by daixun on 2018/8/13.
 */

public class AppUtils {

    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }
}
