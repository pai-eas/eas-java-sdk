package com.aliyun.openservices.eas.discovery.core;

import com.aliyun.openservices.eas.discovery.utils.StringUtils;
import com.taobao.middleware.logger.Level;
import com.taobao.middleware.logger.Logger;
import com.taobao.middleware.logger.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscoveryClient {
    public static final String VERSION = "DISCOVERY-CLIENT-v0.0.1";
    private static final AtomicBoolean logInited = new AtomicBoolean(false);
    public static Logger LOG = LoggerFactory.getLogger(DiscoveryClient.class);
    private static String encoding = "GBK";
    private static String cacheDir;
    private static String logName;

    static {

        String logPath = System.getProperty("user.home") + "/.eas/logs/client.log";

        // check logDir whether exists. if not, create it
        String parentName = new File(logPath).getParent();
        if (parentName == null) {
            throw new IllegalArgumentException("illegal logPath: " + logPath);
        }

        File parentDir = new File(parentName);
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalArgumentException("unable to create parent dirs in logPath: " + logPath);
        }

        logName = System.getProperty("com.aliyun.eas.log.filename");
        if (StringUtils.isEmpty(logName)) {
            logName = "discovery.log";
        }


        cacheDir = System.getProperty("com.aliyun.eas.cache.dir");
        if (StringUtils.isEmpty(cacheDir)) {
            cacheDir = System.getProperty("user.home") + "/.eas/cache/discovery";
        }

        String logLevel = System.getProperty("com.aliyun.discovery.log.level");
        if (StringUtils.isEmpty(logLevel) || Level.codeOf(logLevel) == Level.OFF) {
            logLevel = "INFO";
        }

        try {
            HostReactor.updateHoldInterval =
                    Long.parseLong(System.getProperty("com.aliyun.eas.update.hold.interval", "5000"));
        } catch (Exception e) {
        }

        if (logInited.compareAndSet(false, true)) {
            LOG.activateAppenderWithSizeRolling("eas-logs", "client.log", encoding, "500MB", 5);
            LOG.setLevel(Level.codeOf(logLevel));
            LOG.setAdditivity(false);
        }

        LOG.info("enable tls: " + Boolean.parseBoolean(System.getProperty("tls.enable", "false")));
    }

    public static Endpoint srvHost(String dom) throws Exception {
        return srvHostWithTimeout(dom, -1L);
    }

    public static Endpoint srvHostWithTimeout(String dom, long timeout) throws Exception {
        return LoadBalancer.RR.selectHost(HostReactor.getService(dom, StringUtils.EMPTY, timeout));
    }

    public static List<Endpoint> getHosts(String dom) throws Exception {
        return LoadBalancer.RR.nothing(HostReactor.getService(dom, StringUtils.EMPTY));
    }

    public static List<Endpoint> getHostsWithTimeout(String dom, long timeout) throws Exception {
        return LoadBalancer.RR.nothing(HostReactor.getService(dom, StringUtils.EMPTY, timeout));
    }

    public static void listen(String dom, Listener listener) {
        EventDispatcher.addListener(dom, StringUtils.EMPTY, listener);
    }

    public static void listen(String dom, String clusters, Listener listener) {
        EventDispatcher.addListener(dom, clusters, listener);
    }


    public static void unlisten(String dom, Listener listener) {
        EventDispatcher.removeListener(dom, StringUtils.EMPTY, listener);
    }


    public static Set<String> getDomsSubscribed() {
        return HostReactor.getSubscribed();
    }

    public static void setListenerExecutor(ExecutorService executor) {
        EventDispatcher.setExecutor(executor);
    }

    public static String getEncoding() {
        return encoding;
    }

    public static void setEncoding(String encoding) {
        DiscoveryClient.encoding = encoding;
    }

    public static String getCacheDir() {
        return cacheDir;
    }

    public static String getLogName() {
        return logName;
    }
}
