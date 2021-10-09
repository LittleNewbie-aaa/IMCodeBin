package com.liheit.im.core;

import com.liheit.im_core.BuildConfig;

/**
 * Created by daixun on 2018/7/2.
 * 对im服务器的url配置
 */

public class IMConfig {
    public static final int terminal = 2;

    public String host = BuildConfig.HOST;
    public int port = BuildConfig.PORT;
    public String fileServer = BuildConfig.fileServer;
    public int fileServerPort = BuildConfig.fileServerPort;
    public String restServer = BuildConfig.restServer;
    public int restServerPort = BuildConfig.restServerPort;
    public String contactsServer = BuildConfig.contactsServer;
    public int contactsServerPort = BuildConfig.contactsServerPort;
    public String officialAccountsServer = BuildConfig.OfficialAccountsServer;
    public int officialAccountsServerPort = BuildConfig.OfficialAccountsServerPort;
}
