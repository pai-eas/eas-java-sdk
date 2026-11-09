package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.CallConfig;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the externally provided HttpClient 5 client contract: the SDK uses
 * it as-is and never starts or closes it. HTTP tracing only reads server-side
 * phases from response headers, so it works the same with an external client.
 */
public class Test_HttpClientInjection {

    private static HttpServer server;
    private static String endpoint;

    @BeforeClass
    public static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/predict/m", Test_HttpClientInjection::handle);
        server.createContext("/api/predict/echo", Test_HttpClientInjection::handleEcho);
        server.createContext("/api/predict/trace", Test_HttpClientInjection::handleTrace);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        in.close();
        long now = System.currentTimeMillis();
        exchange.getResponseHeaders().set("Server-Timestamp", String.valueOf(now));
        exchange.getResponseHeaders().set("Enqueue-Timestamp", String.valueOf(now + 1));
        exchange.getResponseHeaders().set("Dequeue-Timestamp", String.valueOf(now + 3));
        exchange.getResponseHeaders().set("Response-Timestamp", String.valueOf(now + 6));
        exchange.getResponseHeaders().set("Queue-Length", "0");
        byte[] body = "prediction-result".getBytes("UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /** echoes the X-Echo-Tag request header into the response body */
    private static void handleEcho(HttpExchange exchange) throws IOException {
        String tag = exchange.getRequestHeaders().getFirst("X-Echo-Tag");
        InputStream in = exchange.getRequestBody();
        while (in.read(new byte[1024]) > 0) {
            // drain the request body
        }
        in.close();
        byte[] body = (tag != null ? tag : "<missing>").getBytes("UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /** echoes the x-eas-trace-id request header into the response body */
    private static void handleTrace(HttpExchange exchange) throws IOException {
        String traceId = exchange.getRequestHeaders().getFirst("x-eas-trace-id");
        InputStream in = exchange.getRequestBody();
        while (in.read(new byte[1024]) > 0) {
            // drain the request body
        }
        in.close();
        byte[] body = (traceId != null ? traceId : "<missing>").getBytes("UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private PredictClient newClient(CloseableHttpAsyncClient client) {
        HttpConfig config = new HttpConfig();
        config.setConnectTimeout(2000);
        config.setReadTimeout(5000);
        PredictClient predictClient = new PredictClient(config, client);
        predictClient.setEndpoint(endpoint);
        predictClient.setServiceName("m");
        predictClient.setRetryCount(0);
        return predictClient;
    }

    @Test
    public void testExternalClientSurvivesSdkShutdown() throws Exception {
        CloseableHttpAsyncClient external = HttpAsyncClients.custom().build();
        external.start();
        try {
            PredictClient first = newClient(external);
            assertEquals("prediction-result", new String(first.predict("hello".getBytes())));
            first.shutdown();

            // The SDK must not close an externally provided client.
            assertTrue("external client must still be running",
                    external.getStatus() == IOReactorStatus.ACTIVE);

            PredictClient second = newClient(external);
            assertEquals("prediction-result", new String(second.predict("world".getBytes())));
            second.shutdown();
            assertTrue("external client must still be running",
                    external.getStatus() == IOReactorStatus.ACTIVE);
        } finally {
            external.close();
        }
    }

    @Test
    public void testTraceIdHeader() throws Exception {
        CloseableHttpAsyncClient external = HttpAsyncClients.custom().build();
        external.start();
        try {
            HttpConfig config = new HttpConfig();
            config.setConnectTimeout(2000);
            config.setReadTimeout(5000);
            PredictClient client = new PredictClient(config, external);
            client.setEndpoint(endpoint);
            client.setServiceName("trace");
            client.setRetryCount(0);
            try {
                client.setTraceId("trace-123");
                assertEquals("trace-123", new String(client.predict("hello".getBytes())));
                assertEquals("trace-123", client.getTraceId());

                // null/empty disables the header
                client.setTraceId("");
                assertEquals("<missing>", new String(client.predict("hello".getBytes())));

                // an explicit trace id wins over a same-named extra header
                client.setTraceId("trace-123");
                client.addExtraHeaders(java.util.Collections.singletonMap("x-eas-trace-id", "from-extra"));
                assertEquals("trace-123", new String(client.predict("hello".getBytes())));
            } finally {
                client.shutdown();
            }
        } finally {
            external.close();
        }
    }

    @Test
    public void testPerCallHeadersOverrideClientLevel() throws Exception {
        CloseableHttpAsyncClient external = HttpAsyncClients.custom().build();
        external.start();
        try {
            HttpConfig config = new HttpConfig();
            config.setConnectTimeout(2000);
            config.setReadTimeout(5000);
            PredictClient client = new PredictClient(config, external);
            client.setEndpoint(endpoint);
            client.setServiceName("echo");
            client.setRetryCount(0);
            client.addExtraHeaders(java.util.Collections.singletonMap("X-Echo-Tag", "client-level"));
            try {
                // client-level header wins without CallConfig
                assertEquals("client-level", new String(client.predict("hello".getBytes())));
                // per-call headers override the client-level ones
                CallConfig callConfig = new CallConfig()
                        .setHeaders(java.util.Collections.singletonMap("X-Echo-Tag", "call-level"));
                assertEquals("call-level", new String(client.predict("hello".getBytes(), callConfig)));
            } finally {
                client.shutdown();
            }
        } finally {
            external.close();
        }
    }
}
