package com.aliyun.openservices.eas.discovery.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Innovated by xuanyin.zy on 10/21/2015.
 */
public class NetUtils {
    private static String LOCAL_IP;

    public static String localIP() {
        try {
            if (!StringUtils.isEmpty(LOCAL_IP)) {
                return LOCAL_IP;
            }

            String ip = System.getProperty("com.taobao.vipserver.localIP", InetAddress.getLocalHost().getHostAddress());

            return LOCAL_IP = ip;
        } catch (UnknownHostException e) {
            return "resolve_failed";
        }
    }
}
