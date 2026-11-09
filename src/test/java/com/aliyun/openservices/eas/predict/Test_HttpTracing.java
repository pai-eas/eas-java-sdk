package com.aliyun.openservices.eas.predict;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.eas.predict.http.Compressor;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end HTTP tracing tests against a local JDK {@link HttpServer} that
 * simulates an EAS endpoint, including the server-side tracing headers.
 *
 * HTTP Tracing only reports server-side phases. Assertions read the JSON log
 * lines captured by {@link TracingLogCollector} (logger
 * "com.aliyun...predict.http.tracing").
 */
public class Test_HttpTracing {

    private static HttpServer server;
    private static String endpoint;
    private static final AtomicInteger retryHits = new AtomicInteger();

    private final TracingLogCollector tracing = new TracingLogCollector();

    @BeforeClass
    public static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/predict/ok", Test_HttpTracing::handleOk);
        server.createContext("/api/predict/retry", Test_HttpTracing::handleRetry);
        server.createContext("/api/predict/gzip", Test_HttpTracing::handleGzip);
        server.createContext("/api/predict/header-check", Test_HttpTracing::handleHeaderCheck);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    @Before
    public void resetCounters() {
        tracing.clear();
        retryHits.set(0);
    }

    private static void handleOk(HttpExchange exchange) throws IOException {
        respond(exchange, 200);
    }

    private static void handleRetry(HttpExchange exchange) throws IOException {
        if (retryHits.incrementAndGet() <= 2) {
            respond(exchange, 500);
        } else {
            respond(exchange, 200);
        }
    }

    private static void handleGzip(HttpExchange exchange) throws IOException {
        readBody(exchange);
        addServerTraceHeaders(exchange.getResponseHeaders());
        exchange.getResponseHeaders().set("Content-Encoding", "gzip");
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(gz);
        gzipOut.write("prediction-result".getBytes("UTF-8"));
        gzipOut.close();
        byte[] body = gz.toByteArray();
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void handleHeaderCheck(HttpExchange exchange) throws IOException {
        readBody(exchange);
        String marker = exchange.getRequestHeaders().containsKey("Client-Timestamp")
                ? "present" : "absent";
        byte[] body = marker.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void respond(HttpExchange exchange, int status) throws IOException {
        readBody(exchange);
        addServerTraceHeaders(exchange.getResponseHeaders());
        byte[] body = "prediction-result".getBytes("UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void addServerTraceHeaders(Headers headers) {
        long now = System.currentTimeMillis();
        headers.set("Client-Timestamp", String.valueOf(now - 2));
        headers.set("Server-Timestamp", String.valueOf(now));
        headers.set("Enqueue-Timestamp", String.valueOf(now + 1));
        headers.set("Dequeue-Timestamp", String.valueOf(now + 3));
        headers.set("Response-Timestamp", String.valueOf(now + 6));
        headers.set("Queue-Length", "2");
    }

    private static void readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        in.close();
    }

    private PredictClient newClient(String serviceName, boolean tracingEnabled) throws IOException {
        HttpConfig config = new HttpConfig();
        config.setConnectTimeout(2000);
        config.setReadTimeout(5000);
        config.setHttpTracingEnabled(tracingEnabled);
        PredictClient client = new PredictClient(config);
        client.setEndpoint(endpoint);
        client.setServiceName(serviceName);
        client.setRetryCount(0);
        return client;
    }

    @Test
    public void testServerPhases() throws Exception {
        tracing.install();
        PredictClient client = newClient("ok", true);
        try {
            byte[] result = client.predict("hello".getBytes());
            assertEquals("prediction-result", new String(result));

            JSONObject e = tracing.awaitEvent();
            assertEquals("http://127.0.0.1:" + server.getAddress().getPort() + "/api/predict/ok",
                    e.getString("url.full"));
            assertEquals(1, e.getIntValue("attempt"));
            assertEquals(200, e.getIntValue("http.response.status_code"));
            // server-side phases, derived from the response tracing headers
            assertEquals(1.0, e.getDoubleValue("srv_recv_ms"), 0.001);   // Enqueue - Server
            assertEquals(2.0, e.getDoubleValue("srv_queue_ms"), 0.001);  // Dequeue - Enqueue
            assertEquals(3.0, e.getDoubleValue("srv_process_ms"), 0.001); // Response - Dequeue
            assertEquals(6.0, e.getDoubleValue("srv_total_ms"), 0.001);  // Response - Server
            assertEquals(2, e.getLongValue("srv_queue_length"));
            // client-side phases are intentionally not reported
            assertFalse(e.containsKey("duration_ms"));
            assertFalse(e.containsKey("dns_lookup_ms"));
            assertFalse(e.containsKey("connect_ms"));
            assertFalse(e.containsKey("time_to_first_byte_ms"));
            assertFalse(e.containsKey("sdk_total_ms"));
            assertFalse(e.containsKey("pool_leased"));
        } finally {
            client.shutdown();
            tracing.uninstall();
        }
    }

    @Test
    public void testRetryAttempts() throws Exception {
        tracing.install();
        PredictClient client = newClient("retry", true);
        client.setRetryCount(2);
        try {
            byte[] result = client.predict("hello".getBytes());
            assertEquals("prediction-result", new String(result));

            tracing.awaitEvents(3);
            assertEquals(3, tracing.events().size());
            assertEquals(1, tracing.events().get(0).getIntValue("attempt"));
            assertEquals(2, tracing.events().get(1).getIntValue("attempt"));
            assertEquals(3, tracing.events().get(2).getIntValue("attempt"));
            assertEquals(500, tracing.events().get(0).getIntValue("http.response.status_code"));
            assertEquals(500, tracing.events().get(1).getIntValue("http.response.status_code"));
            assertEquals(200, tracing.events().get(2).getIntValue("http.response.status_code"));
        } finally {
            client.shutdown();
            tracing.uninstall();
        }
    }

    @Test
    public void testCompressionAndDecompression() throws Exception {
        tracing.install();
        PredictClient client = newClient("gzip", true);
        client.setCompressor(Compressor.Gzip);
        client.setDecompressor(Compressor.Auto);
        try {
            byte[] result = client.predict("hello".getBytes());
            assertEquals("prediction-result", new String(result));

            JSONObject e = tracing.awaitEvent();
            assertEquals(200, e.getIntValue("http.response.status_code"));
            assertTrue("server trace expected", e.containsKey("srv_total_ms"));
            // compression/decompression are client-side and not reported
            assertFalse(e.containsKey("compress_ms"));
            assertFalse(e.containsKey("decompress_ms"));
        } finally {
            client.shutdown();
            tracing.uninstall();
        }
    }

    @Test
    public void testAsyncIsNotTraced() throws Exception {
        tracing.install();
        PredictClient client = newClient("header-check", true);
        try {
            // sync predict with tracing enabled activates the server-side trace
            assertEquals("present", new String(client.predict("hello".getBytes())));

            // async is outside HTTP Tracing and must not activate it either
            CompletableFuture<byte[]> future = client.predictAsync("hello".getBytes());
            assertEquals("absent", new String(future.get(10, TimeUnit.SECONDS)));

            // responses without tracing headers must not produce log events
            assertEquals(0, tracing.events().size());
        } finally {
            client.shutdown();
            tracing.uninstall();
        }
    }
}
