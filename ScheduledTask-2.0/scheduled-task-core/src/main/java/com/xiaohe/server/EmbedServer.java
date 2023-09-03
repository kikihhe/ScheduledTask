package com.xiaohe.server;

import com.xiaohe.biz.ExecutorBiz;
import com.xiaohe.biz.model.IdleBeatParam;
import com.xiaohe.biz.model.LogParam;
import com.xiaohe.biz.model.Result;
import com.xiaohe.biz.model.TriggerParam;
import com.xiaohe.util.GsonTool;
import com.xiaohe.util.RemotingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;

import javax.swing.plaf.synth.SynthTreeUI;
import java.util.concurrent.ThreadPoolExecutor;

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
         * @param httpMethod
         * @param uri
         * @param requestData
         * @param accessTokenReq
         * @return
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
                    case "idleBeat" :
                        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    case "/run" :
                        TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                        return executorBiz.run(triggerParam);
                    case "/log" :
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
    }

}
