package com.aliyun.openservices.eas.predict.http;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared base for the EAS HTTP clients ({@link PredictClient} and
 * {@link QueueClient}). Holds the HTTP transport lifecycle, the retry count
 * and the extra headers, and exposes them as a common fluent configuration
 * surface so both clients do not implement the same parameter interfaces
 * twice.
 *
 * <p>The base is self-typed ({@code T}) so that fluent setters keep returning
 * the concrete client type. It is not intended for external extension: the
 * constructor is package-private.</p>
 *
 * <p>HTTP client ownership: a client built by the SDK is closed by
 * {@link #shutdown()}; a client provided through
 * {@link #setHttpClient(CloseableHttpAsyncClient)} is never started nor
 * closed by the SDK, and the connection-related {@link HttpConfig} settings
 * are ignored (the caller configures them on the provided client).</p>
 */
public abstract class AbstractEasClient<T extends AbstractEasClient<T>> {

    /** HTTP transport shared with the concrete client; set by subclasses. */
    protected volatile HttpClientSupport httpSupport;

    /** SDK-level configuration (retry, compression, content type, ...). */
    protected volatile HttpConfig httpConfig;

    protected volatile int retryCount;

    /** Client-level request path appended after the service/queue segment; "" by default. */
    protected volatile String requestPath = "";

    /** Request header carrying the caller-provided trace id. */
    protected static final String TRACE_ID_HEADER = "x-eas-trace-id";

    /** Trace id sent with every request; null (header omitted) by default. */
    protected volatile String traceId = null;

    protected final Map<String, String> extraHeaders = new ConcurrentHashMap<String, String>();

    AbstractEasClient() {
    }

    /** Returns {@code this}, typed as the concrete client. */
    protected abstract T self();

    /**
     * Sets the number of retries after a failed request.
     *
     * @param retryCount number of retries (the default is client-specific)
     * @return this client
     */
    public T setRetryCount(int retryCount) {
        this.retryCount = Math.max(0, retryCount);
        return self();
    }

    /**
     * Sets the request path appended after the service (PredictClient) or
     * queue (QueueClient) path segment, e.g. "/sleep" turns
     * "/api/predict/{service}" into "/api/predict/{service}/sleep". For
     * services exposing a custom path only.
     *
     * @param requestPath request path relative to the service/queue segment
     * @return this client
     */
    public T setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return self();
    }

    /**
     * Returns the client-level request path ("" by default).
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Sets the trace id sent in the {@code x-eas-trace-id} request header.
     * The value is only transported to the service; it does not enable HTTP
     * tracing. A null or empty value disables the header.
     *
     * @param traceId caller-defined trace id
     * @return this client
     */
    public T setTraceId(String traceId) {
        this.traceId = traceId;
        return self();
    }

    public String getTraceId() {
        return traceId;
    }

    /**
     * Adds custom HTTP headers that are sent with every request.
     *
     * @param extraHeaders headers to add
     * @return this client
     */
    public T addExtraHeaders(Map<String, String> extraHeaders) {
        if (extraHeaders != null && !extraHeaders.isEmpty()) {
            this.extraHeaders.putAll(extraHeaders);
        }
        return self();
    }

    /**
     * Replaces the underlying HTTP client with an externally built one. The
     * SDK neither starts nor closes an externally provided client; the
     * connection-related {@link HttpConfig} settings no longer apply.
     *
     * @param httpClient externally built client; must not be null
     * @return this client
     */
    public synchronized T setHttpClient(CloseableHttpAsyncClient httpClient) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient must not be null");
        }
        HttpConfig config = this.httpConfig != null ? this.httpConfig : new HttpConfig();
        if (this.httpSupport == null) {
            this.httpSupport = HttpClientSupport.withExternalClient(config, httpClient);
        } else {
            this.httpSupport.useExternalClient(config, httpClient);
        }
        return self();
    }

    /**
     * Releases resources held by the SDK-built HTTP client. An externally
     * provided client is left untouched.
     */
    public void shutdown() {
        if (httpSupport != null) {
            httpSupport.shutdown();
        }
    }
}
