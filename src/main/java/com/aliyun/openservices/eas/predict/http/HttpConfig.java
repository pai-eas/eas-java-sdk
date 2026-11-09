package com.aliyun.openservices.eas.predict.http;

/**
 * Created by yaozheng.wyz on 2017/10/25.
 */
public class HttpConfig {
    private int ioThreadNum;
    private int readTimeout;
    private boolean readTimeoutExplicitlySet;
    private int connectTimeout;
    private int requestTimeout;
    private int maxConnectionCount;
    private int maxConnectionPerRoute;
    private boolean keepAlive;
    private boolean redirectsEnabled;

    /**
     * The interval for executing the automatic cleanup task (in milliseconds).
     * Automatic cleanup is enabled when the value is greater than 0.
     */
    private int connectionCleanupInterval;

    /**
     * The duration after which an idle connection is considered eligible for cleanup (in milliseconds).
     * Effective only when the automatic cleanup task is running.
     */
    private int idleConnectionTimeout;

    /**
     * The time to live for persistent connections (in milliseconds).
     * Connections whose time to live expires will be closed.
     * Negative value disables the check.
     */
    private long connTimeToLive = -1L;

    /**
     * Whether EAS server-side HTTP tracing is enabled. When enabled, each
     * synchronous request attempt (including retries) sends the server
     * activation header and emits a structured event with the server-side
     * tracing phases when the server returns them. Disabled by default.
     */
    private boolean httpTracingEnabled = false;

    /**
     * Default constructor that initializes the default configuration.
     */
    public HttpConfig() {
        this.ioThreadNum = 10;
        this.readTimeout = 5000;
        this.connectTimeout = 5000;
        this.maxConnectionCount = 1000;
        this.maxConnectionPerRoute = 1000;
        this.requestTimeout = 0;
        this.keepAlive = true;
        this.redirectsEnabled = false;
        this.connectionCleanupInterval = 0; // By default, automatic cleanup is disabled
        this.idleConnectionTimeout = 0;
    }

    public HttpConfig(int ioThreadNum, int readTimeout, int connectTimeout,
                      int maxConnectionCount, int maxConnectionPerRoute) {
        super();
        this.ioThreadNum = ioThreadNum;
        this.readTimeout = readTimeout;
        this.readTimeoutExplicitlySet = true;
        this.connectTimeout = connectTimeout;
        this.maxConnectionCount = maxConnectionCount;
        this.maxConnectionPerRoute = maxConnectionPerRoute;
        this.keepAlive = true;
        this.redirectsEnabled = false;
    }

    public HttpConfig(int ioThreadNum, int readTimeout, int connectTimeout,
                      int maxConnectionCount, int maxConnectionPerRoute, int requestTimeout) {
        this(ioThreadNum, readTimeout, connectTimeout, maxConnectionCount, maxConnectionPerRoute);
        this.requestTimeout = requestTimeout;
    }

    public int getIoThreadNum() {
        return ioThreadNum;
    }

    public void setIoThreadNum(int ioThreadNum) {
        this.ioThreadNum = ioThreadNum;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the socket read timeout in milliseconds.
     *
     * <p>When this method is not called and {@link #setRequestTimeout(int)} is
     * set to a positive value, the SDK derives an effective read timeout that
     * is larger than the request timeout, so that the SDK-level request
     * timeout fires first. An explicit value always takes precedence.</p>
     *
     * @param readTimeout socket read timeout in milliseconds
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        this.readTimeoutExplicitlySet = true;
    }

    /**
     * Returns whether {@link #setReadTimeout(int)} or a constructor with an
     * explicit read timeout has been used.
     */
    boolean isReadTimeoutExplicitlySet() {
        return readTimeoutExplicitlySet;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getMaxConnectionCount() {
        return maxConnectionCount;
    }

    public void setMaxConnectionCount(int maxConnectionCount) {
        this.maxConnectionCount = maxConnectionCount;
    }

    public int getMaxConnectionPerRoute() {
        return maxConnectionPerRoute;
    }

    public void setMaxConnectionPerRoute(int maxConnectionPerRoute) {
        this.maxConnectionPerRoute = maxConnectionPerRoute;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the SDK-level wait limit for a single request attempt in
     * milliseconds; 0 disables the limit.
     *
     * <p>If {@link #setReadTimeout(int)} has not been called, the transport
     * read timeout is derived as
     * {@code max(readTimeout, requestTimeout + connectTimeout)} so that the
     * request timeout can fire first. This must be set before the client is
     * constructed.</p>
     *
     * @param requestTimeout request timeout in milliseconds; 0 means unlimited
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Returns the read timeout actually applied to the HTTP transport.
     *
     * <p>An explicitly configured read timeout is returned unchanged.
     * Otherwise, when a positive request timeout is configured, the value is
     * raised to {@code requestTimeout + connectTimeout} (keeping the default
     * read timeout when it is already larger).</p>
     */
    int getEffectiveReadTimeout() {
        if (requestTimeout <= 0 || readTimeoutExplicitlySet) {
            return readTimeout;
        }
        long derived = (long) requestTimeout + Math.max(0, connectTimeout);
        if (derived > Integer.MAX_VALUE) {
            derived = Integer.MAX_VALUE;
        }
        return (int) Math.max(readTimeout, derived);
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean getRedirectsEnabled() {
        return redirectsEnabled;
    }

    public void setRedirectsEnabled(boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
    }

    /**
     * Gets the interval for executing the automatic cleanup task (milliseconds).
     *
     * @return connectionCleanupInterval
     */
    public int getConnectionCleanupInterval() {
        return connectionCleanupInterval;
    }

    /**
     * Sets the interval for executing the automatic cleanup task (milliseconds).
     *
     * @param connectionCleanupInterval Interval for executing the automatic cleanup task (milliseconds)
     */
    public void setConnectionCleanupInterval(int connectionCleanupInterval) {
        this.connectionCleanupInterval = connectionCleanupInterval;
    }

    /**
     * Gets the duration after which an idle connection is considered eligible for cleanup (milliseconds).
     *
     * @return idleConnectionTimeout
     */
    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    /**
     * Sets the duration after which an idle connection is considered eligible for cleanup (milliseconds).
     *
     * @param idleConnectionTimeout Duration after which an idle connection is eligible for cleanup (milliseconds)
     */
    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
        this.idleConnectionTimeout = idleConnectionTimeout;

        if (this.connectionCleanupInterval <= 0) {
            this.connectionCleanupInterval = Math.min(3000, idleConnectionTimeout);
        }
    }

    /**
     * Gets the time to live for persistent connections.
     *
     * @return connTimeToLive
     */
    public long getConnTimeToLive() {
        return connTimeToLive;
    }

    /**
     * Sets the time to live for persistent connections.
     *
     * @param connTimeToLive Time to live for persistent connections
     */
    public void setConnTimeToLive(long connTimeToLive) {
        this.connTimeToLive = connTimeToLive;
        if (this.connectionCleanupInterval <= 0) {
            this.connectionCleanupInterval = Math.min(3000, (int)connTimeToLive);
        }
    }

    /**
     * Whether HTTP tracing is enabled.
     *
     * @return httpTracingEnabled
     */
    public boolean isHttpTracingEnabled() {
        return httpTracingEnabled;
    }

    /**
     * Enables or disables EAS server-side HTTP tracing. Must be set before the
     * client is constructed.
     *
     * @param httpTracingEnabled whether to enable HTTP tracing
     */
    public void setHttpTracingEnabled(boolean httpTracingEnabled) {
        this.httpTracingEnabled = httpTracingEnabled;
    }
}