package com.xiaohe.ScheduledTask.core.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-23 23:08
 */
public class CollectionUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
}
