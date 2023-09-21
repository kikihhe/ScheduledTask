package com.xiaohe.util;

import com.xiaohe.biz.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;

/**
 * @author : 小何
 * @Description : 发送HTTP请求的工具类
 * @date : 2023-09-21 12:46
 */
public class XxlJobRemotingUtil {
    private static Logger logger = LoggerFactory.getLogger(XxlJobRemotingUtil.class);

    public static final String XXL_JOB_ACCESS_TOKEN = "XXL-JOB-ACCESS-TOKEN";

    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }};

    /**
     * 信任该HTTPS链接
     *
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
        connection.setHostnameVerifier((hostname, sslSession) -> {
            return true;
        });
    }


    /**
     * 发送post请求
     * @param url 目标url
     * @param accessToken token
     * @param timeout 超时时间
     * @param requestObj 请求体中携带的数据
     * @param returnTargClassOfT 响应中的标签的类型，一般不用。
     * @return Result
     */
    public static Result postBody(String url, String accessToken, int timeout, Object requestObj, Class returnTargClassOfT) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();
            if (url.startsWith("https")) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");
            if (StringUtil.hasText(accessToken)) {
                connection.setRequestProperty(XXL_JOB_ACCESS_TOKEN, accessToken);
            }
            // 连接
            connection.connect();
            // 发送请求
            if (requestObj != null) {
                String requestBody = JsonUtil.writeValueAsString(requestObj);
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
                dataOutputStream.close();
            }
            // 获取响应
            int responseCode = connection.getResponseCode();
            // 如果响应码不是200，返回错误结果
            if (responseCode != 200) {
                return new Result<String>(Result.FAIL_CODE, "xxl-job remoting fail, response code(" + responseCode + "), invalid for url: " + url);
            }
            // 响应成功，读取响应信息
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String resultJson = result.toString();
            return JsonUtil.readValue(resultJson, Result.class);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Result<String>(Result.FAIL_CODE, "xxl-job remoting error(" + e.getMessage() + "), for url : " + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}



