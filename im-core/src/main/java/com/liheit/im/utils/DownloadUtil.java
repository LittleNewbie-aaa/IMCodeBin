package com.liheit.im.utils;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by daixun on 2018/6/18.
 */

public class DownloadUtil {

    private static DownloadUtil downloadUtil;
    private final OkHttpClient okHttpClient;

    private DownloadUtil() {
        okHttpClient = new SSLSupport().addSupport(new OkHttpClient.Builder()).build();
//        okHttpClient =  new  OkHttpClient.Builder().build();
    }


    public static DownloadUtil get() {
        if (downloadUtil == null) {
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    /**
     * @param url      下载连接
     * @param saveDir  储存下载文件的SDCard目录
     * @param listener 下载监听
     */
    public void download(final String url, final String saveDir, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {

            File downloadFile;

            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                String savePath = isExistDir(saveDir);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    downloadFile = new File(savePath, getNameFromUrl(url));
                    if (!downloadFile.exists()) {
                        downloadFile.createNewFile();
                    }
                    fos = new FileOutputStream(downloadFile);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess(downloadFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    /**
     * @param saveDir
     * @return
     * @throws IOException 判断下载目录是否存在
     */
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(saveDir);
        if (!downloadFile.exists()) {
            downloadFile.mkdirs();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    /**
     * @param url
     * @return 从下载连接中解析出文件名
     */
    @NonNull
    private String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public Observable<Pair<File, Integer>> download(final String url, final String saveFile) {

        return Observable.create(new ObservableOnSubscribe<Pair<File, Integer>>() {
            @Override
            public void subscribe(final ObservableEmitter<Pair<File, Integer>> emitter) throws Exception {
                Request request = new Request.Builder().url(url).get().build();
                okHttpClient.newCall(request).enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        // 下载失败
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len = 0;
                        FileOutputStream fos = null;

                        File downloadFile = new File(saveFile);

                        if (!downloadFile.getParentFile().exists()) {
                            downloadFile.getParentFile().mkdirs();
                        }

                        try {
                            is = response.body().byteStream();
                            long total = response.body().contentLength();

                            if (!downloadFile.exists()) {
                                downloadFile.createNewFile();
                            }
                            fos = new FileOutputStream(downloadFile);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                // 下载中

                                if (!emitter.isDisposed()) {
                                    emitter.onNext(new Pair<File, Integer>(downloadFile, progress));
                                }
                            }
                            fos.flush();
                            // 下载完成
                            if (!emitter.isDisposed()) {
                                emitter.onNext(new Pair<File, Integer>(downloadFile, 100));
                                emitter.onComplete();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (!emitter.isDisposed()) {
                                emitter.onError(e);
                            }
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        });
    }


    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(File f);

        /**
         * @param progress 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }

}
