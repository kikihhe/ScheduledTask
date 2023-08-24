package com.xiaohe.ScheduledTask.admin.core.util;

import java.util.Collection;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-08-23 23:08
 */
public class CollectionUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
