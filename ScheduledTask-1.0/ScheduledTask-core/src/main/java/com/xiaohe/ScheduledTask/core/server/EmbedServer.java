package com.xiaohe.ScheduledTask.core.server;

import com.xiaohe.ScheduledTask.core.biz.ExecutorBiz;
import com.xiaohe.ScheduledTask.core.biz.impl.ExecutorBizImpl;
import com.xiaohe.ScheduledTask.core.biz.model.IdleBeatParam;
import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import com.xiaohe.ScheduledTask.core.thread.ExecutorRegistryThread;
import com.xiaohe.ScheduledTask.core.util.GsonTool;
import com.xiaohe.ScheduledTask.core.util.RemotingUtil;
import com.xiaohe.ScheduledTask.core.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-24 16:28
 */
public class EmbedServer {
    private static final Logger logger = LoggerFactory.getLogger(EmbedServer.class);
    private ExecutorBiz executorBiz;

    /**
     * 启动Netty服务器的线程
     */
    private Thread thread;


    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new ExecutorBizImpl();
        thread = new Thread(() -> {
// 先创建好的业务线程池
            ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                    0,
                    200,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(2000),
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "ScheduledTask, EmbedServer bizThreadPool-" + r.hashCode());
                        }
                    },
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            throw new RuntimeException("ScheduledTask, EmbedServer bizThreadPool is EXHAUSTED!");
                        }
                    });


            NioEventLoopGroup bossGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap bootStrap = new ServerBootstrap();
                bootStrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                channel.pipeline()
                                        // 心跳检测
                                        .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))
                                        // http编解码器，入站、出站都有
                                        .addLast(new HttpServerCodec())
                                        // 聚合消息的，入站处理器
                                        .addLast(new HttpObjectAggregator(5 * 1024 * 1024))
                                        // 最后收到消息，处理消息的handler
                                        .addLast(new EmbedHttpServerHandler(bizThreadPool, executorBiz, accessToken));

                            }
                        })
                        .childOption(ChannelOption.SO_KEEPALIVE, true);
                // 绑定端口号
                ChannelFuture future = bootStrap.bind(port).sync();
                // 将执行器注册到调度中心
                startRegistry(appname, address);
                // 等待关闭
                future.channel().closeFuture().sync();


            } catch (InterruptedException e) {
                logger.error(">>>>>>>>>>> ScheduledTask remoting server stop.");
            } finally {
                try {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();


    }

    public void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        stopRegistry();
        logger.info(">>>>>>>>>>> ScheduledTask remoting server destroy success.");
    }

    /**
     * 启动注册线程，然后把执行器注册到调度中心
     *
     * @param appname
     * @param address
     */
    public void startRegistry(final String appname, final String address) {
        //启动线程，注册执行器到调度中心
        ExecutorRegistryThread.getInstance().start(appname, address);
    }

    /**
     * 销毁注册线程
     */
    public void stopRegistry() {
        ExecutorRegistryThread.getInstance().toStop();
    }

    /**
     * 处理调度中心发来的调用信息
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        /**
         * 接收到调度中心的信息后，由这个线程池区分，再调用executorBiz做具体的处理
         */
        private ThreadPoolExecutor bizThreadPool;
        /**
         * 调度中心发来信息后做出具体回应的类
         */
        private ExecutorBiz executorBiz;
        private String accessToken;

        public EmbedHttpServerHandler(ThreadPoolExecutor bizThreadPool, ExecutorBiz executorBiz, String accessToken) {
            this.bizThreadPool = bizThreadPool;
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            HttpMethod method = msg.getMethod();
            String uri = msg.getUri();
            String accessToken = msg.headers().get(RemotingUtil.SCHEDULED_TASK_ACCESS_TOKEN);
            // 判断HTTP链接是否还活着
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // 调度定时任务，得到返回结果
                    Object responseObj = process(method, uri, requestData, accessToken);
                    String responseJson = GsonTool.toJson(responseObj);
                    // 将调用结果回复给调度中心
                    writeResponse(ctx, keepAlive, responseJson);
                }
            });
        }

        /**
         * 使用 ScheduledTaskExecutor 中收集好的bean和method去执行定时任务
         *
         * @param httpMethod
         * @param requestData
         * @return
         */
        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {
            // 参数校验
            if (!HttpMethod.POST.equals(httpMethod)) {
                return new Result<String>(Result.FAIL_CODE, "invalid request, HttpMethod not valid");
            }
            if (!StringUtil.hasText(uri)) {
                return new Result<String>(Result.FAIL_CODE, "invalid request, uri-mapping empty.");
            }
            if (!StringUtil.hasText(accessTokenReq) || !StringUtil.hasText(accessToken) || !accessTokenReq.equals(accessToken)) {
                return new Result<String>(Result.FAIL_CODE, "the access token is wrong!");
            }
            try {
                // 判断从调度中心发来的信息是什么类型，调度信息、心跳检测、忙碌检测
                switch (uri) {
                    case "/run":
                        // 执行任务
                        TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                        return executorBiz.run(triggerParam);

                    case "/beat":
                        // 心跳检测, 回复一下
                        return executorBiz.beat();
                    case "/idleBeat":
                        // 调度中心使用了忙碌转移策略，现在看看负责这个任务的线程是不是忙碌状态
                        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    default:
                        return new Result<String>(Result.FAIL_CODE, "invalid request, uri-mapping " + uri + " not found");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new Result<String>(Result.FAIL_CODE, "request error!");
            }
        }

        /**
         * 将调度结果写回调度中心
         * @param ctx
         * @param keepAlive
         * @param responseJson
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            // 设置响应结果
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));
            // 设置文本类型
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // 如果是存活状态
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            // 发送消息
            ctx.writeAndFlush(response);
        }
    }

}
