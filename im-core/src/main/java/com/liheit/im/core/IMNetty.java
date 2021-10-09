package com.liheit.im.core;

import android.support.annotation.Keep;

import com.google.gson.Gson;
import com.liheit.im.core.protocol.AccessResp;
import com.liheit.im.core.protocol.Pkg;
import com.liheit.im.utils.Log;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Socket 网络连接
 */

public class IMNetty {

    Channel channel;
    IMNative.Callback gloabMsgCallback;
    Callback msgCallback;
    private int TIMEOUT_MILLISECONDS = 5 * 60 * 1000;
    private Map<Integer, IMRespCallback> callback = Collections.synchronizedMap(new HashMap<Integer, IMRespCallback>());
    private EventLoopGroup group;
    public static boolean isDebug = false;

    private Subject<Boolean> connectSubject = BehaviorSubject.createDefault(false);


    private IMNetty() {
        if (!isDebug) {
            InternalLoggerFactory.setDefaultFactory(new InternalLoggerFactory() {
                @Override
                protected InternalLogger newInstance(String name) {
                    return new AndroidLog();
                }
            });
        }
    }

    public static IMNetty instance() {
        return InstanceHolder.instance;
    }

    public Observable<Boolean> connectState() {
        return connectSubject.hide();
    }

    public void setMessageHandler(IMNative.Callback handler) {
        gloabMsgCallback = handler;
    }

    private void onMessage(CommondResp resp) {
        if (msgCallback == null) {
            System.out.println("丢弃消息 " + resp.toString());
        } else {
            msgCallback.onMessage(resp);
        }
    }

    public boolean connect(final String ip, final int port) {
        try {
            final SslContext sslCtx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

            group = new NioEventLoopGroup(2);
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast("ping", new IdleStateHandler(TIMEOUT_MILLISECONDS,
//                                    TIMEOUT_MILLISECONDS, TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS));
                            pipeline.addLast(sslCtx.newHandler(ch.alloc(), ip, port));
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Pkg.MAX_PACKAGE_LENGHT, 6, 2, 2, 0, true));
                            pipeline.addLast(new ProtocolDecoder());
                            pipeline.addLast(new ProtocolEncode());
                            pipeline.addLast(new SimpleChannelInboundHandler<Pkg>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Pkg msg) throws Exception {
                                    if (gloabMsgCallback != null) {
                                        gloabMsgCallback.onMessage(ctx.channel().isOpen(), new String(msg.getBody()), msg.getType(), msg.getSn(), msg.getCmd(), msg.getBody());
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    super.exceptionCaught(ctx, cause);
                                    cause.printStackTrace();
                                    if (gloabMsgCallback != null) {
                                        gloabMsgCallback.onMessage(false, new String(), 0, 0, 0, new byte[0]);
                                    }
                                    /*if (group != null) {
                                        group.shutdownGracefully();
                                    }*/
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelActive(ctx);
                                    connectSubject.onNext(true);
                                    System.out.println("onConnect");
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelInactive(ctx);
                                    /*if (!ctx.channel().isOpen()) {
                                        if (gloabMsgCallback != null) {
                                            gloabMsgCallback.onMessage(false, "", 0, 0, 0, new byte[0]);
                                        }
                                    }*/
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    super.userEventTriggered(ctx, evt);

                                    if (evt instanceof IdleStateEvent) {

                                        IdleStateEvent event = (IdleStateEvent) evt;

                                        if (event.state().equals(IdleState.READER_IDLE)) {
                                            //未进行读操作
                                            System.out.println("READER_IDLE");
                                            // 超时关闭channel
                                            ctx.close();

                                        } else if (event.state().equals(IdleState.WRITER_IDLE)) {


                                        } else if (event.state().equals(IdleState.ALL_IDLE)) {
                                            //未进行读写
                                            System.out.println("ALL_IDLE");
                                            // 发送心跳消息
//                                            MsgHandleService.getInstance().sendMsgUtil.sendHeartMessage(ctx);

                                        }

                                    }
                                }
                            });
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            super.exceptionCaught(ctx, cause);
                            cause.printStackTrace();
                        }
                    });
            // Start the connection attempt.
            channel = b.connect(ip, port).sync().channel();
            channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        connectSubject.onNext(false);
                        System.out.println("onClose");
                    }
                }
            });
            System.out.println("连接成功");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 第一次连接检查登陆账号密码
     * @param ip
     * @param port
     * @param accessPackage
     * @return 是否需要升级，
     */
    public Single<AccessResp> access(final String ip, final int port, final String accessPackage) {

        return Single.create(new SingleOnSubscribe<AccessResp>() {
            @Override
            public void subscribe(final SingleEmitter<AccessResp> emt) throws Exception {
                try {

                    final SslContext sslCtx = SslContextBuilder.forClient()
                            .sslProvider(SslProvider.JDK)
                            .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

                    final EventLoopGroup group1 = new NioEventLoopGroup(1);

                    Bootstrap b = new Bootstrap();

                    b.group(group1)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer() {
                                @Override
                                protected void initChannel(Channel ch) throws Exception {
                                    ChannelPipeline pipeline = ch.pipeline();
                                    pipeline.addLast(sslCtx.newHandler(ch.alloc(), ip, port));
                                    pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Pkg.MAX_PACKAGE_LENGHT, 6, 2, 2, 0, true));
                                    pipeline.addLast(new ProtocolDecoder());
                                    pipeline.addLast(new ProtocolEncode());
                                    pipeline.addLast(new SimpleChannelInboundHandler<Pkg>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Pkg msg) throws Exception {
                                            emt.onSuccess(new Gson().fromJson(new String(msg.getBody()), AccessResp.class));
                                            group1.shutdownGracefully();

                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            super.exceptionCaught(ctx, cause);
                                            cause.printStackTrace();

                                            emt.onError(cause);
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            super.channelActive(ctx);
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                            super.channelInactive(ctx);
                                        }
                                    });
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    super.exceptionCaught(ctx, cause);
                                    cause.printStackTrace();
                                }
                            });
                    // Start the connection attempt.
                    Channel ch = b.connect(ip, port).sync().channel();

                    Pkg pkg = Pkg.createPkg(0, Cmd.ImpAccessReq, Pkg.PACKETE_TYPE_GSON, accessPackage.getBytes());
                    ChannelFuture cf = ch.writeAndFlush(pkg);
                    cf.addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if (!future.isSuccess()) {

                                group1.shutdownGracefully();

                                emt.onError(future.cause());

                            } else {

                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    emt.onError(e);
                }
            }
        });
    }

//    public long start(String loginReq, final String ip, final int port, final IMNative.Callback callback) {
//        //start method contact connect and login operation
//        InternalLoggerFactory.setDefaultFactory(new InternalLoggerFactory() {
//            @Override
//            protected InternalLogger newInstance(String name) {
//                return new AndroidLog();
//            }
//        });
//        stop();
//        try {
//            final SslContext sslCtx = SslContextBuilder.forClient()
//                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//
//            group = new NioEventLoopGroup(2);
//            Bootstrap b = new Bootstrap();
//            b.group(group)
//                    .option(ChannelOption.TCP_NODELAY, true)
//                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30)
//                    .channel(NioSocketChannel.class)
//                    .handler(new ChannelInitializer() {
//                        @Override
//                        protected void initChannel(Channel ch) throws Exception {
//                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(sslCtx.newHandler(ch.alloc(), ip, port));
//                            pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Pkg.MAX_PACKAGE_LENGHT, 6, 2, 2, 0, true));
//                            pipeline.addLast(new ProtocolDecoder());
//                            pipeline.addLast(new ProtocolEncode());
//                            pipeline.addLast(new SimpleChannelInboundHandler<Pkg>() {
//                                @Override
//                                protected void channelRead0(ChannelHandlerContext ctx, Pkg msg) throws Exception {
//                                    callback.onMessage(ctx.channel().isOpen(), new String(msg.getBody()), msg.getType(), msg.getSn(), msg.getCmd(), msg.getBody());
////                                    callback.callback(new String(msg.getBody()));
////                                    callback.onMessage();
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//                            super.exceptionCaught(ctx, cause);
//                            cause.printStackTrace();
//                            if (!ctx.channel().isOpen()) {
//                                callback.onMessage(false, null, 0, 0, 0, null);
//                            }
//                        }
//                    });
//            // Start the connection attempt.
//            ChannelFuture future = b.connect(ip, port).sync();
//
//            if (future.isSuccess()) {
//                System.out.println("连接成功");
//                return ResultCode.ErrOK;
//            } else {
//                return ResultCode.ErrNetBadRequest;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ResultCode.ErrNetBadRequest;
//    }

    public void stop() {
        try {
            if (channel != null) {
                channel.close().sync();
                channel = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (group != null) {
                group.shutdownGracefully();
                group = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        if (channel != null) {
            return channel.isOpen();
        }
        return false;
    }

    public Completable sendPackage(final int packageType, final int packageIndex, final int command, final String jsonPackage) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> e) throws Exception {
                Pkg pkg = Pkg.createPkg(packageIndex, command, ((short) packageType), jsonPackage.getBytes());
                if (channel != null) {
                    ChannelFuture channelFuture = channel.writeAndFlush(pkg);
                    channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if (future.isSuccess()) {
                                e.onNext(true);
                                e.onComplete();
                            } else {
                                e.onError(future.cause());
                            }
                        }
                    });
                } else {
                    e.onError(new RuntimeException("连接已断开"));
                }
            }
        }).takeUntil(connectSubject.skip(1).filter(new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) throws Exception {
                return !aBoolean;
            }
        })).lastOrError().onErrorReturn(new Function<Throwable, Boolean>() {
            @Override
            public Boolean apply(Throwable throwable) throws Exception {
                throwable.printStackTrace();
                if (throwable instanceof NoSuchElementException) {
                    throw IMException.Companion.create(ResultCode.ErrNetNotConnWebapp);
                } else {
                    Exceptions.propagate(throwable);
                    return false;
                }
            }
        }).toCompletable();
    }

    @Keep
    public interface Callback {
        void onMessage(CommondResp resp);
    }

    private static class InstanceHolder {
        public static final IMNetty instance = new IMNetty();
    }
//    public static native boolean sendPackage();

}
