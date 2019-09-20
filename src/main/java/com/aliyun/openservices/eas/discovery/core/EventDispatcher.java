package com.aliyun.openservices.eas.discovery.core;

import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;


public class EventDispatcher {
    private static ExecutorService executor = null;
    private static BlockingQueue<Service> mailbox = new LinkedBlockingQueue<Service>();
    private static ConcurrentMap<String, List<Listener>> observerMap = new ConcurrentHashMap<String, List<Listener>>();

    static {
        EventDispatcher.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DISCOVERY-CLIENT-LISTENER");
                thread.setDaemon(true);

                return thread;
            }
        });

        EventDispatcher.executor.execute(new Notifier());
    }

    public static void addListener(String dom, String clusters, Listener listener) {
        addListener(dom, clusters, StringUtils.EMPTY, listener);
    }


    public static void addListener(String dom, String clusters, String env, Listener listener) {
        List<Listener> observers = Collections.synchronizedList(new ArrayList<Listener>());
        observers.add(listener);

        observers = observerMap.putIfAbsent(Service.getKey(dom, clusters), observers);
        if (observers != null) {
            observers.add(listener);
        }

        // notify immediately when listener initialization,
        // otherwise no data will be returned till data change
        Service service = HostReactor.getService(dom, clusters, env);

        EventDispatcher.changed(service);
    }


    public static void removeListener(String dom, String clusters, Listener listener) {
        List<Listener> observers = observerMap.get(Service.getKey(dom, clusters));
        if (observers != null) {
            Iterator<Listener> iter = observers.iterator();
            while (iter.hasNext()) {
                Listener oldListener = iter.next();
                if (oldListener.equals(listener)) {
                    iter.remove();
                }
            }
        }
    }


    public static void changed(Service service) {
        if (service == null) {
            return;
        }

        mailbox.add(service);
    }

    public static void setExecutor(ExecutorService executor) {
        ExecutorService oldExecutor = EventDispatcher.executor;
        EventDispatcher.executor = executor;

        oldExecutor.shutdown();
    }

    private static class Notifier implements Runnable {
        @Override
        public void run() {
            while (true) {
                Service dom = null;
                try {
                    dom = mailbox.poll(5, TimeUnit.MINUTES);
                } catch (Exception ignore) {
                }

                if (dom == null) {
                    continue;
                }

                try {
                    List<Listener> listeners = observerMap.get(dom.getKey());

                    if (!CollectionUtils.isEmpty(listeners)) {
                        for (Listener listener : listeners) {
                            List<Endpoint> endpoints = Collections.unmodifiableList(dom.getEndpoints());
                            if (!CollectionUtils.isEmpty(endpoints)) {
                                listener.onChange(endpoints);
                                DiscoveryClient.LOG.info("NOTIFY", "finish notifying Listener, dom: "
                                        + dom.getKey() + ", endpoints size: " + endpoints.size());
                            }
                        }
                    }


                } catch (Exception e) {
                    DiscoveryClient.LOG.error("NA", "notify error for dom: "
                            + dom.getName() + ", clusters: " + dom.getClusters(), e);
                }
            }
        }
    }
}
