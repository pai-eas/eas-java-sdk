package com.aliyun.openservices.eas.predict.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Internal helper shared by {@link PredictClient} and {@link QueueClient}.
 * Owns the HttpClient 5 async client and its lifecycle:
 *
 * <ul>
 *   <li>When built from {@link HttpConfig}, the SDK creates, starts and closes
 *       the client.</li>
 *   <li>When provided externally, the SDK never starts nor closes the client,
 *       and the connection-related {@link HttpConfig} settings are ignored
 *       (a warning is logged if any non-default value is present).</li>
 * </ul>
 */
final class HttpClientSupport {

    private static final Log log = LogFactory.getLog(HttpClientSupport.class);

    private CloseableHttpAsyncClient client;
    private boolean ownsClient;
    private ScheduledExecutorService scheduler;
    private PoolingAsyncClientConnectionManager pooledManager;

    private HttpClientSupport(CloseableHttpAsyncClient client, boolean ownsClient,
                              ScheduledExecutorService scheduler,
                              PoolingAsyncClientConnectionManager pooledManager) {
        this.client = client;
        this.ownsClient = ownsClient;
        this.scheduler = scheduler;
        this.pooledManager = pooledManager;
    }

    /**
     * Builds and starts an HttpClient 5 async client from the given config.
     */
    static HttpClientSupport create(HttpConfig config) {
        int readTimeout = config.getEffectiveReadTimeout();
        warnIfReadTimeoutTooSmall(config);

        TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(SSLContexts.createDefault())
                .build();
        DnsResolver dnsResolver = SystemDefaultDnsResolver.INSTANCE;

        PoolingAsyncClientConnectionManagerBuilder managerBuilder = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .setDnsResolver(dnsResolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout()))
                        .setSocketTimeout(Timeout.ofMilliseconds(readTimeout))
                        .build())
                .setMaxConnTotal(config.getMaxConnectionCount())
                .setMaxConnPerRoute(config.getMaxConnectionPerRoute());
        if (config.getConnTimeToLive() > 0) {
            managerBuilder.setConnectionTimeToLive(TimeValue.ofMilliseconds(config.getConnTimeToLive()));
        }
        PoolingAsyncClientConnectionManager pooledManager = managerBuilder.build();

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setTcpNoDelay(true)
                .setSoReuseAddress(true)
                .setIoThreadCount(config.getIoThreadNum())
                .setSoKeepAlive(config.isKeepAlive())
                .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(config.getRedirectsEnabled())
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(pooledManager)
                .setIOReactorConfig(ioReactorConfig)
                .setDefaultRequestConfig(requestConfig)
                .build();
        client.start();

        ScheduledExecutorService scheduler = null;
        int cleanupInterval = config.getConnectionCleanupInterval();
        int idleTimeout = config.getIdleConnectionTimeout();
        if (cleanupInterval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "eas-http-connection-cleanup");
                thread.setDaemon(true);
                return thread;
            });
            final PoolingAsyncClientConnectionManager manager = pooledManager;
            final int idle = idleTimeout;
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    manager.closeExpired();
                    if (idle > 0) {
                        manager.closeIdle(TimeValue.ofMilliseconds(idle));
                    }
                } catch (RuntimeException e) {
                    // A failed cleanup must not cancel all future executions.
                    log.warn("Connection cleanup failed", e);
                }
            }, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
        }
        return new HttpClientSupport(client, true, scheduler, pooledManager);
    }

    /**
     * Adopts an externally provided client. The caller keeps ownership: the
     * SDK never starts or closes it.
     */
    static HttpClientSupport withExternalClient(HttpConfig config, CloseableHttpAsyncClient client) {
        if (client == null) {
            throw new IllegalArgumentException("httpClient must not be null");
        }
        if (config != null) {
            warnIfConnectionSettingsIgnored(config);
        }
        return new HttpClientSupport(client, false, null, null);
    }

    /**
     * Child clients share the underlying client but never own it: a child
     * shutdown or client replacement must not close the parent's connection
     * pool.
     */
    HttpClientSupport shareForChild() {
        return new HttpClientSupport(client, false, null, pooledManager);
    }

    /**
     * Replaces the current client with an externally provided one. An owned
     * client is closed; an external client is never closed by the SDK.
     */
    void useExternalClient(HttpConfig config, CloseableHttpAsyncClient newClient) {
        if (newClient == null) {
            throw new IllegalArgumentException("httpClient must not be null");
        }
        warnIfConnectionSettingsIgnored(config);
        if (ownsClient) {
            closeQuietly(client);
            stopScheduler();
        }
        this.client = newClient;
        this.ownsClient = false;
        this.pooledManager = null;
    }

    CloseableHttpAsyncClient getClient() {
        return client;
    }

    void shutdown() {
        stopScheduler();
        if (ownsClient) {
            closeQuietly(client);
        }
    }

    private void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
    }

    private static void closeQuietly(CloseableHttpAsyncClient client) {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (IOException e) {
            log.warn("Failed to close HTTP client", e);
        }
    }

    /**
     * An explicit read timeout is respected as-is. Warn when it is not larger
     * than the SDK-level request timeout, because the read timeout is likely
     * to fire first and hide the configured request timeout.
     */
    private static void warnIfReadTimeoutTooSmall(HttpConfig config) {
        if (config.getRequestTimeout() > 0
                && config.isReadTimeoutExplicitlySet()
                && config.getRequestTimeout() >= config.getReadTimeout()) {
            log.warn("HttpConfig.requestTimeout (" + config.getRequestTimeout()
                    + " ms) is not smaller than the explicitly configured readTimeout ("
                    + config.getReadTimeout() + " ms); the read timeout may fire first. "
                    + "Increase readTimeout above requestTimeout, or leave readTimeout unset "
                    + "so that it is derived automatically.");
        }
    }

    /**
     * In external-client mode the connection-related settings are owned by the
     * caller; warn when a non-default value suggests the caller expected the
     * SDK to apply them.
     */
    private static void warnIfConnectionSettingsIgnored(HttpConfig config) {
        HttpConfig defaults = new HttpConfig();
        if (config.getIoThreadNum() != defaults.getIoThreadNum()
                || config.getReadTimeout() != defaults.getReadTimeout()
                || config.getConnectTimeout() != defaults.getConnectTimeout()
                || config.getMaxConnectionCount() != defaults.getMaxConnectionCount()
                || config.getMaxConnectionPerRoute() != defaults.getMaxConnectionPerRoute()
                || config.isKeepAlive() != defaults.isKeepAlive()
                || config.getRedirectsEnabled() != defaults.getRedirectsEnabled()
                || config.getConnectionCleanupInterval() != defaults.getConnectionCleanupInterval()
                || config.getIdleConnectionTimeout() != defaults.getIdleConnectionTimeout()
                || config.getConnTimeToLive() != defaults.getConnTimeToLive()) {
            log.warn("HttpConfig connection settings are ignored because an external HTTP client is used; "
                    + "configure timeouts/pooling directly on the provided client.");
        }
    }
}
