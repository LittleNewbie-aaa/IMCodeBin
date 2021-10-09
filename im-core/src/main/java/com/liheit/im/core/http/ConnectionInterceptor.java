package com.liheit.im.core.http;

import com.liheit.im.utils.NetworkUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by daixun on 17-3-4.
 */

public class ConnectionInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (NetworkUtils.isConnected()) {
            Request request = chain.request();
            return chain.proceed(request);
        } else {
            throw new NotConnectedException("网络未连接");
        }
    }

    public static class NotConnectedException extends RuntimeException {
        public NotConnectedException(String message) {
            super(message);
        }
    }
}
