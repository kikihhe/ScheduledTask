package com.xiaohe.core.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author : 小何
 * @Description : 集合工具类
 * @date : 2023-09-20 18:36
 */
public class CollectionUtil {
    /**
     * 判断集合是否为空
     * @param collection 待判断集合
     * @return
     */
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断Map是否为空
     * @param collection
     * @return
     */
    public static boolean isEmpty(Map collection) {
        return collection == null || collection.isEmpty();
    }

}
