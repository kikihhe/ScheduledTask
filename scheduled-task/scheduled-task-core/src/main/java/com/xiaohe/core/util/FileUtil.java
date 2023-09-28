package com.xiaohe.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author : 小何
 * @Description : 文件工具类
 * @date : 2023-09-20 18:41
 */
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 删除指定文件
     *
     * @param file
     */
    public static boolean deleteRecursively(File file) {
        if (file == null) {
            return true;
        }
        // 如果是文件夹，将其下所属的所有文件都删除
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File childFile : files) {
                deleteRecursively(childFile);
            }
        }
        // 最后删除该文件
        return file.delete();
    }

    /**
     * 向文件中写内容
     *
     * @param file 指定文件
     * @param data 待写入的内容
     */
    public static void writeFileContent(File file, byte[] data) {
        if (!file.exists()) {
            return;
        }
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            // 关闭资源
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 读取文件内容，以字节数组的形式返回
     * @param file 文件
     * @return 文件中的内容，字节数组形式
     */
    public static byte[] readFileContent(File file) {
        long fileLength = file.length();
        byte[] fileContent = new byte[(int) fileLength];

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(fileContent);
            return fileContent;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}

