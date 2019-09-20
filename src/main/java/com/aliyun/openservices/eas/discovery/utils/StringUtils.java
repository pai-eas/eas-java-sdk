package com.aliyun.openservices.eas.discovery.utils;

import java.util.Collection;


public class StringUtils {
    public static final String EMPTY = "";

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    public static String join(Collection collection, String separator) {
        if (collection == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        Object[] objects = collection.toArray();

        for (int i = 0; i < collection.size() - 1; i++) {
            stringBuilder.append(objects[i].toString()).append(separator);
        }

        if (collection.size() > 0) {
            stringBuilder.append(objects[collection.size() - 1]);
        }

        return stringBuilder.toString();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

}
