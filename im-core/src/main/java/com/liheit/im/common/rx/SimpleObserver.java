package com.liheit.im.common.rx;

import com.blankj.utilcode.util.NetworkUtils;
import com.liheit.im.core.IMException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.liheit.im.core.http.ApiException;
import com.liheit.im.core.http.ConnectionInterceptor;
import com.tencent.bugly.crashreport.CrashReport;
import com.dagger.baselib.utils.ToastUtils;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import retrofit2.HttpException;

/**
 * Created by daixun on 17-3-4.
 */

public class SimpleObserver<T> implements Observer<T> {
    public static final Gson mGson = new GsonBuilder().create();
    Disposable disposable;

    public Disposable getDisposable() {
        return disposable;
    }

    @Override
    public void onSubscribe(Disposable d) {
        disposable = d;
    }

    @Override
    public void onNext(T data) {

    }

    @Override
    public void onError(Throwable e) {
        if (!NetworkUtils.isConnected()) {
            onError("当前网络状况不佳");
        } else if (e instanceof ApiException) {
            onError(((ApiException) e).getMsg());
        } else if (e instanceof IMException) {
            onError(((IMException) e).getMessage());
        } else if (e instanceof HttpException) {
            HttpException httpException = ((HttpException) e);
            onError(httpException.message());
        } else if (e instanceof ConnectionInterceptor.NotConnectedException) {
            onError("网络已断开");
            return;
        } else if (e instanceof SocketTimeoutException || e instanceof UnknownHostException) {
            onError("当前网络状况不佳");
        } else if (e instanceof JsonSyntaxException) {
            onError("服务器错误");
        } else {
            //onError("网络错误");
        }
        CrashReport.postCatchedException(e);
        e.printStackTrace();
    }


    public void onError(String msg) {
        ToastUtils.showToast(msg);
    }

    @Override
    public void onComplete() {
    }
}
