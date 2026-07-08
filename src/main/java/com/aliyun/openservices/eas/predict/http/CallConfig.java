package com.aliyun.openservices.eas.predict.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Per-call configuration that overrides {@link PredictClient}'s defaults
 * for a single predict invocation.
 *
 * <p>Example:
 * <pre>{@code
 * CallConfig config = new CallConfig()
 *     .setRequestPath("/v2")
 *     .setQueryParams(Collections.singletonMap("uid", "1168XXXX"));
 * String result = client.predict(jsonStr, config);
 * }</pre>
 */
public class CallConfig {
    private String requestPath = null;
    private Map<String, String> queryParams = null;

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
