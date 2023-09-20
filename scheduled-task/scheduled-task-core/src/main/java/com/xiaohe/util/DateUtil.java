package com.xiaohe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : 小何
 * @Description : 日期工具类
 * @date : 2023-09-20 18:59
 */
public class DateUtil {

    private static Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final ThreadLocal<Map<String, DateFormat>> dateFormatThreadLocal = new ThreadLocal<>();

    private static DateFormat getDateFormat(String pattern)  {
        if (!StringUtil.hasText(pattern)) {
            logger.error("date format pattern is empty");
            return null;
        }

        Map<String, DateFormat> dateFormatMap = dateFormatThreadLocal.get();
        if (!CollectionUtil.isEmpty(dateFormatMap)) {
            return dateFormatMap.get(pattern);
        }
        synchronized (dateFormatThreadLocal) {
            if (dateFormatMap == null) {
                dateFormatMap = new HashMap<>();
            }
            dateFormatMap.put(pattern, new SimpleDateFormat(pattern));
            dateFormatThreadLocal.set(dateFormatMap);
        }
        return dateFormatMap.get(pattern);

    }



    /**
     * 将Date形式的日期转为字符串形式, yyyy-MM-dd
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        return format(date, DATE_FORMAT);
    }

    /**
     * 将Date形式的日期转为字符串形式, yyyy-MM-dd HH:mm:ss
     * @param date
     * @return
     */
    public static String formatDateTime(Date date) {
        return format(date, DATETIME_FORMAT);
    }


    /**
     * 将字符串类的时间转为Date类，yyyy-MM-dd
     * @param dateString
     * @return
     */
    public static Date parseDate(String dateString) {
        return parse(dateString, DATETIME_FORMAT);
    }

    /**
     * 将字符串类的时间转为Date类，yyyy-MM-dd HH:mm:ss
     * @param dateString
     * @return
     */
    public static Date parseDateTime(String dateString) {
        return parse(dateString, DATETIME_FORMAT);
    }



    private static String format(Date date, String pattern) {
        return getDateFormat(pattern).format(date);
    }

    /**
     * 将字符串形式的日期转为 指定格式的Date类
     * @param dateString 字符串形式的日期
     * @param pattern 指定形式
     * @return
     */
    private static Date parse(String dateString, String pattern) {
        try {
            Date date = getDateFormat(pattern).parse(pattern);
            return date;
        } catch (Exception e) {
            logger.error("parse date error, dateString = {}, pattern = {}, errorMessage = {}", dateString, pattern, e.getMessage());
            return null;
        }
    }

}
