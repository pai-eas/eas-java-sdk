package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.CallConfig;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.QueueUser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Protocol-level tests for {@link QueueClient} against a local JDK
 * {@link HttpServer}: verifies HTTP methods, query parameters, headers, bodies
 * and response parsing stay identical after the HttpClient 5 migration.
 */
public class Test_QueueClient {

    private static HttpServer server;
    private static String baseUrl;
    private static final AtomicInteger countHits = new AtomicInteger();

    private static final List<String> requests = new CopyOnWriteArrayList<String>();

    @BeforeClass
    public static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/predict/q", Test_QueueClient::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/predict/q";
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getRawQuery();
        String body = readBody(exchange);
        Headers requestHeaders = exchange.getRequestHeaders();
        requests.add(method + " " + exchange.getRequestURI().getPath() + " " + query + " "
                + requestHeaders.getFirst("Authorization") + " "
                + requestHeaders.getFirst(QueueClient.HeaderRedisUid) + " "
                + requestHeaders.getFirst("x-eas-trace-id") + " " + body);
        Headers headers = exchange.getResponseHeaders();

        if (query != null && query.contains("_attrs_=true")) {
            respond(exchange, 200, "{\"meta.header.priority\":\"X-Priority\"}");
        } else if (query != null && query.contains("_count_=true")) {
            if (countHits.incrementAndGet() <= 2) {
                respond(exchange, 500, "not ready");
            } else {
                respond(exchange, 200, "42");
            }
        } else if (query != null && query.contains("_indexes_=")) {
            respond(exchange, 200, "OK");
        } else if (query != null && query.contains("_trunc_=true")) {
            respond(exchange, 200, "");
        } else if (query != null && query.contains("_eos_=true")) {
            respond(exchange, 200, "");
        } else if ("POST".equals(method)) {
            headers.set(QueueClient.HeaderRequestId, "req-1");
            respond(exchange, 200, "7");
        } else {
            respond(exchange, 200, "");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        in.close();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private QueueClient newClient() {
        QueueClient client = new QueueClient(baseUrl, "my-token", new HttpConfig(), new QueueUser("uid-1", "gid-1"));
        client.setRetryCount(0);
        return client;
    }

    @Test
    public void testAttributesAndIdentityHeaders() throws Exception {
        QueueClient client = newClient();
        try {
            requests.clear();
            client.setTraceId("trace-queue-1");
            client.addExtraHeaders(Collections.<String, String>singletonMap("x-eas-trace-id", "from-extra"));
            assertEquals("X-Priority", client.attributes().getString("meta.header.priority"));
            String last = requests.get(requests.size() - 1);
            assertTrue("GET with _attrs_ expected: " + last, last.startsWith("GET "));
            assertTrue("_attrs_ expected: " + last, last.contains("_attrs_=true"));
            assertTrue("Authorization header expected: " + last, last.contains("my-token"));
            assertTrue("uid header expected: " + last, last.contains("uid-1"));
            // the explicit trace id wins over a same-named extra header
            assertTrue("trace id header expected: " + last, last.contains(" trace-queue-1 "));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void testCountAndRetry() throws Exception {
        countHits.set(0);
        QueueClient client = newClient();
        client.setRetryCount(5);
        try {
            // the server returns 500 twice, the client must retry and parse 42
            assertEquals(42L, client.count(0L, null));
            assertTrue("expected retries", countHits.get() >= 3);
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void testPutReturnsIndexAndRequestId() throws Exception {
        QueueClient client = newClient();
        try {
            requests.clear();
            Pair<Long, String> result = client.put("payload", 0L,
                    Collections.<String, String>emptyMap());
            assertEquals(Long.valueOf(7L), result.getLeft());
            assertEquals("req-1", result.getRight());
            String last = requests.get(requests.size() - 1);
            assertTrue("POST expected: " + last, last.startsWith("POST "));
            assertTrue("body expected: " + last, last.contains("payload"));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void testDeleteCommitNegative() throws Exception {
        QueueClient client = newClient();
        try {
            requests.clear();
            assertEquals("OK", client.delete(3L));
            assertEquals("OK", client.commit(3L));
            assertEquals("OK", client.negative(3L, "400", "bad"));
            boolean sawDelete = false;
            boolean sawPut = false;
            for (String request : requests) {
                if (request.startsWith("DELETE ") && request.contains("_indexes_=3")) {
                    sawDelete = true;
                }
                if (request.startsWith("PUT ") && request.contains("_negative_=true")
                        && request.contains("_code_=400") && request.contains("_reason_=bad")) {
                    sawPut = true;
                }
            }
            assertTrue("DELETE with _indexes_ expected: " + requests, sawDelete);
            assertTrue("PUT with _negative_ expected: " + requests, sawPut);
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void testExternalClientIsNotClosedByShutdown() throws Exception {
        org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient external =
                org.apache.hc.client5.http.impl.async.HttpAsyncClients.custom().build();
        external.start();
        QueueClient first = new QueueClient(baseUrl, "t", new HttpConfig(), new QueueUser(), external);
        first.setRetryCount(0);
        try {
            assertEquals("X-Priority", first.attributes().getString("meta.header.priority"));
            first.shutdown();

            // the external client must still be usable after the SDK shutdown
            QueueClient second = new QueueClient(baseUrl, "t", new HttpConfig(), new QueueUser(), external);
            second.setRetryCount(0);
            try {
                assertEquals("X-Priority", second.attributes().getString("meta.header.priority"));
            } finally {
                second.shutdown();
            }
        } finally {
            external.close();
        }
    }

    @Test
    public void testRequestPathAndCallConfig() throws Exception {
        QueueClient client = newClient();
        try {
            // client-level request path
            client.setRequestPath("/custom");
            requests.clear();
            assertEquals("X-Priority", client.attributes().getString("meta.header.priority"));
            String last = requests.get(requests.size() - 1);
            assertTrue("custom path expected: " + last, last.contains("/api/predict/q/custom"));
            assertTrue("_attrs_ expected: " + last, last.contains("_attrs_=true"));
            client.setRequestPath("");

            // per-call path, query parameter and header override
            requests.clear();
            CallConfig config = new CallConfig()
                    .setRequestPath("/submit")
                    .setQueryParams(Collections.<String, String>singletonMap("source", "demo"))
                    .setHeaders(Collections.<String, String>singletonMap("Authorization", "call-level-token"));
            Pair<Long, String> result = client.put("payload", 0L, null, config);
            assertEquals(Long.valueOf(7L), result.getLeft());
            last = requests.get(requests.size() - 1);
            assertTrue("per-call path expected: " + last, last.startsWith("POST /api/predict/q/submit"));
            assertTrue("per-call query param expected: " + last, last.contains("source=demo"));
            assertTrue("per-call header override expected: " + last, last.contains("call-level-token"));
            assertTrue("identity header must survive: " + last, last.contains("uid-1"));
        } finally {
            client.shutdown();
        }
    }
}
