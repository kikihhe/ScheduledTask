package com.xiaohe.server;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.client.ExecutorBizClient;
import com.xiaohe.biz.impl.ExecutorBizImpl;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.thread.ExecutorRegistryThread;
import com.xiaohe.util.GsonTool;
import com.xiaohe.util.RemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;

import javax.swing.plaf.synth.SynthTreeUI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行器的服务端，用于接收调度中心发送的消息，如:心跳检测、忙碌检测、任务执行。每一个执行器都有一个EmbedServer
 * @date : 2023-09-03 12:42
 */
public class EmbedServer {

    private static Logger logger = LoggerFactory.getLogger(EmbedServer.class);

    /**
     * ExecutorBizImpl 执行器用它来处理从调度中心发来的消息
     */
    private ExecutorBiz executorBiz;

    /**
     * 启动执行器的服务器的线程
     */
    private Thread thread;

    /**
     * 启动内嵌服务器，接收来自调度中心的消息。同时启动注册线程
     *
     * @param address     该执行器的 IP
     * @param port        该执行器的 PORT
     * @param appname     该执行器所属的appname
     * @param accessToken token
     */
    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new ExecutorBizImpl();
        // 在线程中启动Netty服务器
        thread = new Thread(() -> {
            // bizThreadPool 会传入 EmbedHttpServerHandler 处理器中，用于接收任务进行对应的调度
            ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                    0,
                    200,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(2000),
                    // ThreadFactory
                    (r) -> {
                        return new Thread(r, "xxl-job, EmbedServer bizThreadPool-" + r.hashCode());
                    },
                    // Rejection
                    (r, executor) -> {
                        throw new RuntimeException("scheduled-task, EmbedServer bizThreadPool is EXHAUSTED!");
                    }
            );

            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                channel.pipeline()
                                        // 心跳检测
                                        .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))
                                        // Http编码器
                                        .addLast(new HttpServerCodec())
                                        // 聚合消息，Http消息过大时会被拆分，使用这个类将被拆分的消息再次聚合
                                        .addLast(new HttpObjectAggregator(5 * 1024 * 1024))
                                        // 具体处理入站消息的类
                                        .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
                            }
                        }).childOption(ChannelOption.SO_KEEPALIVE, true);
                // 绑定端口号
                ChannelFuture future = bootstrap.bind(port).sync();
                logger.info(">>>>>>>>>>> scheduled-task remoting server start success, nettype = {}, port = {}", EmbedServer.class, port);
                // 执行器的服务端启动后，将此执行器注册到调度中心
                startRegistry(appname, address);

                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                logger.info(">>>>>>>>>>> scheduled-task remoting server stop.");
            } finally {
                try {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

        });
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * 销毁资源的方法
     */
    public void stop() {
        if (thread != null && thread.isAlive()) {
            // 中断此线程
            thread.interrupt();
        }
        // 销毁注册执行器到调度中心的线程
        stopRegistry();
        logger.info("remoting server destroy success");
    }

    /**
     * 启动此执行器的注册线程
     * @param appname
     * @param address
     */
    public void startRegistry(final String appname, final String address) {
        ExecutorRegistryThread.getInstance().start(appname, address);
    }

    /**
     * 停止此执行器的注册线程
     */
    public void stopRegistry() {
        ExecutorRegistryThread.getInstance().toStop();
    }

    /**
     * Netty服务器的入站处理器，所有从调度中心发来的消息由它处理
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        /**
         * ExecutorBizImpl 执行器用它来处理从调度中心发来的消息
         */
        private ExecutorBiz executorBiz;

        /**
         * token令牌
         */
        private String accessToken;

        /**
         * 每进来一个消息就交给线程池处理，避免大量消息 的处理过程 拖垮服务器
         */
        private ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        /**
         * 入站方法，在该方法中进行定时任务的调用
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            // 获取本次请求的信息，如: 数据、uri、请求方法、是否存活、令牌
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(RemotingUtil.SCHEDULED_TASK_ACCESS_TOKEN);

            // 在线程池中调用、返回结果
            bizThreadPool.execute(() -> {
                Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);
                writeResponse(ctx, keepAlive, GsonTool.toJson(responseObj));
            });
        }


        /**
         * 调用 ExecutorBizImpl 处理来自调度中心的请求
         *
         * @param httpMethod
         * @param uri
         * @param requestData
         * @param accessTokenReq
         */
        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {
            // 校验请求方式、uri、token
            if (!HttpMethod.POST.equals(httpMethod)) {
                return new Result<String>(Result.FAIL_CODE, "invalid request, HttpMethod not support");
            }
            if (uri == null || uri.trim().isEmpty()) {
                return new Result<String>(Result.FAIL_CODE, "invalid request, uri-mapping is empty");
            }
            if (accessToken == null || accessToken.trim().isEmpty() || !accessToken.equals(accessTokenReq)) {
                return new Result<String>(Result.FAIL_CODE, "the access token is wrong");
            }
            // 从uri中判断需要做什么样的处理
            try {
                switch (uri) {
                    case "/beat":
                        return executorBiz.beat();
                    case "idleBeat":
                        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    case "/run":
                        TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                        return executorBiz.run(triggerParam);
                    case "/log":
                        LogParam logParam = GsonTool.fromJson(requestData, LogParam.class);
                        return executorBiz.log(logParam);
                    default:
                        return new Result<String>(Result.FAIL_CODE, "invalid request, uri(" + uri + ") is not mapping");
                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new Result<String>(Result.FAIL_CODE, "request error: " + e.getMessage());
            }

        }

        /**
         * 将调度结果写回调度中心
         *
         * @param ctx
         * @param keepAlive
         * @param responseJson
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));
            //设置文本类型
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            //消息的字节长度
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                //连接是存活状态
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(responseJson);

        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error(">>>>>>>>>>> xxl-job provider netty_http server caught exception", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();
                logger.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

}
