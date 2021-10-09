package com.liheit.im.core.http;

import com.liheit.im.utils.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.observers.DefaultObserver;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * 下载文件回调监听
 * @param <T>
 */
public abstract class FileDownLoadObserver<T> extends DefaultObserver<T> {

    @Override
    public void onNext(T t) {
        onDownLoadSuccess(t);
    }

    @Override
    public void onError(Throwable e) {
        Log.e("aaa e="+e.getMessage());
        onDownLoadFail(e);
    }

    //可以重写，具体可由子类实现
    @Override
    public void onComplete() {
    }

    //下载成功的回调
    public abstract void onDownLoadSuccess(T t);

    //下载失败回调
    public abstract void onDownLoadFail(Throwable throwable);

    //下载进度监听
    public abstract void onProgress(int progress, long total);

    /**
     * 将文件写入本地
     *
     * @param response 请求结果全体
     * @param destFileDir  目标文件夹
     * @param destFileName 目标文件名
     * @return 写入完成的文件
     * @throws IOException IO异常
     */
    public File saveFile(Response<ResponseBody> response, String destFileDir, String destFileName) throws IOException {
//        Log.e("aaa "+response.code()+"  ///"+response.message());
        if(response.code()!= 200){
            if (response.code() == 404) {
                onDownLoadFail(new Throwable("没有找到对应资源(10002)"));
            }else {
                onDownLoadFail(new Throwable("下载失败"+response.code()));
            }
            return null;
        }
        ResponseBody responseBody=response.body();
        Log.e("aaa destFileDir=" + destFileDir + "////" + destFileName);
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            is = responseBody.byteStream();
            final long total = responseBody.contentLength();
            long sum = 0;
            File dir = new File(destFileDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, destFileName);
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1) {
                sum += len;
                fos.write(buf, 0, len);
                final long finalSum = sum;
                Log.e("aaa  finalSum="+finalSum+"  total="+total );
                //这里就是对进度的监听回调
                onProgress((int) (finalSum * 100 / total), total);
            }
            fos.flush();
            return file;
        } finally {
            try {
                if (is != null) is.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
