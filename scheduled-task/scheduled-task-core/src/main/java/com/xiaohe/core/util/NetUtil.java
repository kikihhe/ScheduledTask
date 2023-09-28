package com.xiaohe.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author : 小何
 * @Description : 网络工具类
 * @date : 2023-09-20 19:46
 */
public class NetUtil {

    private static Logger logger = LoggerFactory.getLogger(NetUtil.class);
    /**
     * 查找可用端口, 从defaultPort开始，先向上找，直到65535, 再向下找
     * @param defaultPort
     * @return
     */
    public static int findAvailablePort(int defaultPort) {
        for (int i = defaultPort; i < 65535; i++) {
            if (!isPortUsed(i)) {
                return i;
            }
        }

        for (int i = defaultPort - 1; i > 0; i--) {
            if (!isPortUsed(i)) {
                return i;
            }
        }
        throw new RuntimeException("no available port");

    }


    /**
     * 查找某个端口是否正在被使用
     * @param port
     * @return
     */
    private static boolean isPortUsed(int port) {
        boolean used = false;
        // new一个ServerSocket，如果没有出现异常，说明没有被使用。
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            used = false;
        } catch (IOException e) {
            logger.error("port: {} is used", port);
            used = true;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.info("");
                }
            }
        }
        return used;
    }

}
