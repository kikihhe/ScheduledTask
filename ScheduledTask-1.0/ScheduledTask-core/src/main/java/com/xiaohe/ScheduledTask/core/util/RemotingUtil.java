package com.xiaohe.ScheduledTask.core.util;

import com.xiaohe.ScheduledTask.core.biz.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author : 小何
 * @Description : 发送http请求的工具类
 * @date : 2023-08-24 20:59
 */
public class RemotingUtil {
    private static Logger logger = LoggerFactory.getLogger(RemotingUtil.class);

    public static final String SCHEDULED_TASK_ACCESS_TOKEN = "SCHEDULED-TASK-ACCESS-TOKEN";

    /**
     * 信任该http链接
     * @param connection
     */
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();
            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }


    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};


    /**
     * 发送post消息
     * @param url 目的地url
     * @param accessToken 通信token
     * @param timeout 超时时间
     * @param requestObj 请求体中的数据
     * @param returnTargClassOfT 返回数据的类型
     * @return
     */
    public static Result postBody(String url, String accessToken, int timeout, Object requestObj, Class returnTargClassOfT) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            //创建链接
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();
            //判断是不是https开头的
            boolean useHttps = url.startsWith("https");
            if (useHttps) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");
            //判断令牌是否为空
            if(accessToken!=null && accessToken.trim().length()>0){
                //设置令牌，以键值对的形式，键就是该类的静态成员变量
                connection.setRequestProperty(SCHEDULED_TASK_ACCESS_TOKEN, accessToken);
            }
            //进行连接
            connection.connect();
            if (requestObj != null) {
                //序列化请求体，也就是要发送的触发器参数
                String requestBody = JacksonUtil.writeValueAsString(requestObj);
                //下面就开始正式发送消息了
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes("UTF-8"));
                //刷新缓冲区
                dataOutputStream.flush();
                //释放资源
                dataOutputStream.close();
            }
            //获取响应码
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                //设置失败结果
                return new Result<String>(Result.FAIL_CODE, "ScheduledTask remoting fail, StatusCode("+ statusCode +") invalid. for url : " + url);
            }
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            //接收返回信息
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            //转换为字符串
            String resultJson = result.toString();
            try {
                //转换为ReturnT对象，返回给用户
                Result returnT = JacksonUtil.readValue(resultJson, Result.class, returnTargClassOfT);
                return returnT;
            } catch (Exception e) {
                logger.error("ScheduledTask remoting (url="+url+") response content invalid("+ resultJson +").", e);
                return new Result<String>(Result.FAIL_CODE, "ScheduledTask remoting (url="+url+") response content invalid("+ resultJson +").");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<String>(Result.FAIL_CODE, "ScheduledTask remoting error("+ e.getMessage() +"), for url : " + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
    }

}
