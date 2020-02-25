package com.aliyun.openservices.eas.discovery.core;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.eas.discovery.backups.FailoverReactor;
import com.aliyun.openservices.eas.discovery.cache.LocalCache;
import com.aliyun.openservices.eas.discovery.net.DiscoveryServerProxy;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import java.util.*;
import java.util.concurrent.*;

public class HostReactor {
    public static final long DEFAULT_DELAY = 3000L;
    private static final Map<String, ScheduledFuture<?>> futureMap = new HashMap<>();
    public static long updateHoldInterval = 5000L;
    private static Map<String, Service> cachedService = new ConcurrentHashMap<>(LocalCache.read());
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "DISCOVERY-CLIENT-UPDATER");
            thread.setDaemon(true);

            return thread;
        }
    });

    private static ScheduledExecutorService registor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "DISCOVERY-CLIENT-REGISTER");
            thread.setDaemon(true);

            return thread;
        }
    });
    private static FailoverReactor failoverReactor = new FailoverReactor();

    public static Map<String, Service> getCachedService() {
        return cachedService;
    }

    public synchronized static ScheduledFuture<?> addTask(UpdateTask task) {
        return executor.schedule(task, DEFAULT_DELAY, TimeUnit.MILLISECONDS);
    }

    public static Set<String> getSubscribed() {
        Set<String> result = new HashSet<String>();
        List<Service> services = new ArrayList<Service>(getCachedService().values());
        for (Service service : services) {
            result.add(service.getName());
        }

        return result;
    }

    public static Service processService(Service service) {
        Service oldDom = cachedService.get(service.getKey());
        if (service.getEndpoints() == null || !service.validate()) {
            //empty or error push, just ignore
            return oldDom;
        }

        if (oldDom != null) {
            if (oldDom.getLastRefTime() > service.getLastRefTime()) {
                DiscoveryClient.LOG.warn("out of date data received, old-t: " + oldDom.getLastRefTime()
                        + ", new-t: " + service.getLastRefTime());
            }

            cachedService.put(service.getKey(), service);

            Map<String, com.aliyun.openservices.eas.discovery.core.Endpoint> oldHostMap = new HashMap<String, com.aliyun.openservices.eas.discovery.core.Endpoint>();
            for (Endpoint endpoint : oldDom.getEndpoints()) {
                oldHostMap.put(endpoint.toInetAddr(), endpoint);
            }

            Map<String, com.aliyun.openservices.eas.discovery.core.Endpoint> newHostMap = new HashMap<String, com.aliyun.openservices.eas.discovery.core.Endpoint>();
            for (Endpoint endpoint : service.getEndpoints()) {
                newHostMap.put(endpoint.toInetAddr(), endpoint);
            }

            Set<Endpoint> modEndpoints = new HashSet<Endpoint>();
            Set<Endpoint> newEndpoints = new HashSet<Endpoint>();
            Set<Endpoint> remvEndpoints = new HashSet<Endpoint>();

            List<Map.Entry<String, Endpoint>> newDomHosts = new ArrayList<Map.Entry<String, Endpoint>>(newHostMap.entrySet());
            for (Map.Entry<String, Endpoint> entry : newDomHosts) {
                Endpoint endpoint = entry.getValue();
                String key = entry.getKey();
                if (oldHostMap.containsKey(key) && !StringUtils.equals(endpoint.toString(), oldHostMap.get(key).toString())) {
                    modEndpoints.add(endpoint);
                    continue;
                }

                if (!oldHostMap.containsKey(key)) {
                    newEndpoints.add(endpoint);
                }

            }

            for (Map.Entry<String, Endpoint> entry : oldHostMap.entrySet()) {
                Endpoint endpoint = entry.getValue();
                String key = entry.getKey();
                if (newHostMap.containsKey(key)) {
                    continue;
                }

                if (!newHostMap.containsKey(key)) {
                    remvEndpoints.add(endpoint);
                }

            }

            if (newEndpoints.size() > 0) {
                DiscoveryClient.LOG.info("new ips(" + newEndpoints.size() + ") dom: "
                        + service.getName() + " -> " + JSON.toJSONString(newEndpoints));
            }

            if (remvEndpoints.size() > 0) {
                DiscoveryClient.LOG.info("removed ips(" + remvEndpoints.size() + ") dom: "
                        + service.getName() + " -> " + JSON.toJSONString(remvEndpoints));
            }

            if (modEndpoints.size() > 0) {
                DiscoveryClient.LOG.info("modified ips(" + modEndpoints.size() + ") dom: "
                        + service.getName() + " -> " + JSON.toJSONString(modEndpoints));
            }


            if (newEndpoints.size() > 0 || remvEndpoints.size() > 0 || modEndpoints.size() > 0) {
                EventDispatcher.changed(service);
                LocalCache.write(service);
            }

        } else {
            DiscoveryClient.LOG.info("new ips(" + service.ipCount() + ") dom: " + service.getName() + " -> " + JSON.toJSONString(service.getEndpoints()));
            cachedService.put(service.getKey(), service);
            EventDispatcher.changed(service);
            LocalCache.write(service);
        }

        DiscoveryClient.LOG.info("current ips:(" + service.ipCount() + ") dom: " + service.getName() +
                " -> " + JSON.toJSONString(service.getEndpoints()));

        return service;
    }

    private static Service getService0(String dom, String clusters) {
        return cachedService.get(Service.getKey(dom, clusters));
    }


    public static Service getService(String dom, String clusters) {
        return getService(dom, clusters, StringUtils.EMPTY);
    }

    public static Service getService(String dom, String clusters, String env) {
        return getService(dom, clusters, -1L);
    }


    public static Service getService(final String dom, final String clusters, long timeout) {
        DiscoveryClient.LOG.debug("failover-mode: " + failoverReactor.isFailoverSwitch());
        String key = Service.getKey(dom, clusters);
        if (failoverReactor.isFailoverSwitch()) {
            return failoverReactor.getDom(key);
        }

        Service domObj = getService0(dom, clusters);


        if (null == domObj) {
            domObj = new Service(dom, clusters);
            cachedService.put(domObj.getKey(), domObj);
            updateDomNow(dom, clusters);
        } else if (domObj.getEndpoints().isEmpty()) {

            if (updateHoldInterval > 0) {
                // hold a moment waiting for update finish
                synchronized (domObj) {
                    try {
                        domObj.wait(updateHoldInterval);
                    } catch (InterruptedException e) {
                        DiscoveryClient.LOG.error("[getService]", "dom:" + dom + ", clusters:" + clusters + e);
                    }
                }
            }
        }

        scheduleUpdateIfAbsent(dom, clusters);

        return cachedService.get(domObj.getKey());
    }

    public static void scheduleUpdateIfAbsent(String dom, String clusters) {
        if (futureMap.get(Service.getKey(dom, clusters)) != null) {
            return;
        }

        synchronized (futureMap) {
            if (futureMap.get(Service.getKey(dom, clusters)) != null) {
                return;
            }

            ScheduledFuture<?> future = HostReactor.addTask(new UpdateTask(dom, clusters));
            futureMap.put(Service.getKey(dom, clusters), future);
        }
    }

    public static void updateDomNow(String service, String clusters) {
        Service oldDom = getService0(service, clusters);
        try {
            Map<String, String> params = new HashMap<>();

            String namespace = System.getenv("NAMESPACE");
            String podName = System.getenv("POD_NAME");
            String url = "/exported/apis/eas.alibaba-inc.k8s.io/v1/upstreams/" + service;
            if (namespace != null && podName != null) {
              url += "?internal=true";
            }

            String result = DiscoveryServerProxy.reqAPI(url, params);
            if (StringUtils.isNotEmpty(result)) {
                Service inDom = JSON.parseObject(result, Service.class);
                inDom.setName(service);
                inDom.setClusters(clusters);
                inDom.setJsonFromServer(result);
                processService(inDom);
            }
            //else nothing has changed
        } catch (Throwable e) {
            DiscoveryClient.LOG.error("NA", "failed to update service: " + service, e);
        } finally {
            if (oldDom != null) {
                synchronized (oldDom) {
                    oldDom.notifyAll();
                }
            }
        }
    }


    public static class UpdateTask implements Runnable {
        long lastRefTime = Long.MAX_VALUE;
        private String clusters;
        private String dom;


        public UpdateTask(String dom, String clusters) {
            this.dom = dom;
            this.clusters = clusters;
        }


        @Override
        public void run() {
            long delay = DEFAULT_DELAY;
            try {
                Service domObj = cachedService.get(Service.getKey(dom, clusters));
                if (domObj == null) {
                    updateDomNow(dom, clusters);
                    executor.schedule(this, DEFAULT_DELAY, TimeUnit.MILLISECONDS);
                    return;
                }


                if (domObj.getLastRefTime() <= lastRefTime) {
                    updateDomNow(dom, clusters);
                    domObj = cachedService.get(Service.getKey(dom, clusters));


                }

                delay = domObj.getCacheMillis();
                lastRefTime = domObj.getLastRefTime();
            } catch (Throwable e) {
                DiscoveryClient.LOG.error("NA", "failed to update dom: " + dom, e);
            } finally {
                try {
                    executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                    DiscoveryClient.LOG.error("NA", "failed to schedule update task for dom: " + dom, e);
                }
            }
        }
    }
}
