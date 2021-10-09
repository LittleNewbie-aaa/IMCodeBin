package com.liheit.im.core.protocol;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 返回数据类型
 */
public class PackageType {
    public static final int PACKAGE_TYPE_GSON = 0; //  0 －表示JSON字符串，以’\0’结尾；
    public static final int PACKAGE_TYPE_URL = 1; //  1 －表示url方式下载的JSON字符串，以’\0’结尾；
    public static final int PACKAGE_TYPE_ZIP = 2;//  2 －表示内容为ZIP压缩数据。


    @IntDef({PACKAGE_TYPE_GSON, PACKAGE_TYPE_URL, PACKAGE_TYPE_ZIP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {

    }
}
