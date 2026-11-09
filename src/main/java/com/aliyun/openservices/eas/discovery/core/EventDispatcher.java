package com.aliyun.openservices.eas.discovery.core;

import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;


public class EventDispatcher {
    private static volatile ExecutorService executor = null;
    private static BlockingQueue<Service> mailbox = new LinkedBlockingQueue<Service>();
    private static ConcurrentMap<String, List<Listener>> observerMap = new ConcurrentHashMap<String, List<Listener>>();

    static {
        ExecutorService initialExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DISCOVERY-CLIENT-LISTENER");
                thread.setDaemon(true);

                return thread;
            }
        });
        executor = initialExecutor;
        initialExecutor.execute(new Notifier(initialExecutor));
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
            synchronized (observers) {
                Iterator<Listener> iter = observers.iterator();
                while (iter.hasNext()) {
                    Listener oldListener = iter.next();
                    if (oldListener.equals(listener)) {
                        iter.remove();
                    }
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
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        ExecutorService oldExecutor = EventDispatcher.executor;
        if (oldExecutor == executor) {
            return;
        }
        EventDispatcher.executor = executor;
        executor.execute(new Notifier(executor));
        oldExecutor.shutdownNow();
    }

    private static class Notifier implements Runnable {
        private final ExecutorService owner;

        Notifier(ExecutorService owner) {
            this.owner = owner;
        }

        @Override
        public void run() {
            while (executor == owner) {
                Service dom = null;
                try {
                    dom = mailbox.poll(5, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ignore) {
                }

                if (dom == null) {
                    continue;
                }

                try {
                    List<Listener> listeners = observerMap.get(dom.getKey());
                    if (!CollectionUtils.isEmpty(listeners)) {
                        List<Listener> snapshot;
                        synchronized (listeners) {
                            snapshot = new ArrayList<Listener>(listeners);
                        }
                        List<Endpoint> endpoints = Collections.unmodifiableList(dom.getEndpoints());
                        if (!CollectionUtils.isEmpty(endpoints)) {
                            for (Listener listener : snapshot) {
                                try {
                                    listener.onChange(endpoints);
                                    DiscoveryClient.LOG.info("NOTIFY", "finish notifying Listener, dom: "
                                            + dom.getKey() + ", endpoints size: " + endpoints.size());
                                } catch (Exception e) {
                                    // A broken listener must not prevent the
                                    // remaining listeners from being notified.
                                    DiscoveryClient.LOG.error("NA", "listener notify error for dom: "
                                            + dom.getName() + ", clusters: " + dom.getClusters(), e);
                                }
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
