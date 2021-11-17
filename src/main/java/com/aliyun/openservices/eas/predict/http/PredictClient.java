package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.request.*;
import com.aliyun.openservices.eas.predict.response.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by xiping.zk on 2018/07/25.
 */

class BlacklistData{
    private long timestamp = 0L;
    private int count = 0;

    public BlacklistData(long timestamp, int count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}

// define blacklist task
class BlacklistTask implements Runnable {
    private Map<String, BlacklistData> blacklist = null;
    private ReentrantReadWriteLock rwlock = null;
    private int blacklistTimeout = 0;
    private static Log log = LogFactory.getLog(BlacklistTask.class);

    public BlacklistTask(Map<String, BlacklistData> blacklist,
                         ReentrantReadWriteLock rwlock, int blacklistTimeout) {
        this.blacklist = blacklist;
        this.rwlock = rwlock;
        this.blacklistTimeout = blacklistTimeout;
    }

    @Override
    public void run() {
        while (true) {
            try {
                rwlock.writeLock().lock();
                Iterator<Map.Entry<String, BlacklistData>> it = blacklist.entrySet().iterator();
                long currentTimestamp = System.currentTimeMillis();
                while (it.hasNext()) {
                    Map.Entry<String, BlacklistData> entry = it.next();
                    if (entry.getValue().getTimestamp() <= currentTimestamp) {
                        log.info("Remove [" + entry.getKey() + "] from blacklist");
                        it.remove();
                    }
                }
                rwlock.writeLock().unlock();
                Thread.sleep(blacklistTimeout * 1000);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

public class PredictClient {
    private static Log log = LogFactory.getLog(PredictClient.class);
    final private int endpointRetryCount = 10;
    private HashMap<String, String> mapHeader = null;
    private CloseableHttpAsyncClient httpclient = null;
    private String token = null;
    private String modelName = null;
    private String endpoint = null;
    private boolean isCompressed = false;
    private int retryCount = 3;
    private String contentType = "application/octet-stream";
    private int errorCode = 400;
    private String errorMessage;
    private String vipSrvEndPoint = null;
    private String directEndPoint = null;
    private int requestTimeout = 0;
    private boolean enableBlacklist = false;
    private int blacklistSize = 10;
    private int blacklistTimeout = 30;
    private int blacklistTimeoutCount = 10;
    private Map<String, BlacklistData> blacklist = null;
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    public PredictClient() {
    }

    public PredictClient(HttpConfig httpConfig) {
        try {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
            PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
                    ioReactor);
            cm.setMaxTotal(httpConfig.getMaxConnectionCount());
            cm.setDefaultMaxPerRoute(httpConfig.getMaxConnectionPerRoute());
            requestTimeout = httpConfig.getRequestTimeout();
            IOReactorConfig config = IOReactorConfig.custom()
                    .setTcpNoDelay(true)
                    .setSoTimeout(httpConfig.getReadTimeout())
                    .setSoReuseAddress(true)
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setIoThreadCount(httpConfig.getIoThreadNum())
                    .setSoKeepAlive(httpConfig.isKeepAlive()).build();
            final RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setSocketTimeout(httpConfig.getReadTimeout()).build();
            httpclient = HttpAsyncClients.custom().setConnectionManager(cm)
                    .setDefaultIOReactorConfig(config)
                    .setDefaultRequestConfig(requestConfig).build();
            httpclient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PredictClient setHttp(CloseableHttpAsyncClient httpclient) {
        this.httpclient = httpclient;
        return this;
    }

    public PredictClient setToken(String token) {
        if (token == null || token.length() > 0) {
            this.token = token;
        }
        return this;
    }

    public PredictClient setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public PredictClient setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public PredictClient setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public PredictClient setVIPServer(String vipSrvEndPoint) {
        if (vipSrvEndPoint == null || vipSrvEndPoint.length() > 0) {
            this.vipSrvEndPoint = vipSrvEndPoint;
        }
        return this;
    }

    public PredictClient setDirectEndpoint(String directEndpoint) {
        if (directEndPoint == null || directEndPoint.length() > 0) {
            this.directEndPoint = directEndpoint;
            System.setProperty("com.aliyun.eas.discovery", directEndpoint);
        }
        return this;
    }

    public PredictClient setIsCompressed(boolean isCompressed) {
        this.isCompressed = isCompressed;
        return this;
    }

    public PredictClient setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public PredictClient setTracing(HashMap<String, String> mapHeader) {
        this.mapHeader = mapHeader;
        return this;
    }

    public PredictClient setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public PredictClient startBlacklistMechanism(int blacklistSize,
                                                 int blacklistTimeout,
                                                 int blacklistTimeoutCount) {
        this.enableBlacklist = true;
        this.blacklistSize = blacklistSize;
        this.blacklistTimeout = blacklistTimeout;
        this.blacklistTimeoutCount = blacklistTimeoutCount;
        this.blacklist = new HashMap<String, BlacklistData>();
        BlacklistTask task = new BlacklistTask(this.blacklist,
                this.rwlock, this.blacklistTimeout);
        Thread t = new Thread(task);
        t.start();
        return this;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public PredictClient createChildClient(String token, String endPoint,
                                           String modelName) {
        PredictClient client = new PredictClient();
        client.setHttp(this.httpclient).setToken(token).setEndpoint(endPoint)
                .setModelName(modelName);
        return client;
    }

    // to be compatible with old version typo
    public PredictClient createChlidClient(String token, String endPoint,
                                           String modelName) {
        return createChildClient(token, endpoint, modelName);
    }

    public PredictClient createChildClient() {
        PredictClient client = new PredictClient();
        client.setHttp(this.httpclient)
                .setToken(this.token)
                .setModelName(this.modelName)
                .setRetryCount(this.retryCount)
                .setRequestTimeout(this.requestTimeout);
        if (this.vipSrvEndPoint != null) {
            client.setVIPServer(this.vipSrvEndPoint);
        } else if (this.directEndPoint != null) {
            client.setDirectEndpoint(this.directEndPoint);
        } else {
            client.setEndpoint(this.endpoint);
        }
        return client;
    }

    // to be compatible with old version typo
    public PredictClient createChlidClient() {
        return createChildClient();
    }

    private String getUrl(String lastUrl) throws Exception {
        String endpoint = this.endpoint;
        String url = "";
        if (enableBlacklist) {
            int retryCount = endpointRetryCount;
            if (blacklistSize * 2 > endpointRetryCount) {
                retryCount = blacklistSize * 2;
            }
            for (int i = 0; i < retryCount; i++) {
                if (directEndPoint != null) {
                    endpoint = DiscoveryClient.srvHost(this.modelName).toInetAddr();
                    url = "http://" + endpoint + "/api/predict/" + modelName;
                    // System.out.println("URL: " + url + " LastURL: " + lastUrl);
                    if (DiscoveryClient.getHosts(this.modelName).size() < 2) {
                        return url;
                    }
                    try {
                        rwlock.readLock().lock();
                        if (!url.equals(lastUrl)) {
                            if (!blacklist.containsKey(url)) {
                                return url;
                            } else if (blacklist.get(url).getCount() < blacklistTimeoutCount) {
                                return url;
                            }
                        }
                    } finally {
                        rwlock.readLock().unlock();
                    }
                } else {
                    url = "http://" + endpoint + "/api/predict/" + modelName;
                    break;
                }
            }
        } else {
            for (int i = 0; i < endpointRetryCount; i++) {
                if (directEndPoint != null) {
                    endpoint = DiscoveryClient.srvHost(this.modelName).toInetAddr();
                    url = "http://" + endpoint + "/api/predict/" + modelName;
                    // System.out.println("URL: " + url + " LastURL: " + lastUrl);
                    if (DiscoveryClient.getHosts(this.modelName).size() < 2) {
                        return url;
                    }
                    if (!url.equals(lastUrl)) {
                        return url;
                    }
                } else {
                    url = "http://" + endpoint + "/api/predict/" + modelName;
                    break;
                }
            }
        }

        return url;
    }

    private HttpPost generateSignature(byte[] requestContent, String lastUrl) throws Exception {
        HttpPost request = new HttpPost(getUrl(lastUrl));
        request.setEntity(new NByteArrayEntity(requestContent));
        if (isCompressed) {
            try {
                requestContent = Snappy.compress(requestContent);
            } catch (IOException e) {
                log.error("Compress Error", e);
            }
        }
        HmacSha1Signature signature = new HmacSha1Signature();
        String md5Content = signature.getMD5(requestContent);
        request.addHeader(HttpHeaders.CONTENT_MD5, md5Content);
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentTime = dateFormat.format(now) + " GMT";
        request.addHeader(HttpHeaders.DATE, currentTime);
        request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);

        if (mapHeader != null) {
            request.addHeader("Client-Timestamp",
                    String.valueOf(System.currentTimeMillis()));
        }

        if (token != null) {
            String auth = "POST" + "\n" + md5Content + "\n"
                    + "application/octet-stream" + "\n" + currentTime + "\n"
                    + "/api/predict/" + modelName;
            request.addHeader(HttpHeaders.AUTHORIZATION,
                    "EAS " + signature.computeSignature(token, auth));
        }
        return request;
    }

    private byte[] getContent(HttpPost request) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {
        byte[] content = null;
        HttpResponse response = null;

        Future<HttpResponse> future = httpclient.execute(request, null);
        if (requestTimeout > 0) {
            response = future.get(requestTimeout, TimeUnit.MILLISECONDS);
        } else {
            response = future.get();
        }

        if (mapHeader != null) {
            Header[] header = response.getAllHeaders();
            for (int i = 0; i < header.length; i++) {
                mapHeader.put(header[i].getName(), header[i].getValue());
            }
        }
        if (future.isDone()) {
            try {
                errorCode = response.getStatusLine().getStatusCode();
                errorMessage = "";

                if (errorCode == 200) {
                    content = IOUtils.toByteArray(response.getEntity()
                            .getContent());
                    if (isCompressed) {
                        content = Snappy.uncompress(content);
                    }
                } else {
                    errorMessage = IOUtils.toString(response.getEntity()
                            .getContent(), "UTF-8");
                    throw new HttpException(errorCode, errorMessage);
                }
            } catch (IllegalStateException e) {
                log.error("Illegal State", e);
            }
        } else if (future.isCancelled()) {
            log.error("request cancelled!", new Exception("Request cancelled"));
        } else {
            throw new HttpException(-1, "request failed!");
        }
        return content;
    }

    public BladeResponse predict(BladeRequest runRequest) throws Exception {
        BladeResponse runResponse = new BladeResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public TFResponse predict(TFRequest runRequest) throws Exception {
        TFResponse runResponse = new TFResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public CaffeResponse predict(CaffeRequest runRequest) throws Exception {
        CaffeResponse runResponse = new CaffeResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public JsonResponse predict(JsonRequest requestContent)
            throws Exception {
        byte[] result = predict(requestContent.getJSON().getBytes());
        JsonResponse jsonResponse = new JsonResponse();
        if (result != null) {
            jsonResponse.setContentValues(result);
        }
        return jsonResponse;
    }

    public TorchResponse predict(TorchRequest runRequest) throws Exception {
        TorchResponse runResponse = new TorchResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public String predict(String requestContent) throws Exception {
        byte[] result = predict(requestContent.getBytes());
        if (result != null) {
            return new String(result);
        }
        return null;
    }

    private void handleBlacklist(String key) {
        if (blacklist.containsKey(key)) {
            int timeoutCount = blacklist.get(key).getCount();
            if (timeoutCount < blacklistTimeoutCount) {
                blacklist.get(key).setCount(timeoutCount + 1);
                log.info("Set [" + key + "] timeoutCount:"
                        + blacklist.get(key).getCount());
            } else {
                long expirationTimestamp =
                        System.currentTimeMillis() + blacklistTimeout * 1000;
                blacklist.get(key).setTimestamp(expirationTimestamp);
                log.info("Set [" + key + "] timestamp: " +
                        blacklist.get(key).getTimestamp()
                        + " timeoutCount: " + blacklist.get(key).getCount());
            }
        } else {
            if (blacklist.size() < blacklistSize) {
                long expirationTimestamp =
                        System.currentTimeMillis() + blacklistTimeout * 1000;
                blacklist.put(key, new BlacklistData(expirationTimestamp, 1));
                log.info("Put [" + key + "] into blacklist");
            }
        }
    }

    public byte[] predict(byte[] requestContent) throws Exception{
        byte[] content = null;
        String lastUrl = "";
        for (int i = 0; i <= retryCount; i++) {
            try {
                HttpPost request = generateSignature(requestContent, lastUrl);
                lastUrl = request.getURI().toString();
                content = getContent(request);
                break;
            } catch (ConnectTimeoutException e) {
                String errorMessage = "URL: " + lastUrl + ", " + e.getMessage();
                if (enableBlacklist) {
                    rwlock.writeLock().lock();
                    handleBlacklist(lastUrl);
                    rwlock.writeLock().unlock();
                }
                if (i == retryCount) {
                    log.error(errorMessage);
                    throw new Exception(errorMessage);
                } else {
                    log.debug(errorMessage);
                }
            } catch (Exception e) {
                String errorMesssage = "URL: " + lastUrl + ", " + e.getMessage();
                if (i == retryCount) {
                    log.error(errorMesssage);
                    e.printStackTrace();
                    throw new Exception(errorMesssage);
                } else {
                    log.debug(errorMesssage);
                }
            }
        }

        return content;
    }

    public void shutdown() {
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
