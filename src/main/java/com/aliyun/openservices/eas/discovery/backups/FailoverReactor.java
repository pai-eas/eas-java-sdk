package com.aliyun.openservices.eas.discovery.backups;

import com.aliyun.openservices.eas.discovery.cache.ConcurrentDiskUtil;
import com.aliyun.openservices.eas.discovery.cache.LocalCache;
import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.discovery.core.HostReactor;
import com.aliyun.openservices.eas.discovery.core.Service;
import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;
import com.aliyun.openservices.eas.discovery.utils.UtilAndComs;
import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;


public class FailoverReactor {
    private Map<String, Service> domainMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("DISCOVERY-FAILOVER");
            return thread;
        }
    });
    private Timer timer = new Timer(true);
    private Map<String, String> switchParams = new ConcurrentHashMap<>();
    private String failoverDir;
    public FailoverReactor() {
        this.init();
    }

    public void init() {
        failoverDir = System.getProperty("com.aliyun.eas.failover", "");

        if (StringUtils.isEmpty(failoverDir)) {
            failoverDir = UtilAndComs.FAILOVER_DIR;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 3); //03:05
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();

        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }

        executorService.scheduleWithFixedDelay(new SwitchRefresher(), 0L, 5000L, TimeUnit.MILLISECONDS);
        long DAY_PERIOD_MILLS = 24 * 3600 * 1000;
        timer.schedule(new DiskFileWriter(), date, DAY_PERIOD_MILLS);
        executorService.schedule(new Runnable() {// backup file on startup if failover directory is empty.
            @Override
            public void run() {
                try {
                    File cacheDir = new File(failoverDir);
                    if ((!cacheDir.exists() || !cacheDir.isDirectory()) && !cacheDir.mkdirs()) {
                        throw new IllegalStateException("failed to create cache dir: " + failoverDir);
                    }

                    File[] files = cacheDir.listFiles();
                    if (files == null || files.length <= 0) {
                        new DiskFileWriter().run();
                    }
                } catch (Throwable e) {
                    DiscoveryClient.LOG.error("NA", "failed to backup file on startup.", e);
                }

            }
        }, 10000L, TimeUnit.MILLISECONDS);
    }

    public Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    public boolean isFailoverSwitch() {
        return Boolean.parseBoolean(switchParams.get("failover-mode"));
    }

    public Service getDom(String key) {
        Service service = domainMap.get(key);

        if (service == null) {
            service = new Service();
            service.setName(key);
        }

        return service;
    }

    class SwitchRefresher implements Runnable {
        long lastModifiedMillis = 0L;

        @Override
        public void run() {
            try {
                File switchFile = new File(failoverDir + UtilAndComs.FAILOVER_SWITCH);
                if (!switchFile.exists()) {
                    switchParams.put("failover-mode", "false");
                    DiscoveryClient.LOG.debug("failover switch is not found, " + switchFile.getName());
                    return;
                }

                long modified = switchFile.lastModified();

                if (lastModifiedMillis < modified) {
                    lastModifiedMillis = modified;
                    String failover = ConcurrentDiskUtil.getFileContent(failoverDir + UtilAndComs.FAILOVER_SWITCH, Charset.defaultCharset().toString());
                    if (!StringUtils.isEmpty(failover)) {
                        String[] lines = failover.split(LocalCache.getLineSeperator());

                        for (String line : lines) {
                            String line1 = line.trim();
                            if (line1.equals("1")) {
                                switchParams.put("failover-mode", "true");
                                DiscoveryClient.LOG.info("failover-mode is on");
                                new FailoverFileReader().run();
                            } else if (line1.equals("0")) {
                                switchParams.put("failover-mode", "false");
                                DiscoveryClient.LOG.info("failover-mode is off");
                            }
                        }
                    } else {
                        switchParams.put("failover-mode", "false");
                    }
                }

            } catch (Throwable e) {
                DiscoveryClient.LOG.error("NA", "failed to read failover switch.", e);
            }
        }
    }

    class FailoverFileReader implements Runnable {

        @Override
        public void run() {
            Map<String, Service> domMap = new HashMap<String, Service>();

            BufferedReader reader = null;
            try {

                File cacheDir = new File(failoverDir);
                if ((!cacheDir.exists() || !cacheDir.isDirectory()) && !cacheDir.mkdirs()) {
                    throw new IllegalStateException("failed to create cache dir: " + failoverDir);
                }

                File[] files = cacheDir.listFiles();
                if (files == null) {
                    return;
                }

                for (File file : files) {
                    if (!file.isFile()) {
                        continue;
                    }

                    if (file.getName().equals(UtilAndComs.FAILOVER_SWITCH)) {
                        continue;
                    }

                    Service dom = new Service(file.getName());

                    try {
                        String dataString = ConcurrentDiskUtil.getFileContent(file, Charset.defaultCharset().toString());
                        reader = new BufferedReader(new StringReader(dataString));

                        String json;
                        if ((json = reader.readLine()) != null) {
                            try {
                                dom = JSON.parseObject(json, Service.class);
                            } catch (Exception e) {
                                DiscoveryClient.LOG.error("NA", "error while parsing cached dom : " + json, e);
                            }
                        }

                    } catch (Exception e) {
                        DiscoveryClient.LOG.error("NA", "failed to read cache for dom: " + file.getName(), e);
                    } finally {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                    if (!CollectionUtils.isEmpty(dom.getEndpoints())) {
                        domMap.put(dom.getKey(), dom);
                    }
                }
            } catch (Exception e) {
                DiscoveryClient.LOG.error("NA", "failed to read cache file", e);
            }

            if (domMap.size() > 0) {
                domainMap = domMap;
            }
        }
    }

    class DiskFileWriter extends TimerTask {
        public void run() {
            Map<String, Service> map = HostReactor.getCachedService();
            for (Map.Entry<String, Service> entry : map.entrySet()) {
                Service service = entry.getValue();
                if (StringUtils.equals(service.getKey(), UtilAndComs.ALL_IPS) || StringUtils.equals(service.getName(), UtilAndComs.ENV_LIST_KEY)
                        || StringUtils.equals(service.getName(), "00-00---000-ENV_CONFIGS-000---00-00")
                        || StringUtils.equals(service.getName(), "discovery.properties")
                        || StringUtils.equals(service.getName(), "00-00---000-ALL_HOSTS-000---00-00")) {
                    continue;
                }

                LocalCache.write(service, failoverDir);
            }
        }
    }
}
