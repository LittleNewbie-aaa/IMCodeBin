package com.liheit.im.core.http;

import android.os.SystemClock;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class ProgressRequestBody extends RequestBody {

    protected final RequestBody mDelegate;
    protected final ProgressListener mListener;
    protected int mRefreshTime = 0;
    private BufferedSink mBufferedSink = null;


    public ProgressRequestBody(RequestBody delegate, ProgressListener listener) {
        this.mDelegate = delegate;
        mListener = listener;
    }

    @Override
    public MediaType contentType() {
        return mDelegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mDelegate.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (sink instanceof Buffer){
            //因为项目重写了日志拦截器，而日志拦截器里面调用了 RequestBody.writeTo方法，但是 它的sink类型是Buffer类型，所以直接写入
            //如果不这么做的话，上传进度最终会达到200%，因为被调用2次，而且日志拦截的writeTo是直接写入到 buffer 对象中，所以会很快；
            mDelegate.writeTo(sink);
            return;
        }
        if (mBufferedSink == null) {
            mBufferedSink = Okio.buffer(new CountingSink(sink));
        }
        mDelegate.writeTo(mBufferedSink);
        mBufferedSink.flush();
    }

    public interface ProgressListener {
        void onProgress(long total, long transferSize);
    }

    protected final class CountingSink extends ForwardingSink {
        private long totalBytesRead = 0L;
        private long lastRefreshTime = 0L;  //最后一次刷新的时间
        private long tempSize = 0L;
        private long contentLength = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);

            if (contentLength == 0) { //避免重复调用 contentLength()
                contentLength = contentLength();
            }
            totalBytesRead += byteCount;
            tempSize += byteCount;
            if (mListener != null) {
                long curTime = SystemClock.elapsedRealtime();
                if (curTime - lastRefreshTime >= mRefreshTime || totalBytesRead == contentLength) {
                    mListener.onProgress(contentLength, totalBytesRead);
                    lastRefreshTime = curTime;
                    tempSize = 0;
                }
            }
        }
    }
}
