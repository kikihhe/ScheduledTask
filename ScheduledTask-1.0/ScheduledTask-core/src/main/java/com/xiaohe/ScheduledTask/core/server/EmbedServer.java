package com.xiaohe.ScheduledTask.core.server;

import com.xiaohe.ScheduledTask.core.biz.model.Result;
import com.xiaohe.ScheduledTask.core.biz.model.TriggerParam;
import com.xiaohe.ScheduledTask.core.executor.ScheduledTaskExecutor;
import com.xiaohe.ScheduledTask.core.handler.IJobHandler;
import com.xiaohe.ScheduledTask.core.handler.impl.MethodJobHandler;
import com.xiaohe.ScheduledTask.core.thread.JobThread;
import com.xiaohe.ScheduledTask.core.util.JacksonUtil;
import com.xiaohe.ScheduledTask.core.util.ObjectUtil;
import io.netty.bootstrap.ServerBootstrap;
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

    public void start(final String address, final int port, final String appname) {
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
                                    .addLast(new EmbedHttpServerHandler(bizThreadPool));

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

    }

    /**
     * 处理调度中心发来的调用信息
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ThreadPoolExecutor bizThreadPool) {
            this.bizThreadPool = bizThreadPool;
        }


        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            HttpMethod method = msg.getMethod();
            // 判断HTTP链接是否还活着
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // 调度定时任务，得到返回结果
                    Object responseObj = process(method, requestData);
                    String responseJson = JacksonUtil.writeValueAsString(responseObj);
                    // 将调用结果回复给调度中心
                    writeResponse(ctx, keepAlive, responseJson);
                }
            });
        }

        /**
         * 使用 ScheduledTaskExecutor 中收集好的bean和method去执行定时任务
         * @param httpMethod
         * @param requestData
         * @return
         */
        public Object process(HttpMethod httpMethod, String requestData) {
            if (!HttpMethod.POST.equals(httpMethod)) {
                return new Result<String>(Result.FAIL_CODE, "invalid request, HttpMethod not valid");
            }
            try {
                TriggerParam triggerParam = (TriggerParam) JacksonUtil.readValue(requestData, TriggerParam.class);
                // 通过 jobId 从jobThreadRepository中获取执行负责该任务的线程
                JobThread jobThread = ScheduledTaskExecutor.loadJobThread(triggerParam.getJobId());
                MethodJobHandler methodJobHandler = (MethodJobHandler) ScheduledTaskExecutor.loadJobHandler(triggerParam.getExecutorHandler());
                // 如果已经注册过，直接将调度参数放入阻塞队列中
                if (!ObjectUtil.isNull(jobThread)) {
                    Result<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
                    return pushResult;
                }
                // 如果没注册过，注册
                jobThread = ScheduledTaskExecutor.registJobThread(triggerParam.getJobId(), methodJobHandler);
                return jobThread.pushTriggerQueue(triggerParam);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new Result<String>(Result.FAIL_CODE, "request error!");
            }
        }
    }

}
