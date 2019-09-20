package com.aliyun.openservices.eas.discovery.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class FlowControl {
    private static ConcurrentMap<String, Flow> flows
            = new ConcurrentHashMap<String, Flow>();

    public static void enter(String dom) {
        try {
            Flow flow = flows.get(dom);
            if (flow == null) {
                flows.putIfAbsent(dom, new Flow());
                return;
            }

            if (System.currentTimeMillis() - flow.time > 1000) {
                flow.time = System.currentTimeMillis();
                flow.old = flow.cur;
                flow.cur = 1;
            } else {
                flow.cur++;
            }
        } catch (Exception e) {
            DiscoveryClient.LOG.error("NA", "error while entering motion flow for dom: " + dom);
        }
    }

    public static long view(String dom) {
        try {
            Flow flow = flows.get(dom);
            if (flow == null) {
                return 0;
            }

            return flow.old;
        } catch (Exception e) {
            DiscoveryClient.LOG.error("NA", "error while viewing motion flow for dom: " + dom);
            return 0;
        }
    }

    private static class Flow {
        private long cur = 1L;
        private long old = 1L;
        private long time = System.currentTimeMillis();
    }
}