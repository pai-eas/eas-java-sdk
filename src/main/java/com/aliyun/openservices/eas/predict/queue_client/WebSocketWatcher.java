package com.aliyun.openservices.eas.predict.queue_client;

import com.aliyun.openservices.eas.predict.http.HttpException;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.net.URIBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketWatcher {
    private static final Log log = LogFactory.getLog(WebSocketWatcher.class);

    /** Bounded wait for the initial connection and each reconnect attempt. */
    private static final long CONNECT_TIMEOUT_SECONDS = 30;

    /** Reconnect attempts are reset only after a connection stayed up this long. */
    private static final long STABLE_CONNECTION_MILLIS = 60_000;

    private static final int DATA_QUEUE_CAPACITY = 1024;

    private final QueueClient queueClient;
    private final URI uri;
    private final Map<String, String> headers;
    private final AtomicBoolean tryReconnect;
    private final AtomicBoolean needPing;
    private final boolean unlimitedReConnect;
    private final int maxReConnectCnt;
    private final AtomicInteger reConnectTimes;
    private final int reConnectInterval;
    private final AtomicBoolean end;
    private final AtomicBoolean reconnecting;
    private final BlockingQueue<DataFrame> dataQueue;
    private final AtomicLong lastIndex;
    private final AtomicLong lastOpenTime;

    private volatile Exception terminalError;
    private volatile Thread pingThread;

    public WebSocketWatcher(
        QueueClient queueClient,
        URI uri,
        Map<String, String> headers,
        WatchConfig watchConfig)
        throws Exception {
        this.queueClient = queueClient;
        this.uri = uri;
        if (headers != null && !headers.isEmpty()) {
            this.headers = new HashMap<String, String>();
            this.headers.putAll(headers);
        } else {
            this.headers = new HashMap<String, String>();
        }
        this.unlimitedReConnect = watchConfig.isUnLimitedReCon();
        this.maxReConnectCnt = watchConfig.getReConCnt();
        this.reConnectTimes = new AtomicInteger(0);
        this.reConnectInterval = Math.max(1, watchConfig.getReConInterval());
        this.needPing = new AtomicBoolean(true);
        this.tryReconnect = new AtomicBoolean(false);
        this.end = new AtomicBoolean(false);
        this.reconnecting = new AtomicBoolean(false);
        this.dataQueue = new LinkedBlockingQueue<>(DATA_QUEUE_CAPACITY);
        this.lastIndex = new AtomicLong(parseInitialIndex(uri));
        this.lastOpenTime = new AtomicLong(0L);

        queueClient.registerWatcher(this);
        try {
            createWebSocketClient();
            startPing();
            WebSocketClient client = queueClient.webSocketClient;
            if (client == null
                    || !client.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new HttpException(-1,
                        "WebSocket connect timed out after " + CONNECT_TIMEOUT_SECONDS + "s");
            }
        } catch (Exception e) {
            fail(e);
            throw e;
        }
    }

    private static long parseInitialIndex(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) {
            return -1L;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if ("_index_".equals(pair.substring(0, eq))) {
                try {
                    return Long.parseLong(pair.substring(eq + 1));
                } catch (NumberFormatException ignored) {
                    return -1L;
                }
            }
        }
        return -1L;
    }

    private void startPing() {
        Thread thread = new Thread(() -> {
            while (needPing.get()) {
                try {
                    queueClient.lock.lock();
                    try {
                        WebSocketClient client = queueClient.webSocketClient;
                        if (client != null && client.isOpen()) {
                            client.sendPing();
                        }
                    } finally {
                        queueClient.lock.unlock();
                    }
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    log.warn("PingServer Error: " + e.getMessage());
                }
            }
            log.debug("PingServer is Closed");
        }, "eas-queue-ws-ping");
        thread.setDaemon(true);
        this.pingThread = thread;
        thread.start();
    }

    private URI reconnectUri() {
        long index = lastIndex.get();
        if (index < 0) {
            return uri;
        }
        try {
            return new URIBuilder(uri)
                    .setParameter("_index_", Long.toString(index))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to build reconnect URI, using original: " + e.getMessage());
            return uri;
        }
    }

    private void needReconnect(String errorMessage) {
        while (!end.get()) {
            try {
                Thread.sleep(reConnectInterval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (end.get()) {
                    return;
                }
            }
            if (end.get()) {
                return;
            }
            int current = reConnectTimes.incrementAndGet();
            if (!unlimitedReConnect && current > maxReConnectCnt) {
                fail(new Exception("WebSocket re-connect failed after " + maxReConnectCnt
                        + " attempts, url: " + uri + ", error: " + errorMessage));
                return;
            }

            try {
                tryReconnect.set(true);
                closeCurrentSocket();
                createWebSocketClient();
                WebSocketClient client = queueClient.webSocketClient;
                if (client != null
                        && client.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.info("WebSocketClient re-connected: " + uri);
                    return;
                }
            } catch (Exception e) {
                log.warn("WebSocketClient re-connect error, url: " + uri + ", error: " + e);
            } finally {
                tryReconnect.set(false);
            }
        }
    }

    private void startReconnect(final String reason) {
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                needReconnect(reason);
            } catch (Exception e) {
                fail(e);
            } finally {
                reconnecting.set(false);
            }
        }, "eas-queue-ws-reconnect");
        thread.setDaemon(true);
        thread.start();
    }

    private void createWebSocketClient() throws Exception {
        queueClient.lock.lock();
        try {
            if (queueClient.webSocketClient != null) {
                throw new HttpException(400, "Another watcher is already running");
            }
            queueClient.webSocketClient = new WebSocketClient(reconnectUri(), headers) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    log.info(String.format("WebSocketClient connected to %s, url: %s",
                            getRemoteSocketAddress(), this.uri));
                    long now = System.currentTimeMillis();
                    long previousOpen = lastOpenTime.getAndSet(now);
                    // Do not reset the counter on a flapping connection that
                    // opens and immediately closes again.
                    if (previousOpen == 0L || now - previousOpen > STABLE_CONNECTION_MILLIS) {
                        reConnectTimes.set(0);
                    }
                    tryReconnect.set(false);
                }

                @Override
                public void onMessage(String text) {
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    DataFrame frame;
                    try {
                        frame = new DataFrame().decode(bytes);
                    } catch (Exception e) {
                        enqueue(new DataFrame(e));
                        return;
                    }
                    long index = frame.getIndex();
                    lastIndex.accumulateAndGet(index, Math::max);
                    enqueue(frame);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn(String.format(
                            "WebSocketClient closed, url: %s, code: %d, reason: %s, stopped: %b",
                            this.uri, code, reason, end.get()));
                    if (!end.get() && !tryReconnect.get()) {
                        startReconnect(reason);
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Keep the error for the final failure only. Enqueuing it
                    // here would leave stale errors from failed reconnect
                    // attempts at the head of the consumer queue.
                    terminalError = e;
                    log.warn("WebSocketClient error: " + e.getMessage());
                }
            };
        } finally {
            queueClient.lock.unlock();
        }
    }

    /**
     * Enqueues a frame with bounded backpressure. The WebSocket IO thread may
     * wait for a slow consumer, but it checks the close flag regularly and
     * therefore cannot be blocked forever by close().
     */
    private void enqueue(DataFrame frame) {
        while (!end.get()) {
            try {
                if (dataQueue.offer(frame, 200, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Dropping WebSocket frame because the watcher is closed");
    }

    private void closeCurrentSocket() {
        queueClient.lock.lock();
        try {
            WebSocketClient client = queueClient.webSocketClient;
            queueClient.webSocketClient = null;
            if (client != null) {
                client.close();
            }
        } finally {
            queueClient.lock.unlock();
        }
    }

    private void fail(Exception error) {
        if (terminalError == null) {
            terminalError = error;
        }
        log.error("WebSocket watcher failed: " + error.getMessage(), error);
        close();
    }

    public DataFrame getDataFrame() throws Exception {
        while (true) {
            DataFrame frame = dataQueue.poll(200, TimeUnit.MILLISECONDS);
            if (frame != null) {
                if (frame.getError() != null) {
                    throw frame.getError();
                }
                return frame;
            }
            if (end.get()) {
                // Drain frames that were already delivered before the close.
                frame = dataQueue.poll();
                if (frame != null) {
                    if (frame.getError() != null) {
                        throw frame.getError();
                    }
                    return frame;
                }
                if (terminalError != null) {
                    throw terminalError;
                }
                throw new Exception("WebSocketClient Closed");
            }
        }
    }

    public boolean isOpen() {
        WebSocketClient client = queueClient.webSocketClient;
        return client != null && client.isOpen();
    }

    public long getLastIndex() {
        return lastIndex.get();
    }

    public void close() {
        boolean firstClose = end.compareAndSet(false, true);
        if (!firstClose) {
            return;
        }
        needPing.set(false);
        Thread pinger = pingThread;
        if (pinger != null) {
            pinger.interrupt();
        }
        closeCurrentSocket();
        queueClient.unregisterWatcher(this);
        log.info("WebSocketClient closed");
    }
}
