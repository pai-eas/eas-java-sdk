package com.aliyun.openservices.eas.predict.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Per-call configuration that overrides the client's defaults for a single
 * invocation: request path, query parameters, custom HTTP headers and trace id.
 *
 * <p>Example:
 * <pre>{@code
 * CallConfig config = new CallConfig()
 *     .setRequestPath("/v2")
 *     .setQueryParams(Collections.singletonMap("uid", "1168XXXX"))
 *     .setHeaders(Collections.singletonMap("X-Trace-Tag", "demo"))
 *     .setTraceId("trace-123");
 * String result = client.predict(jsonStr, config);
 * }</pre>
 */
public class CallConfig {
    private String requestPath = null;
    private Map<String, String> queryParams = null;
    private Map<String, String> headers = null;
    private String traceId = null;

    public CallConfig() {
    }

    public String getRequestPath() {
        return requestPath;
    }

    public CallConfig setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public CallConfig setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    /**
     * Per-call custom HTTP headers; applied last so they override the
     * client-level headers ({@code addExtraHeaders}) for this request.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    public CallConfig setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Trace id sent in the {@code x-eas-trace-id} header for this call.
     *
     * <p>A non-empty value also overrides a same-named entry configured with
     * {@link #setHeaders(Map)}. When it is {@code null} or empty, no dedicated
     * trace id is added; a same-named entry from {@link #setHeaders(Map)} is
     * left untouched.</p>
     *
     * @return per-call trace id
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Sets the trace id sent in the {@code x-eas-trace-id} header for this
     * call. This takes precedence over a same-named header configured with
     * {@link #setHeaders(Map)}.
     *
     * @param traceId caller-defined trace id; null or empty adds no dedicated
     *                trace id for this call
     */
    public CallConfig setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * Build the request URI suffix (path + query string) for this call,
     * falling back to {@code defaultPath} when no per-call path is set.
     */
    public String requestURI(String defaultPath) {
        String path = this.requestPath != null ? this.requestPath : defaultPath;
        return normalizeRequestPath(path) + buildQueryString(queryParams);
    }

    private static String normalizeRequestPath(String requestPath) {
        if (requestPath == null) {
            return "";
        }
        if (requestPath.length() > 0 && requestPath.charAt(0) != '/') {
            requestPath = "/" + requestPath;
        }
        return requestPath;
    }

    private static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(encodeParam(entry.getKey()));
            String value = entry.getValue();
            if (value != null) {
                sb.append("=").append(encodeParam(value));
            }
        }
        return sb.toString();
    }

    private static String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
