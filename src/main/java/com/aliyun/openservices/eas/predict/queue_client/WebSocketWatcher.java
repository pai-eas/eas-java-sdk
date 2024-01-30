package com.aliyun.openservices.eas.predict.queue_client;

import com.aliyun.openservices.eas.predict.http.HttpException;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketWatcher {
    private static Log log = LogFactory.getLog(WebSocketWatcher.class);

    /**
     * QueueClient
     */
    private QueueClient queueClient;
    /**
     * websocket uri
     */
    private URI uri;
    /**
     * Headers with Identity
     */
    private Map<String, String> headers;
    /**
     * whether to reconnect
     */
    private AtomicBoolean tryReconnect;
    /**
     * whether to ping server
     */
    private AtomicBoolean needPing;
    /**
     * whether unlimited reConnect
     */
    private boolean unlimitedReConnect;
    /**
     * count of attempting to reconnect
     */
    private AtomicInteger reConnectTimes;
    /**
     * max count of attempting to reconnect
     */
    private int maxReConnectCnt;
    /**
     * connection close flag
     */
    private AtomicBoolean end;
    /**
     * reconnect interval
     */
    private int reConnectInterval;
    /**
     * store the data that has been watched
     */
    private BlockingQueue<DataFrame> dataQueue;

    private Exception error;

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
        }
        this.unlimitedReConnect = watchConfig.isUnLimitedReCon();
        this.maxReConnectCnt = watchConfig.getReConCnt();
        this.reConnectTimes = new AtomicInteger(0);
        this.reConnectInterval = watchConfig.getReConInterval();
        this.needPing = new AtomicBoolean(true);
        this.tryReconnect = new AtomicBoolean(false);
        this.end = new AtomicBoolean(false);
        this.dataQueue = new LinkedBlockingQueue<>(100);
        // create ws client
        createWebSocketClient();
        // ping
        pingServer();
        // connect
        this.queueClient.webSocketClient.connectBlocking();
    }

    private void needReconnect(String errorMessage) throws Exception {
        while (true) {
            Thread.sleep(reConnectInterval * 1000);

            if (tryReconnect.get()) {
                log.warn("WebSocketClient Failed to Connect to Server");
                break;
            }
            int cur = reConnectTimes.incrementAndGet();
            if (!unlimitedReConnect) {
                if (cur > this.maxReConnectCnt) {
                    close();
                    log.error("WebSocketClient Re-Connect Failed, Exhausted maxReConnectCnt: " + this.maxReConnectCnt + ", error = " + errorMessage);
                    throw new Exception(errorMessage);
                }
            }

            try {
                tryReconnect.set(true);
                if (queueClient.webSocketClient.isOpen()) {
                    log.warn("Prepare to Re-Connect, Close Existing WebSocket Connection");
                    queueClient.webSocketClient.closeConnection(1000, "Re-Connect Stop");
                }
                queueClient.webSocketClient = null;
                createWebSocketClient();
                if (queueClient.webSocketClient.connectBlocking()) {
                    break;
                }
            } catch (Exception e) {
                log.warn("WebSocketClient Re-Connect Error, Error: " + e.getMessage());
            } finally {
                tryReconnect.set(false);
            }
        }
    }

    private void createWebSocketClient() throws Exception {
        try {
            queueClient.lock.lock();
            if (queueClient.webSocketClient != null) {
                throw new HttpException(400, "Another watcher is already running");
            }
            queueClient.webSocketClient =
                new WebSocketClient(uri, headers) {
                    @Override
                    public void onOpen(ServerHandshake serverHandshake) {
                        log.info(
                            "WebSocketClient Successfully Connects to Server: "
                                + getRemoteSocketAddress());
                        reConnectTimes.set(0);
                        tryReconnect.set(false);
                    }

                    @Override
                    public void onMessage(String text) {
                    }

                    @Override
                    public void onMessage(ByteBuffer bytes) {
                        try {
                            dataQueue.put(new DataFrame().decode(bytes));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        log.warn(String.format("WebSocketClient is Closed, Code: %d, Reason: %s, Re-Connect times: %d", code, reason, reConnectTimes.get()));
                        if (end.get()) {
                            return;
                        }
                        if (tryReconnect.get() == false) {
                            try {
                                needReconnect(reason);
                            } catch (Exception e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        try {
                            dataQueue.put(new DataFrame(e));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
        } finally {
            queueClient.lock.unlock();
        }
    }

    public void pingServer() {
        Thread t =
            new Thread(
                () -> {
                    while (needPing.get()) {
                        try {
                            queueClient.lock.lock();
                            if (queueClient.webSocketClient != null && queueClient.webSocketClient.isOpen()) {
                                queueClient.webSocketClient.sendPing();
                            }
                        } catch (Exception e) {
                            log.warn("PingServer Error, error = " + e.getMessage());
                        } finally {
                            queueClient.lock.unlock();
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("PingServer is Closed");
                });
        t.setDaemon(true);
        t.start();
    }

    public DataFrame getDataFrame() throws Exception {
        DataFrame df = dataQueue.take();
        if (df.getError() != null) {
            throw df.getError();
        }
        return df;
    }

    public void close() {
        try {
            queueClient.lock.lock();
            needPing.set(false);
            end.set(true);
            if (queueClient.webSocketClient != null) {
                queueClient.webSocketClient.closeConnection(1000, "Real Stop");
            }
        } finally {
            queueClient.lock.unlock();
        }
    }
}
