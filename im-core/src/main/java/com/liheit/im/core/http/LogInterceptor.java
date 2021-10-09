package com.liheit.im.core.http;

import com.liheit.im.utils.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;

import static okhttp3.internal.platform.Platform.INFO;

/**
 * Created by daixun on 17-3-4.
 */

public class LogInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Logger logger;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonParser jsonParser = new JsonParser();
    private volatile Level level = Level.NONE;

    public LogInterceptor() {
        this(Logger.DEFAULT);
    }

    public LogInterceptor(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    public Level getLevel() {
        return level;
    }

    /**
     * Change the level at which this interceptor logs.
     */
    public LogInterceptor setLevel(Level level) {
        if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
        this.level = level;
        return this;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        StringBuilder sb = new StringBuilder();
        long begin = System.currentTimeMillis();


        Level level = this.level;

        Request request = chain.request();
        if (level == Level.NONE) {
            return chain.proceed(request);
        }

        boolean logBody = level == Level.BODY;
        boolean logHeaders = logBody || level == Level.HEADERS;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }
//        String method = request.url().queryParameter("method");
//        if (method != null) {
//            sb.append(ApiKt.getUrlMapping().get(method));
//        }
//        logger.log(requestStartMessage);
        sb.append(requestStartMessage).append("\n");
        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null) {
//                    logger.log("Content-Type: " + requestBody.contentType());
                    sb.append("Content-Type: " + requestBody.contentType()).append("\n");
                }
                if (requestBody.contentLength() != -1) {
//                    logger.log("Content-Length: " + requestBody.contentLength());
                    sb.append("Content-Length: " + requestBody.contentLength()).append("\n");
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
//                    logger.log(name + ": " + headers.value(i));
                    sb.append(name + ": " + headers.value(i)).append("\n");
                }
            }

            if (!logBody || !hasRequestBody) {
//                logger.log("--> END " + request.method());
                sb.append("--> END " + request.method()).append("\n");
            } else if (bodyEncoded(request.headers())) {
//                logger.log("--> END " + request.method() + " (encoded body omitted)");
                sb.append("--> END " + request.method() + " (encoded body omitted)").append("\n");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

//                logger.log("");
                sb.append("").append("\n");
                if (isPlaintext(buffer)) {
//                    logger.log(buffer.readString(charset));
                    sb.append(buffer.readString(charset)).append("\n");
//                    logger.log("--> END " + request.method()
//                            + " (" + requestBody.contentLength() + "-byte body)");
                    sb.append("--> END " + request.method()
                            + " (" + requestBody.contentLength() + "-byte body)").append("\n");
                } else {
//                    logger.log("--> END " + request.method() + " (binary "
//                            + requestBody.contentLength() + "-byte body omitted)");
                    sb.append("--> END " + request.method() + " (binary "
                            + requestBody.contentLength() + "-byte body omitted)").append("\n");
                }
            }
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            try {
                response = chain.proceed(request);
            } catch (Exception e) {
                logger.log("<-- HTTP FAILED: " + e);
                sb.append("<-- HTTP FAILED: " + e).append("\n");
                throw e;
            }
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            ResponseBody responseBody = response.body();
            long contentLength = responseBody.contentLength();
            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
//        logger.log("<-- " + response.code() + ' ' + response.message() + ' '
//                + response.request().url() + " (" + tookMs + "ms" + (!logHeaders ? ", "
//                + bodySize + " body" : "") + ')');
            sb.append("<-- " + response.code() + ' ' + response.message() + ' '
                    + response.request().url() + " (" + tookMs + "ms" + (!logHeaders ? ", "
                    + bodySize + " body" : "") + ')').append("\n");

            if (logHeaders) {
                Headers headers = response.headers();
                for (int i = 0, count = headers.size(); i < count; i++) {
//                logger.log(headers.name(i) + ": " + headers.value(i));
                    sb.append(headers.name(i) + ": " + headers.value(i)).append("\n");
                }

                if (!logBody || !HttpHeaders.hasBody(response)) {
//                logger.log("<-- END HTTP");
                    sb.append("<-- END HTTP").append("\n");
                } else if (bodyEncoded(response.headers())) {
//                logger.log("<-- END HTTP (encoded body omitted)");
                    sb.append("<-- END HTTP (encoded body omitted)").append("\n");
                } else {
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.buffer();

                    Charset charset = UTF8;
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        try {
                            charset = contentType.charset(UTF8);
                        } catch (UnsupportedCharsetException e) {
//                        logger.log("");
//                        logger.log("Couldn't decode the response body; charset is likely malformed.");
//                        logger.log("<-- END HTTP");
                            sb.append("").append("\n");
                            sb.append("Couldn't decode the response body; charset is likely malformed.").append("\n");
                            sb.append("<-- END HTTP").append("\n");

                            return response;
                        }
                    }

                    if (!isPlaintext(buffer)) {
//                    logger.log("");
//                    logger.log("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
                        sb.append("").append("\n");
                        sb.append("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)").append("\n");
                        return response;
                    }

                    if (contentLength != 0) {
//                    logger.log("");
//                    logger.log(buffer.clone().readString(charset));

                        sb.append("").append("\n");
                        String respString = buffer.clone().readString(charset);
                        try {
                            String jsonStr = gson.toJson(jsonParser.parse(respString));
                            sb.append(jsonStr).append("\n");
                        } catch (Exception e) {
                            //sb.append(respString);
                        }
                    }

//                logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
                    sb.append("<-- END HTTP (" + buffer.size() + "-byte body)").append("\n");
                }
            }
        } finally {
            long endTime = System.currentTimeMillis();
            sb.append("接口耗时 " + (endTime - begin) + "毫秒");
            Log.d(sb.toString());
        }


        return response;
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers.
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * }</pre>
         */
        HEADERS,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    public interface Logger {
        /**
         * A {@link HttpLoggingInterceptor.Logger} defaults output appropriate for the current platform.
         */
        Logger DEFAULT = new Logger() {
            @Override
            public void log(String message) {
                Platform.get().log(INFO, message, null);
            }
        };

        void log(String message);
    }
}
