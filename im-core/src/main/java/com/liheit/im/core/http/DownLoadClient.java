package com.liheit.im.core.http;

import android.support.annotation.NonNull;

import com.liheit.im.core.IMClient;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class DownLoadClient {
    private volatile static DownLoadClient instance;

    private volatile static Retrofit retrofit;

    public static DownLoadClient getInstance() {
        if (instance == null) {
            synchronized (DownLoadClient.class) {
                if (instance == null) {
                    instance = new DownLoadClient();
                    retrofit = new Retrofit.Builder()
                            .client(new OkHttpClient())
                            .baseUrl(IMClient.INSTANCE.getConfig().fileServer + ":" + IMClient.INSTANCE.getConfig().fileServerPort)
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * 下载单文件，该方法不支持断点下载
     *
     * @param url                  文件地址
     * @param destDir              存储文件夹
     * @param fileName             存储文件名
     * @param fileDownLoadObserver 监听回调
     */
    public Observable<File> downloadFile(@NonNull String url,
                                         int type, String fileToken, Long size, String filename,
                                         Boolean source, String account, String sign,
                                         final String destDir,
                                         final String fileName,
                                         final FileDownLoadObserver<File> fileDownLoadObserver) {
        return retrofit.create(Api.class)
                .downloadFile(url, "", type, fileToken, size, filename,
                        source, "", 2, account, sign)
                .subscribeOn(Schedulers.io())//subscribeOn和ObserOn必须在io线程，如果在主线程会出错
                .observeOn(Schedulers.io())
                .observeOn(Schedulers.computation())//需要
                .map(new Function<Response<ResponseBody>, File>() {
                    @Override
                    public File apply(@io.reactivex.annotations.NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        return fileDownLoadObserver.saveFile(responseBodyResponse, destDir, fileName);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 下载单文件，该方法不支持断点下载
     *
     * @param url                  文件地址
     * @param destDir              存储文件夹
     * @param fileName             存储文件名
     * @param fileDownLoadObserver 监听回调
     */
    public void downloadFile2(@NonNull String url,
                              int type, String fileToken, Long size, String filename,
                              Boolean source, String account, String sign,
                              final String destDir,
                              final String fileName,
                              final FileDownLoadObserver<File> fileDownLoadObserver) {
        retrofit.create(Api.class)
                .downloadFile(url, "", type, fileToken, size, filename,
                        source, "", 2, account, sign)
                .subscribeOn(Schedulers.io())//subscribeOn和ObserOn必须在io线程，如果在主线程会出错
                .observeOn(Schedulers.io())
                .observeOn(Schedulers.computation())//需要
                .map(new Function<Response<ResponseBody>, File>() {
                    @Override
                    public File apply(@io.reactivex.annotations.NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        return fileDownLoadObserver.saveFile(responseBodyResponse, destDir, fileName);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileDownLoadObserver);
    }

    /**
     * 下载单文件，该方法不支持断点下载
     *
     * @param url                  文件地址
     * @param destDir              存储文件夹
     * @param fileName             存储文件名
     * @param fileDownLoadObserver 监听回调
     */
    public void downloadFileByUrl(@NonNull String url, final String destDir,
                                  final String fileName, final FileDownLoadObserver<File> fileDownLoadObserver) {
        retrofit.create(Api.class)
                .downloadFileByUrl(url)
                .subscribeOn(Schedulers.io())//subscribeOn和ObserOn必须在io线程，如果在主线程会出错
                .observeOn(Schedulers.io())
                .observeOn(Schedulers.computation())//需要
                .map(new Function<Response<ResponseBody>, File>() {
                    @Override
                    public File apply(@io.reactivex.annotations.NonNull Response<ResponseBody> responseBodyResponse) throws Exception {
                        return fileDownLoadObserver.saveFile(responseBodyResponse, destDir, fileName);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileDownLoadObserver);
    }
}
