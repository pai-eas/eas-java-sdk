package com.aliyun.openservices.eas.predict.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;

/**
 * EAS server-side HTTP tracing.
 *
 * <p>When enabled, the SDK sends a {@code Client-Timestamp} header on
 * synchronous predict calls. The EAS server then returns its own phase
 * timestamps in the response headers, which are emitted as one JSON log line
 * on the logger {@code com.aliyun.openservices.eas.predict.http.tracing}.</p>
 *
 * <p>Only server-side phases are reported. Client-side phases (DNS, connect,
 * TLS, send, TTFB, download, ...) are intentionally left to the caller, e.g.
 * via interceptors on an externally provided HttpClient.</p>
 */
final class ServerTracing {

    static final String LOGGER_NAME = "com.aliyun.openservices.eas.predict.http.tracing";

    private static final Log log = LogFactory.getLog(LOGGER_NAME);

    private ServerTracing() {
    }

    /**
     * Parses the server tracing headers and emits one log line when the
     * response contains a server timestamp. Responses without tracing headers
     * (server-side tracing not enabled) produce no log.
     */
    public static void logIfPresent(String url, int attempt, int statusCode, HttpResponse response) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Trace trace = Trace.parse(response);
        if (!trace.isPresent()) {
            return;
        }

        JSONObject o = new JSONObject(true);
        o.put("timestamp", System.currentTimeMillis());
        o.put("event", "eas_http_tracing");
        if (url != null) {
            o.put("url.full", url);
        }
        o.put("attempt", attempt);
        o.put("http.response.status_code", statusCode);
        putMs(o, "srv_recv_ms", trace.recvMs());
        putMs(o, "srv_queue_ms", trace.queueMs());
        putMs(o, "srv_process_ms", trace.processMs());
        putMs(o, "srv_total_ms", trace.totalMs());
        if (trace.queueLength >= 0) {
            o.put("srv_queue_length", trace.queueLength);
        }
        log.info(o.toJSONString());
    }

    private static void putMs(JSONObject o, String key, double value) {
        if (value >= 0) {
            o.put(key, value);
        }
    }

    private static final class Trace {

        private final long tsServer;
        private final long tsEnqueue;
        private final long tsDequeue;
        private final long tsResponse;
        private final long queueLength;

        private Trace(long tsServer, long tsEnqueue, long tsDequeue, long tsResponse, long queueLength) {
            this.tsServer = tsServer;
            this.tsEnqueue = tsEnqueue;
            this.tsDequeue = tsDequeue;
            this.tsResponse = tsResponse;
            this.queueLength = queueLength;
        }

        static Trace parse(HttpResponse response) {
            long nowMs = System.currentTimeMillis();
            return new Trace(
                    normalizeToMillis(parseHeader(response, "Server-Timestamp"), nowMs),
                    normalizeToMillis(parseHeader(response, "Enqueue-Timestamp"), nowMs),
                    normalizeToMillis(parseHeader(response, "Dequeue-Timestamp"), nowMs),
                    normalizeToMillis(parseHeader(response, "Response-Timestamp"), nowMs),
                    parseHeader(response, "Queue-Length"));
        }

        boolean isPresent() {
            return tsServer > 0;
        }

        double recvMs() {
            return tsEnqueue > 0 ? tsEnqueue - tsServer : -1;
        }

        double queueMs() {
            return tsEnqueue > 0 && tsDequeue > 0 ? tsDequeue - tsEnqueue : -1;
        }

        double processMs() {
            return tsDequeue > 0 && tsResponse > 0 ? tsResponse - tsDequeue : -1;
        }

        double totalMs() {
            return tsResponse > 0 ? tsResponse - tsServer : -1;
        }
    }

    /**
     * Different EAS deployments report epoch timestamps at different
     * precisions (the documented format is milliseconds, but some gateways
     * return microseconds or nanoseconds). The magnitude relative to the
     * current wall clock identifies the unit unambiguously.
     */
    static long normalizeToMillis(long timestamp, long nowMs) {
        if (timestamp <= 0) {
            return -1;
        }
        if (timestamp > nowMs * 100_000L) {
            return timestamp / 1_000_000L; // nanoseconds
        }
        if (timestamp > nowMs * 100L) {
            return timestamp / 1_000L;      // microseconds
        }
        return timestamp;                    // milliseconds
    }

    private static long parseHeader(HttpResponse response, String name) {
        Header header = response.getFirstHeader(name);
        if (header == null) {
            return -1;
        }
        try {
            return Long.parseLong(header.getValue().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
