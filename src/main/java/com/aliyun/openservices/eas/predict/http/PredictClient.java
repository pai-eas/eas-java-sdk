package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.proto.EasyRecPredictProtos;
import com.aliyun.openservices.eas.predict.proto.TorchRecPredictProtos;
import com.aliyun.openservices.eas.predict.request.*;
import com.aliyun.openservices.eas.predict.response.*;
import com.aliyun.openservices.eas.predict.utils.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpConnectionFactory;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.conn.SchemePortResolver;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
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
    private PoolingNHttpClientConnectionManager cm = null;
    private ScheduledExecutorService scheduler = null;

    private String token = null;
    private String modelName = null;
    private String requestPath = "";
    private String endpoint = null;
    private String url = null;
    private boolean isCompressed = false;
    private int retryCount = 3;
    private EnumSet<RetryCondition> retryConditions = EnumSet.noneOf(RetryCondition.class);
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
    private Compressor compressor = null;
    private Compressor decompressor = null;
    private Map<String, String> extraHeaders = new HashMap<>();

    public PredictClient() {
    }

    public PredictClient(HttpConfig httpConfig) {
        try {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
            Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", SSLIOSessionStrategy.getDefaultStrategy())
                    .build();
            PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
                ioReactor, (NHttpConnectionFactory)null, sessionStrategyRegistry,(SchemePortResolver)null, (DnsResolver)null,
                httpConfig.getConnTimeToLive(), TimeUnit.MILLISECONDS);
            cm.setMaxTotal(httpConfig.getMaxConnectionCount());
            cm.setDefaultMaxPerRoute(httpConfig.getMaxConnectionPerRoute());

            requestTimeout = httpConfig.getRequestTimeout();
            IOReactorConfig config = IOReactorConfig.custom()
                    .setTcpNoDelay(true)
                    .setSoTimeout(httpConfig.getReadTimeout())
                    .setSoReuseAddress(true)
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setIoThreadCount(httpConfig.getIoThreadNum())
                    .setSoKeepAlive(httpConfig.isKeepAlive())
                    .build();
            final RequestConfig requestConfig = RequestConfig.custom()
                    .setRedirectsEnabled(httpConfig.getRedirectsEnabled())
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setSocketTimeout(httpConfig.getReadTimeout())
                    .build();

            httpclient = HttpAsyncClients.custom()
                .setConnectionManager(cm)
                .setDefaultIOReactorConfig(config)
                .setDefaultRequestConfig(requestConfig)
                .build();
            httpclient.start();

            int cleanupInterval = httpConfig.getConnectionCleanupInterval();
            int idleTimeout = httpConfig.getIdleConnectionTimeout();
            if (cleanupInterval > 0) {
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    // Close all expired connections
                    cm.closeExpiredConnections();
                    if (idleTimeout > 0) {
                        // Close all connections idle for longer than idleTimeout milliseconds
                        cm.closeIdleConnections(idleTimeout, TimeUnit.MILLISECONDS);
                    }
                }, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error initializing PredictClient", e);
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
        if (endpoint != null && !endpoint.startsWith("http://") && !endpoint.startsWith("https://")){
            this.endpoint = "http://" + endpoint;
        } else {
            this.endpoint = endpoint;
        }
        return this;
    }

    public PredictClient setUrl(String url) {
        this.url = url;
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
    public PredictClient setCompressor(Compressor compressor) {
        this.compressor = compressor;
        return this;
    }
    
    /**
     * Set decompressor for response data
     * @param decompressor The decompression algorithm to use
     * @return PredictClient instance
     */
    public PredictClient setDecompressor(Compressor decompressor) {
        this.decompressor = decompressor;
        return this;
    }

    public PredictClient setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public PredictClient setRetryConditions(EnumSet<RetryCondition> retryConditions) {
        this.retryConditions = retryConditions;
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

    public String getRequestPath() {
        return requestPath;
    }

    public PredictClient setRequestPath(String requestPath) {
        if (requestPath == null) {
            return this;
        }
        if (requestPath.length() > 0 && requestPath.charAt(0) != '/') {
            requestPath = "/" + requestPath;
        }
        this.requestPath = requestPath;
        return this;
    }

    public PredictClient addExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders.putAll(extraHeaders);
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
            .setRetryConditions(this.retryConditions)
            .setRequestTimeout(this.requestTimeout)
            .setIsCompressed(this.isCompressed)
            .setContentType(this.contentType)
            .setRequestPath(this.requestPath)
            .setUrl(this.url)
            .addExtraHeaders(this.extraHeaders);
        if (this.vipSrvEndPoint != null) {
            client.setVIPServer(this.vipSrvEndPoint);
        } else if (this.directEndPoint != null) {
            client.setDirectEndpoint(this.directEndPoint);
        } else {
            client.setEndpoint(this.endpoint);
        }
        if (this.compressor != null) {
            client.setCompressor(this.compressor);
        }
        if (this.decompressor != null) {
            client.setDecompressor(this.decompressor);
        }
        return client;
    }

    // to be compatible with old version typo
    public PredictClient createChlidClient() {
        return createChildClient();
    }

    private String getUrl(String lastUrl) throws Exception {
        if (this.url != null) {
            return this.url + this.requestPath;
        }
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
                    url = "http://" + endpoint + "/api/predict/" + modelName + requestPath;
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
                    url = endpoint + "/api/predict/" + modelName + requestPath;
                    break;
                }
            }
        } else {
            for (int i = 0; i < endpointRetryCount; i++) {
                if (directEndPoint != null) {
                    endpoint = DiscoveryClient.srvHost(this.modelName).toInetAddr();
                    url = "http://" + endpoint + "/api/predict/" + modelName + requestPath;
                    // System.out.println("URL: " + url + " LastURL: " + lastUrl);
                    if (DiscoveryClient.getHosts(this.modelName).size() < 2) {
                        return url;
                    }
                    if (!url.equals(lastUrl)) {
                        return url;
                    }
                } else {
                    url = endpoint + "/api/predict/" + modelName + requestPath;
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
        for (Map.Entry<String, String> entry : this.extraHeaders.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
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

        if (this.token != null) {
            String auth = "POST" + "\n" + md5Content + "\n"
                + this.contentType + "\n" + currentTime + "\n";
            if (this.url == null) {
                auth = auth + "/api/predict/" + this.modelName + this.requestPath;
            } else {
                URL u = new URL(this.url);
                auth = auth + u.getPath() + this.requestPath;
            }
            request.addHeader(HttpHeaders.AUTHORIZATION,
                "EAS " + signature.computeSignature(token, auth));
        }
        return request;
    }

    private byte[] handleResponse(HttpResponse response) throws IOException, HttpException {
        byte[] content;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            content = IOUtils.toByteArray(response.getEntity().getContent());
            if (isCompressed) {
                content = Snappy.uncompress(content);
            } else if (decompressor != null) {
                content = decompressContent(content, response);
            }
        } else {
            String errorMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            throw new HttpException(statusCode, errorMsg);
        }
        return content;
    }

    /**
     * Decompress content based on manually set decompressor
     * @param content The compressed content
     * @param response HTTP response object
     * @return Decompressed content or original content if decompression fails
     */
    private byte[] decompressContent(byte[] content, HttpResponse response) {
        try {
            switch (decompressor) {
                case Gzip:
                    return GzipUtils.decompressToBytes(content);
                case Zlib:
                    return ZlibUtils.decompress(content);
                case Snappy:
                    return SnappyUtils.decompressToBytes(content);
                case LZ4:
                    Header lz4OriginalLength = response.getFirstHeader("X-LZ4-Original-Length");
                    if (lz4OriginalLength != null) {
                        try {
                            int originalLength = Integer.parseInt(lz4OriginalLength.getValue());
                            return LZ4Utils.decompress(content, originalLength);
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse X-LZ4-Original-Length header: " + e.getMessage());
                        }
                    }
                    // If no original length header, return uncompressed content
                    return content;
                case LZ4Frame:
                    return LZ4Utils.decompressFrame(content);
                case Zstd:
                    return ZstdUtils.decompress(content);
                case Auto:
                    return autoDecompressContent(content, response);
                default:
                    return content;
            }
        } catch (Exception e) {
            log.warn("Decompression failed, returning original content. err: " + e.getMessage());
            return content;
        }
    }

    /**
     * Automatically decompress content based on Content-Encoding header
     * @param content The compressed content
     * @param response HTTP response object
     * @return Decompressed content
     * @throws IOException
     */
    private byte[] autoDecompressContent(byte[] content, HttpResponse response) throws IOException {
        Header contentEncodingHeader = response.getFirstHeader("Content-Encoding");
        if (contentEncodingHeader == null) {
            return content;
        }

        String contentEncoding = contentEncodingHeader.getValue().toLowerCase();
        switch (contentEncoding) {
            case "gzip":
                return GzipUtils.decompressToBytes(content);
            case "zlib":
            case "deflate":
                return ZlibUtils.decompress(content);
            case "snappy":
                return Snappy.uncompress(content);
            case "zstd":
                return ZstdUtils.decompress(content);
            case "lz4":
                return LZ4Utils.decompressFrame(content);
            default:
                log.warn("Unsupported Content-Encoding: " + contentEncoding);
                return content;
        }
    }

    private byte[] getContent(HttpPost request) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {
        byte[] content = null;
        HttpResponse response = null;

        Future<HttpResponse> future = httpclient.execute(request, null);
        try {
            if (requestTimeout > 0) {
                response = future.get(requestTimeout, TimeUnit.MILLISECONDS);
            } else {
                response = future.get();
            }
        } catch (ExecutionException e) {
            throw e;
        }

        if (mapHeader != null) {
            Header[] header = response.getAllHeaders();
            for (int i = 0; i < header.length; i++) {
                mapHeader.put(header[i].getName(), header[i].getValue());
            }
        }
        if (future.isDone()) {
            try {
                content = handleResponse(response);
            } catch (HttpException e) {
                throw e;
            } catch (Exception e) {
                log.error("handle response error:", e);
                throw e;
            }
        } else if (future.isCancelled()) {
            log.error("request cancelled!", new Exception("Request cancelled"));
        } else {
            throw new HttpException(-1, "request failed!");
        }
        return content;
    }


    private boolean shouldRetry(Exception e) {
        // Always need retry if there are no specific retryConditions
        if (retryConditions.isEmpty()) {
            return true;
        }

        if (e instanceof HttpException) {
            int statusCode = ((HttpException) e).getCode();
            if (retryConditions.contains(RetryCondition.RESPONSE_4XX) && statusCode / 100 == 4) {
                return true;
            }
            if (retryConditions.contains(RetryCondition.RESPONSE_5XX) && statusCode / 100 == 5) {
                return true;
            }
        }

        Throwable cause = e.getCause();
        if ((cause instanceof ConnectException || cause instanceof ConnectionClosedException || e instanceof ConnectException || e instanceof ConnectionClosedException) && retryConditions.contains(RetryCondition.CONNECTION_FAILED)) {
            return true;
        }
        if ((cause instanceof ConnectTimeoutException || e instanceof ConnectTimeoutException) && retryConditions.contains(RetryCondition.CONNECTION_TIMEOUT)) {
            return true;
        }
        if ((cause instanceof SocketTimeoutException || cause instanceof TimeoutException || e instanceof SocketTimeoutException || e instanceof TimeoutException) && retryConditions.contains(RetryCondition.READ_TIMEOUT)) {
            return true;
        }

        return false;
    }

    public byte[] predict(byte[] requestContent) throws Exception {
        if (compressor != null) {
            if (compressor == Compressor.Gzip) {
                requestContent = GzipUtils.compress(requestContent);
            } else if (compressor == Compressor.Zlib) {
                requestContent = ZlibUtils.compress(requestContent);
            }  else if (compressor == Compressor.Snappy) {
                requestContent = SnappyUtils.compress(requestContent);
            }  else if (compressor == Compressor.LZ4) {
                requestContent = LZ4Utils.compress(requestContent);
            }   else if (compressor == Compressor.LZ4Frame) {
                requestContent = LZ4Utils.compressFrame(requestContent);
            }  else if (compressor == Compressor.Zstd) {
                requestContent = ZstdUtils.compress(requestContent);
            } else {
                log.warn("Compressor are not supported!");
            }
        }
        byte[] content = null;
        String lastUrl = "";
        for (int currentRetry = 0; currentRetry <= retryCount; currentRetry++) {
            try {
                HttpPost request = generateSignature(requestContent, lastUrl);
                lastUrl = request.getURI().toString();
                content = getContent(request);
                break;
            } catch (HttpException e) {
                int statusCode = e.getCode();
                String errorMessage = String.format("URL: %s, Status Code:: %d, Message: %s", lastUrl, statusCode, e.getMessage());
                if (shouldRetry(e) && currentRetry < retryCount) {
                    log.warn(String.format("Predict failed on %dth retry, %s", currentRetry + 1, errorMessage));
                } else {
                    log.error(errorMessage);
                    throw new HttpException(statusCode, errorMessage);
                }
            } catch (Exception e) {
                String errorMessage = String.format("URL: %s, Message: %s", lastUrl, (e.getMessage() == null) ? e : e.getMessage());
                if (shouldRetry(e) && currentRetry < retryCount) {
                    log.warn(String.format("Predict failed on %dth retry, %s", currentRetry + 1, errorMessage));
                } else {
                    log.error(errorMessage);
                    throw e;
                }
            }
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



    public EasyRecPredictProtos.PBResponse predict(EasyRecRequest runRequest) throws Exception {
        EasyRecPredictProtos.PBResponse runResponse = null;
        byte[] result = this.predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse = EasyRecPredictProtos.PBResponse.parseFrom(result);
        }

        return runResponse;
    }

    public TorchRecPredictProtos.PBResponse predict(TorchRecRequest runRequest) throws Exception {
        TorchRecPredictProtos.PBResponse runResponse = null;
        byte[] result = this.predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse = TorchRecPredictProtos.PBResponse.parseFrom(result);
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

    private void handleRetryOrFailure(CompletableFuture<byte[]> futureResponse, Exception exception, int currentRetry, String url, byte[] requestData) {
        String errorMessage;
        if (exception instanceof HttpException) {
            int statusCode = ((HttpException) exception).getCode();
            errorMessage = String.format("URL: %s, Status Code:: %d, Message: %s", url, statusCode, exception.getMessage());
        } else {
            errorMessage = String.format("URL: %s, Message: %s", url, (exception.getMessage() == null) ? exception : exception.getMessage());
        }

        if (currentRetry < retryCount && shouldRetry(exception)) {
            log.warn(String.format("PredictAsync failed on %dth retry, %s", currentRetry + 1, errorMessage));
            predictAsyncInternal(requestData, currentRetry + 1, url).whenComplete((result, ex) -> {
                if (ex != null) {
                    Throwable cause = ex.getCause();
                    futureResponse.completeExceptionally((cause != null) ? cause : ex);
                } else {
                    futureResponse.complete(result);
                }
            });
        } else {
            log.error(errorMessage);
            futureResponse.completeExceptionally(exception);
        }
    }

    private CompletableFuture<byte[]> predictAsyncInternal(byte[] requestData, int currentRetry, String lastUrl) {
        CompletableFuture<byte[]> futureResponse = new CompletableFuture<>();
        try {
            // Generate the HTTP POST request with signatures
            HttpPost request = generateSignature(requestData, lastUrl);
            httpclient.execute(request, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse response) {
                    try {
                        byte[] responseContent = handleResponse(response);
                        futureResponse.complete(responseContent);
                    } catch (HttpException e) {
                        handleRetryOrFailure(futureResponse, e, currentRetry, request.getURI().toString(), requestData);
                    } catch (Exception e) {
                        handleRetryOrFailure(futureResponse, e, currentRetry, request.getURI().toString(), requestData);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    handleRetryOrFailure(futureResponse, ex, currentRetry, request.getURI().toString(), requestData);
                }

                @Override
                public void cancelled() {
                    futureResponse.cancel(true);
                }
            });
        } catch (Exception ex) {
            futureResponse.completeExceptionally(ex);
        }
        return futureResponse;
    }

    public CompletableFuture<byte[]> predictAsync(byte[] requestContent) {
        // Start the asynchronous prediction with initial retry parameters
        return predictAsyncInternal(requestContent, 0, "");
    }

    public CompletableFuture<BladeResponse> predictAsync(BladeRequest runRequest) {
        CompletableFuture<BladeResponse> futureResponse = new CompletableFuture<>();

        predictAsync(runRequest.getRequest().toByteArray())
            .thenApply(result -> {
                BladeResponse runResponse = new BladeResponse();
                runResponse.setContentValues(result);
                return runResponse;
            })
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    futureResponse.completeExceptionally(ex);
                } else {
                    futureResponse.complete(res);
                }
            });

        return futureResponse;
    }

    public CompletableFuture<TFResponse> predictAsync(TFRequest runRequest) {
        CompletableFuture<TFResponse> futureResponse = new CompletableFuture<>();

        predictAsync(runRequest.getRequest().toByteArray())
            .thenApply(result -> {
                TFResponse runResponse = new TFResponse();
                runResponse.setContentValues(result);
                return runResponse;
            })
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    futureResponse.completeExceptionally(ex);
                } else {
                    futureResponse.complete(res);
                }
            });

        return futureResponse;
    }

    public CompletableFuture<CaffeResponse> predictAsync(CaffeRequest runRequest) {
        CompletableFuture<CaffeResponse> futureResponse = new CompletableFuture<>();

        predictAsync(runRequest.getRequest().toByteArray())
            .thenApply(result -> {
                CaffeResponse runResponse = new CaffeResponse();
                runResponse.setContentValues(result);
                return runResponse;
            })
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    futureResponse.completeExceptionally(ex);
                } else {
                    futureResponse.complete(res);
                }
            });

        return futureResponse;
    }

    public CompletableFuture<JsonResponse> predictAsync(JsonRequest requestContent) {
        CompletableFuture<JsonResponse> futureResponse = new CompletableFuture<>();

        byte[] requestData;
        try {
            requestData = requestContent.getJSON().getBytes();
        } catch (IOException ex) {
            futureResponse.completeExceptionally(ex);
            return futureResponse;
        }

        predictAsync(requestData)
            .thenApply(resultBytes -> {
                JsonResponse jsonResponse = new JsonResponse();
                try {
                    jsonResponse.setContentValues(resultBytes);
                    return jsonResponse;
                } catch (Exception ex) {
                    throw new CompletionException(ex);
                }
            })
            .whenComplete((jsonResponse, throwable) -> {
                if (throwable != null) {
                    futureResponse.completeExceptionally(throwable.getCause());
                } else {
                    futureResponse.complete(jsonResponse);
                }
            });

        return futureResponse;
    }


    public CompletableFuture<TorchResponse> predictAsync(TorchRequest runRequest) {
        CompletableFuture<TorchResponse> futureResponse = new CompletableFuture<>();

        predictAsync(runRequest.getRequest().toByteArray())
            .thenApply(result -> {
                TorchResponse runResponse = new TorchResponse();
                runResponse.setContentValues(result);
                return runResponse;
            })
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    futureResponse.completeExceptionally(ex);
                } else {
                    futureResponse.complete(res);
                }
            });

        return futureResponse;
    }

    public CompletableFuture<String> predictAsync(String requestContent) {
        CompletableFuture<String> futureResponse = new CompletableFuture<>();

        predictAsync(requestContent.getBytes())
            .thenApply(result -> {
                return new String(result);
            })
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    futureResponse.completeExceptionally(ex);
                } else {
                    futureResponse.complete(res);
                }
            });

        return futureResponse;
    }

    public void shutdown() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            }

            if (httpclient != null) {
                httpclient.close();
            }

            if (cm != null) {
                cm.shutdown();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
