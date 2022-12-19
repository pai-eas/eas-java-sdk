package com.aliyun.openservices.eas.predict.queue_client;

import com.aliyun.openservices.eas.predict.http.HttpException;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import org.apache.commons.lang3.StringUtils;
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

  /** QueueClient */
  private QueueClient queueClient;
  /** websocket uri */
  private URI uri;
  /** Headers with Identity */
  private Map<String, String> headers;
  /** whether to reconnect */
  private AtomicBoolean tryReconnect;
  /** whether to ping server */
  private AtomicBoolean needPing;
  /** count of attempts to reconnect */
  private AtomicInteger reConnectCnt;
  /** connection close flag */
  private AtomicBoolean end;
  /** reconnect interval */
  private int reConnectInterval;
  /** store the data that has been watched */
  private BlockingQueue<DataFrame> dataQueue;

  public WebSocketWatcher(
      QueueClient queueClient,
      URI uri,
      Map<String, String> headers,
      int reConnectCnt,
      int reConnectInterval)
      throws Exception {
    this.queueClient = queueClient;
    this.uri = uri;
    if (headers != null && !headers.isEmpty()) {
      this.headers = new HashMap<String, String>();
      this.headers.putAll(headers);
    }
    this.reConnectCnt = new AtomicInteger(reConnectCnt);
    this.reConnectInterval = reConnectInterval;
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

  private void needReconnect() throws Exception {
    Thread.sleep(reConnectInterval * 1000);
    int cul = reConnectCnt.decrementAndGet();
    if (cul < 0) {
      close();
      throw new Exception("reconnect failed");
    }
    if (tryReconnect.get()) {
      tryReconnect.set(false);
      needReconnect();
      return;
    }
    try {
      tryReconnect.set(true);
      if (queueClient.webSocketClient.isOpen()) {
        log.info("prepare to reconnect, close old connection");
        queueClient.webSocketClient.closeConnection(2, "reconnect stop");
      }
      queueClient.webSocketClient = null;
      createWebSocketClient();
      queueClient.webSocketClient.connectBlocking();
    } catch (Exception e) {
      log.error("reconnect failed, error: " + e.getMessage());
      needReconnect();
    } finally {
      tryReconnect.set(false);
    }
  }

  private void createWebSocketClient() throws HttpException {
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
                  "WebSocketClient successfully connects to the server "
                      + getRemoteSocketAddress());
              tryReconnect.set(false);
            }

            @Override
            public void onMessage(String text) {}

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
              log.info("WebSocketClient is closed, reason = " + reason);
              if (StringUtils.isBlank(reason)) {
                if (end.get()) {
                  return;
                }
                try {
                  needReconnect();
                } catch (Exception e) {
                  log.error("WebSocketClient reconnect error, e = " + e);
                }
              }
            }

            @Override
            public void onError(Exception e) {
              log.error("WebSocketClient error, e = " + e);
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
                } finally {
                  queueClient.lock.unlock();
                }
                try {
                  Thread.sleep(2000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
              log.info("pingServer is closed");
            });
    t.setDaemon(true);
    t.start();
  }

  public DataFrame getDataFrame() throws InterruptedException {
    return dataQueue.take();
  }

  public void close() {
    try {
      queueClient.lock.lock();
      needPing.set(false);
      end.set(true);
      if (queueClient.webSocketClient != null) {
        queueClient.webSocketClient.closeConnection(3, "real stop");
      }
    } finally {
      queueClient.lock.unlock();
    }
  }
}
