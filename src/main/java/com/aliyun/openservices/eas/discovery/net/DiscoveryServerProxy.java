package com.aliyun.openservices.eas.discovery.net;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.discovery.core.Service;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;
import com.aliyun.openservices.eas.discovery.utils.UtilAndComs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

public class DiscoveryServerProxy {
    static final String SERVER_PORT = System.getProperty("com.aliyun.eas.server.port", "80");
    static final String TLS_SERVER_PORT = System.getProperty("com.aliyun.eas.tls.server.port", "443");
    public static String LOCAL_IP;
    private static ArrayList<String> EASDiscoveryServers = new ArrayList<String>();

    static {
        InitDiscoveryServers();
    }


    public static String reqAPIAsync(final String api, final Map<String, String> params, long timeout) {
        FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return reqAPI(api, params);
            }
        });

        Thread getServiceThread = new Thread(futureTask);
        getServiceThread.start();

        if (timeout <= 0) {
            timeout = UtilAndComs.SERVER_TIME_OUT_MILLIS;
        }

        try {
            return futureTask.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            DiscoveryClient.LOG.error("[reqAPI]", "api:" + api + "params: " + JSON.toJSONString(params), e);
        }

        throw new IllegalStateException("failed to req API:/api/" + api + " after " + UtilAndComs.SERVER_TIME_OUT_MILLIS + " ms.");
    }

    public static String reqAPI(String api, Map<String, String> params, long timeout) {
        return reqAPIAsync(api, params, timeout);
    }


    private static String getSignData(Map<String, String> params) {
        if (params.containsKey("dom")) {
            return System.currentTimeMillis() + Service.SPLITER + params.get("dom");
        } else {
            return String.valueOf(System.currentTimeMillis());
        }
    }


    public static String reqAPI(String api, Map<String, String> params) throws Exception {
        if (EASDiscoveryServers.size() == 0) {
            throw new IllegalStateException("discovery server list is empty");
        }

        int loopCount = EASDiscoveryServers.size();
        for (int i = 0; i < loopCount; i++) {
            for (String curServer : EASDiscoveryServers) {
                try {
                    return callServer(api, params, curServer);
                } catch (Exception e) {
                    // continue trying next server
                    DiscoveryClient.LOG.error("NA", "req api:" + api + " failed, server(" + curServer + ")", e);
                }
            }
        }

        throw new IllegalStateException("failed to req API:" + api + " after all servers(" + EASDiscoveryServers + ") tried");
    }

    public static String callServer(String api, Map<String, String> params, String curServer) throws Exception {

        List<String> headers = Arrays.asList("User-Agent", DiscoveryClient.VERSION,
                "Accept-Encoding", "gzip,deflate,sdch", //
                "Content-Type", "application/json",
                "Connection", "Keep-Alive");

        String url;
        if (curServer.contains(":")) {
            url = HttpClient.getPrefix() + curServer + api;
        } else {
            url = HttpClient.getPrefix() + curServer + ":" + HttpClient.getServerPort() + api;
        }

        HttpClient.HttpResult result = HttpClient.httpGet(url, headers, params, DiscoveryClient.getEncoding());
        if (HttpURLConnection.HTTP_OK == result.code) {
            return result.content;
        }

        if (HttpURLConnection.HTTP_NOT_MODIFIED == result.code) {
            return StringUtils.EMPTY;
        }

        throw new IOException("failed to req API:" + url + " code:" + result.code + " msg: " + result.content);
    }

    public static String localIP() {
        try {
            if (!StringUtils.isEmpty(LOCAL_IP)) {
                return LOCAL_IP;
            }
            String localIP = System.getProperty("com.aliyun.eas.discovery", StringUtils.EMPTY);

            if (!StringUtils.isEmpty(localIP)) {
                LOCAL_IP = localIP;
            } else {
                LOCAL_IP = InetAddress.getLocalHost().getHostAddress();
            }
            return LOCAL_IP;
        } catch (UnknownHostException e) {
            return "resolve_failed";
        }
    }


    public static void InitDiscoveryServers() {
        String s = System.getProperty("com.aliyun.eas.discovery");
        if (StringUtils.isEmpty(s)) {
            throw new IllegalArgumentException("discovery server is not configured");
        }

        EASDiscoveryServers.addAll(Arrays.asList(s.split(",")));
    }

    public static String reqAPI(String api, Map<String, String> params, List<String> servers) {
        if (servers.size() == 0) {
            throw new IllegalArgumentException("no server available");
        }

        Random random = new Random(System.currentTimeMillis());
        int index = random.nextInt(servers.size());

        for (int i = 0; i < servers.size(); i++) {
            String server = servers.get(index);
            try {
                return callServer(api, params, server);
            } catch (Exception e) {
                DiscoveryClient.LOG.error("NA", "req api:" + api + " failed, server(" + server + ")", e);
            }

            index = (index + 1) % servers.size();
        }

        throw new IllegalStateException("failed to req API:/api/" + api + " after all servers(" + servers + ") tried");
    }
}
