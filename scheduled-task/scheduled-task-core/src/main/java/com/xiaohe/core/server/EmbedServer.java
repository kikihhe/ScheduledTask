package com.xiaohe.core.server;

import com.xiaohe.core.biz.ExecutorBiz;
import com.xiaohe.core.biz.impl.ExecutorBizImpl;
import com.xiaohe.core.model.*;
import com.xiaohe.core.util.JsonUtil;
import com.xiaohe.core.util.StringUtil;
import com.xiaohe.core.util.XxlJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description : 执行器的服务端，即接收调度中心发送的消息的Http服务器
 * @date : 2023-09-30 19:06
 */
public class EmbedServer {
    private static Logger logger = LoggerFactory.getLogger(EmbedServer.class);

    /**
     * ExecutorBizImpl，服务器接收到消息后做出处理动作的类
     */
    private ExecutorBiz executorBiz;

    /**
     * 启动Netty服务器的线程
     */
    private Thread thread;

    /**
     * 启动执行器端的服务器。
     *
     * @param address     此执行器的地址
     * @param port        此执行器的端口
     * @param appname     此执行器的appname
     * @param accessToken 通信token
     */
    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new ExecutorBizImpl();
        thread = new Thread(() -> {
            // 接收到消息后将调用流程交给线程池
            ThreadPoolExecutor bizThreadPool = createThreadPool();

            // 开始构建服务端
            NioEventLoopGroup bossGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
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
                                        // http的编解码器
                                        .addLast(new HttpServerCodec())
                                        // 聚合消息，消息太大时拆开
                                        .addLast(new HttpObjectAggregator(5 * 1024 * 1024))
                                        // 具体处理消息的handler
                                        .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
                            }
                        }).childOption(ChannelOption.SO_KEEPALIVE, true);
                // 绑定端口号
                ChannelFuture future = bootstrap.bind(port).sync();
                logger.info(">>>>>>>>>>>>>>> xxl-job remoting server start success, nettype = {}, port = {}", EmbedServer.class, port);
                // TODO 执行器的服务端启动后立即向调度中心注册

            } catch (Exception e) {
                logger.error(">>>>>>>>>>>>>> xxl-job remoting server error.", e);
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

    private ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                0,
                200,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                // ThreadFactory
                r -> new Thread(r, "xxl-job, EmbedServer bizThreadPool-" + r.hashCode()),
                // 拒绝策略
                (r, executor) -> {
                    throw new RuntimeException("xxl-job, EmbedServer bizThreadPool is EXHAUSTED");
                }
        );
    }

    public void stop() throws Exception {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        // TODO 停止此执行器向调度中心注册
        logger.info(">>>>>>>>>>>>>xxl-job remoting server destroy success.");
    }

    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        private ExecutorBiz executorBiz;
        private String accessToken;

        private ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            String requestData = msg.content().toString(StandardCharsets.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
            bizThreadPool.execute(() -> {
                // 通过process调用(process判断uri后借助ExecutorBizImpl完成功能)，返回结果
                Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);
                String responseJson = JsonUtil.writeValueAsString(responseObj);
                // 把此次调用的结果返回给调度中心。
                writeResponse(ctx, keepAlive, responseJson);
            });


        }

        /**
         * 根据uri判断调度中心要干啥，使用ExecutorBizImpl去完成
         * @param httpMethod
         * @param uri
         * @param requestData
         * @param accessTokenReq
         * @return
         */
        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {
            Result result = check(httpMethod, uri, accessTokenReq);
            if (result != null) return result;
            // 根据uri做出相应处理
            try {
                switch (uri) {
                    case "/beat":
                        return executorBiz.beat();
                    case "/idleBeat":
                        IdleBeatParam idleBeatParam = JsonUtil.readValue(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    case "/run" :
                        TriggerParam triggerParam = JsonUtil.readValue(requestData, TriggerParam.class);
                        return executorBiz.run(triggerParam);
                    case "/kill" :
                        KillParam killParam = JsonUtil.readValue(requestData, KillParam.class);
                        return executorBiz.kill(killParam);
                    case "/log":
                        LogParam logParam = JsonUtil.readValue(requestData, LogParam.class);
                        return executorBiz.log(logParam);
                    default:
                        return Result.error("invalid request, uri-mapping(" + uri + ") not found");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return Result.error("request error:" + e);
            }


        }

        /**
         * 将此次http请求的结果返回给调度中心
         * @param ctx
         * @param keepAlive
         * @param responseJson
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, StandardCharsets.UTF_8));
            // 设置文本类型、消息的字节长度
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(response);
        }

        /**
         * 检查此次请求的正确性
         * @param httpMethod
         * @param uri
         * @param accessTokenReq
         * @return
         */
        private Result check(HttpMethod httpMethod, String uri, String accessTokenReq) {
            if (!HttpMethod.POST.equals(httpMethod)) {
                return Result.error("invalid request, HttpMethod not support:" + httpMethod);
            }
            if (!StringUtil.hasText(uri)) {
                return Result.error("invalid request, uri-mapping empty.");
            }
            if (!StringUtil.hasText(accessTokenReq) || !accessTokenReq.equals(accessToken)) {
                return Result.error("The accessToken is wrong.");
            }
            return null;
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



