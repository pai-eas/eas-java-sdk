package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.proto.EasyRecPredictProtos;
import com.aliyun.openservices.eas.predict.proto.TorchRecPredictProtos;
import com.aliyun.openservices.eas.predict.request.*;
import com.aliyun.openservices.eas.predict.response.*;
import com.aliyun.openservices.eas.predict.utils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by xiping.zk on 2018/07/25.
 */

public class PredictClient extends AbstractEasClient<PredictClient> {
    private static Log log = LogFactory.getLog(PredictClient.class);
    final private int endpointRetryCount = 10;
    private Map<String, String> mapHeader = null;
    private boolean httpTracingEnabled = false;

    private String token = null;
    private String serviceName = null;
    private String endpoint = null;
    private String url = null;
    private boolean isCompressed = false;
    private volatile EnumSet<RetryCondition> retryConditions = EnumSet.noneOf(RetryCondition.class);
    private String contentType = "application/octet-stream";
    private volatile int errorCode = 200;
    private volatile String errorMessage;
    private String directEndPoint = null;
    private int requestTimeout = 0;
    private Compressor compressor = null;
    private Compressor decompressor = null;

    private static final CallConfig EMPTY_CALL_CONFIG = new CallConfig();

    public PredictClient() {
        this(new HttpConfig());
    }

    public PredictClient(HttpConfig httpConfig) {
        this.httpConfig = httpConfig != null ? httpConfig : new HttpConfig();
        this.retryCount = 3;
        this.httpTracingEnabled = this.httpConfig.isHttpTracingEnabled();
        this.requestTimeout = this.httpConfig.getRequestTimeout();
        this.httpSupport = HttpClientSupport.create(this.httpConfig);
    }

    /**
     * Creates a client using an externally built HttpClient 5 async client.
     * The provided client is used as-is: the SDK neither starts nor closes it,
     * and the connection-related settings of {@code httpConfig} are ignored
     * (configure timeouts, pooling, TLS and proxies directly on the client).
     *
     * @param httpConfig SDK-level configuration (retry, compression, content type, ...)
     * @param httpClient externally built and started client; must not be null
     */
    public PredictClient(HttpConfig httpConfig, CloseableHttpAsyncClient httpClient) {
        this.httpConfig = httpConfig != null ? httpConfig : new HttpConfig();
        this.retryCount = 3;
        this.httpTracingEnabled = this.httpConfig.isHttpTracingEnabled();
        this.requestTimeout = this.httpConfig.getRequestTimeout();
        this.httpSupport = HttpClientSupport.withExternalClient(this.httpConfig, httpClient);
    }


    public PredictClient setToken(String token) {
        if (token == null || token.length() > 0) {
            this.token = token;
        }
        return this;
    }

    /**
     * Sets the SDK-level wait limit for a single request attempt in
     * milliseconds; 0 disables the limit.
     *
     * <p>To let the SDK derive a compatible transport read timeout
     * automatically, set the request timeout on {@link HttpConfig} before
     * constructing the client.</p>
     *
     * @param requestTimeout request timeout in milliseconds; 0 means unlimited
     */
    public PredictClient setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * Sets the name of the online prediction service. It is used as the path
     * segment after {@code /api/predict/}.
     */
    public PredictClient setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    /**
     * @deprecated use {@link #setServiceName(String)}; "model name" is the
     *             legacy name of the service name and may be misleading for
     *             services that do not serve a model.
     */
    @Deprecated
    public PredictClient setModelName(String modelName) {
        return setServiceName(modelName);
    }

    public PredictClient setEndpoint(String endpoint) {
        if (endpoint != null && !endpoint.startsWith("http://") && !endpoint.startsWith("https://")){
            this.endpoint = "https://" + endpoint;
        } else {
            this.endpoint = endpoint;
        }
        return this;
    }

    public PredictClient setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @deprecated VIPServer-based discovery is no longer supported by the SDK.
     *             Use {@link #setDirectEndpoint(String)} for the VPC direct
     *             channel, or set a normal {@link #setEndpoint(String)}.
     */
    @Deprecated
    public PredictClient setVIPServer(String vipSrvEndPoint) {
        log.warn("setVIPServer is deprecated and has no effect; use setDirectEndpoint or setEndpoint instead.");
        return this;
    }

    public PredictClient setDirectEndpoint(String directEndpoint) {
        this.directEndPoint = directEndpoint;
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


    public PredictClient setRetryConditions(EnumSet<RetryCondition> retryConditions) {
        this.retryConditions = (retryConditions == null || retryConditions.isEmpty())
                ? EnumSet.noneOf(RetryCondition.class)
                : EnumSet.copyOf(retryConditions);
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

    /**
     * @deprecated the instance blacklist mechanism was never wired into the
     *             request path and has been removed. Endpoint health is handled
     *             by the underlying HTTP connection pool and retry policy.
     */
    @Deprecated
    public PredictClient startBlacklistMechanism(int blacklistSize,
                                                 int blacklistTimeout,
                                                 int blacklistTimeoutCount) {
        log.warn("startBlacklistMechanism is deprecated and has no effect.");
        return this;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Creates a child client for another service while sharing the parent's
     * connection pool. The child inherits the parent's request configuration
     * (retry, timeout, compression, tracing and headers).
     */
    public PredictClient createChildClient(String token, String endPoint,
                                           String serviceName) {
        PredictClient client = new PredictClient();
        client.httpSupport = this.httpSupport.shareForChild();
        copyConfigurationTo(client);
        client.setToken(token).setEndpoint(endPoint).setServiceName(serviceName);
        return client;
    }

    public PredictClient createChildClient() {
        PredictClient client = new PredictClient();
        client.httpSupport = this.httpSupport.shareForChild();
        copyConfigurationTo(client);
        if (this.directEndPoint != null) {
            client.setDirectEndpoint(this.directEndPoint);
        } else {
            client.setEndpoint(this.endpoint);
        }
        return client;
    }

    private void copyConfigurationTo(PredictClient client) {
        client.setToken(this.token)
            .setServiceName(this.serviceName)
            .setTraceId(this.traceId)
            .setRetryCount(this.retryCount)
            .setRetryConditions(this.retryConditions)
            .setRequestTimeout(this.requestTimeout)
            .setIsCompressed(this.isCompressed)
            .setContentType(this.contentType)
            .setRequestPath(this.requestPath)
            .setUrl(this.url)
            .addExtraHeaders(this.extraHeaders);
        client.httpTracingEnabled = this.httpTracingEnabled;
        if (this.compressor != null) {
            client.setCompressor(this.compressor);
        }
        if (this.decompressor != null) {
            client.setDecompressor(this.decompressor);
        }
    }

    private String getUrl(String lastUrl, CallConfig callConfig) throws Exception {
        String pathWithQuery = callConfig.requestURI(this.requestPath);
        if (this.url != null) {
            return this.url + pathWithQuery;
        }
        String apiPath = "/api/predict/" + serviceName + pathWithQuery;
        if (directEndPoint != null) {
            for (int i = 0; i < endpointRetryCount; i++) {
                com.aliyun.openservices.eas.discovery.core.Endpoint host =
                        DiscoveryClient.srvHost(this.serviceName);
                if (host == null) {
                    break;
                }
                String url = "https://" + host.toInetAddr() + apiPath;
                if (DiscoveryClient.getHosts(this.serviceName).size() < 2 || !url.equals(lastUrl)) {
                    return url;
                }
            }
            throw new IllegalStateException("no available EAS instance for service: " + serviceName);
        }
        if (this.endpoint == null) {
            throw new IllegalStateException("endpoint is not configured; call setEndpoint, setUrl or setDirectEndpoint first");
        }
        return this.endpoint + apiPath;
    }

    private SimpleHttpRequest generateSignature(byte[] requestContent, String lastUrl, CallConfig callConfig,
                                                boolean tracingEnabled) throws Exception {
        SimpleHttpRequest request = SimpleHttpRequest.create("POST", getUrl(lastUrl, callConfig));
        request.setBody(requestContent, null);
        HmacSha1Signature signature = new HmacSha1Signature();
        String md5Content = signature.getMD5(requestContent);
        for (Map.Entry<String, String> entry : this.extraHeaders.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        if (traceId != null && !traceId.isEmpty()) {
            request.setHeader(TRACE_ID_HEADER, traceId);
        }
        request.setHeader(HttpHeaders.CONTENT_MD5, md5Content);
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentTime = dateFormat.format(now) + " GMT";
        request.setHeader(HttpHeaders.DATE, currentTime);
        request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);

        if (mapHeader != null || tracingEnabled) {
            // Client-Timestamp activates the EAS server-side HTTP tracing,
            // which reports its phase timestamps back in the response headers.
            request.setHeader("Client-Timestamp",
                    String.valueOf(System.currentTimeMillis()));
        }

        if (this.token != null) {
            String pathWithQuery = callConfig.requestURI(this.requestPath);
            String authPath;
            if (this.url == null) {
                authPath = "/api/predict/" + this.serviceName + pathWithQuery;
            } else {
                URL u = new URL(this.url);
                authPath = u.getPath() + pathWithQuery;
            }
            String auth = "POST" + "\n" + md5Content + "\n"
                + this.contentType + "\n" + currentTime + "\n" + authPath;
            request.setHeader(HttpHeaders.AUTHORIZATION,
                "EAS " + signature.computeSignature(token, auth));
        }

        if (callConfig != null && callConfig.getHeaders() != null) {
            for (Map.Entry<String, String> entry : callConfig.getHeaders().entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    /**
     * Applies the configured compression to the request body. The returned
     * bytes are the exact bytes that must be signed and transmitted.
     */
    private byte[] prepareRequestBody(byte[] requestContent) throws IOException {
        if (compressor != null) {
            switch (compressor) {
                case Gzip:
                    return GzipUtils.compress(requestContent);
                case Zlib:
                    return ZlibUtils.compress(requestContent);
                case Snappy:
                    return SnappyUtils.compress(requestContent);
                case LZ4:
                    return LZ4Utils.compress(requestContent);
                case LZ4Frame:
                    return LZ4Utils.compressFrame(requestContent);
                case Zstd:
                    return ZstdUtils.compress(requestContent);
                default:
                    log.warn("Compressor is not supported: " + compressor);
                    return requestContent;
            }
        }
        if (isCompressed) {
            return Snappy.compress(requestContent);
        }
        return requestContent;
    }

    private byte[] handleResponse(SimpleHttpResponse response) throws IOException, HttpException {
        byte[] content;
        int statusCode = response.getCode();
        if (statusCode == 200) {
            content = response.getBodyBytes();
            if (content == null) {
                content = new byte[0];
            }
            if (isCompressed) {
                content = Snappy.uncompress(content);
            } else if (decompressor != null) {
                content = decompressContent(content, response);
            }
        } else {
            byte[] body = response.getBodyBytes();
            String errorMsg = body == null ? "" : new String(body, StandardCharsets.UTF_8);
            throw new HttpException(statusCode, errorMsg);
        }
        return content;
    }

    /**
     * Decompresses content based on the configured decompressor.
     *
     * @throws IOException when the payload cannot be decompressed; returning
     *                     the still-compressed bytes would hand corrupt data to
     *                     the caller.
     */
    private byte[] decompressContent(byte[] content, HttpResponse response) throws IOException {
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
                    if (lz4OriginalLength == null) {
                        throw new IOException("Missing X-LZ4-Original-Length header for LZ4 response");
                    }
                    int originalLength;
                    try {
                        originalLength = Integer.parseInt(lz4OriginalLength.getValue().trim());
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid X-LZ4-Original-Length header: "
                                + lz4OriginalLength.getValue(), e);
                    }
                    return LZ4Utils.decompress(content, originalLength);
                case LZ4Frame:
                    return LZ4Utils.decompressFrame(content);
                case Zstd:
                    return ZstdUtils.decompress(content);
                case Auto:
                    return autoDecompressContent(content, response);
                default:
                    return content;
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Decompression failed: " + e.getMessage(), e);
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

        String contentEncoding = contentEncodingHeader.getValue().toLowerCase(Locale.ROOT);
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

    private byte[] getContent(SimpleHttpRequest request, int attempt, String url) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {
        Future<SimpleHttpResponse> future =
                httpSupport.getClient().execute(request, (FutureCallback<SimpleHttpResponse>) null);
        SimpleHttpResponse response;
        try {
            if (requestTimeout > 0) {
                response = future.get(requestTimeout, TimeUnit.MILLISECONDS);
            } else {
                response = future.get();
            }
        } catch (TimeoutException e) {
            // Do not leave the request running in the background: it would
            // keep a connection and a server-side model execution alive.
            future.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }

        if (mapHeader != null) {
            synchronized (mapHeader) {
                Header[] header = response.getHeaders();
                for (int i = 0; i < header.length; i++) {
                    mapHeader.put(header[i].getName(), header[i].getValue());
                }
            }
        }
        if (httpTracingEnabled) {
            // Emit before handleResponse so error responses are traced too.
            ServerTracing.logIfPresent(url, attempt, response.getCode(), response);
        }
        return handleResponse(response);
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
        return predict(requestContent, (CallConfig) null);
    }

    public byte[] predict(byte[] requestContent, CallConfig callConfig) throws Exception {
        requestContent = prepareRequestBody(requestContent);
        CallConfig config = callConfig != null ? callConfig : EMPTY_CALL_CONFIG;
        byte[] content = null;
        String lastUrl = "";
        for (int currentRetry = 0; currentRetry <= Math.max(0, retryCount); currentRetry++) {
            try {
                SimpleHttpRequest request = generateSignature(requestContent, lastUrl, config, httpTracingEnabled);
                lastUrl = request.getUri().toString();
                content = getContent(request, currentRetry + 1, lastUrl);
                this.errorCode = 200;
                this.errorMessage = null;
                break;
            } catch (HttpException e) {
                int statusCode = e.getCode();
                this.errorCode = statusCode;
                String errorMessage = String.format("URL: %s, Status Code:: %d, Message: %s", lastUrl, statusCode, e.getMessage());
                this.errorMessage = errorMessage;
                if (shouldRetry(e) && currentRetry < retryCount) {
                    log.warn(String.format("Predict failed on %dth retry, %s", currentRetry + 1, errorMessage));
                } else {
                    log.error(errorMessage);
                    throw new HttpException(statusCode, errorMessage);
                }
            } catch (Exception e) {
                this.errorCode = -1;
                String errorMessage = String.format("URL: %s, Message: %s", lastUrl, (e.getMessage() == null) ? e : e.getMessage());
                this.errorMessage = errorMessage;
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
        return predict(runRequest, null);
    }

    public BladeResponse predict(BladeRequest runRequest, CallConfig callConfig) throws Exception {
        BladeResponse runResponse = new BladeResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public TFResponse predict(TFRequest runRequest) throws Exception {
        return predict(runRequest, null);
    }

    public TFResponse predict(TFRequest runRequest, CallConfig callConfig) throws Exception {
        TFResponse runResponse = new TFResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public CaffeResponse predict(CaffeRequest runRequest) throws Exception {
        return predict(runRequest, null);
    }

    public CaffeResponse predict(CaffeRequest runRequest, CallConfig callConfig) throws Exception {
        CaffeResponse runResponse = new CaffeResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public JsonResponse predict(JsonRequest requestContent) throws Exception {
        return predict(requestContent, null);
    }

    public JsonResponse predict(JsonRequest requestContent, CallConfig callConfig) throws Exception {
        byte[] result = predict(requestContent.getJSON().getBytes(StandardCharsets.UTF_8), callConfig);
        JsonResponse jsonResponse = new JsonResponse();
        if (result != null) {
            jsonResponse.setContentValues(result);
        }
        return jsonResponse;
    }

    public TorchResponse predict(TorchRequest runRequest) throws Exception {
        return predict(runRequest, null);
    }

    public TorchResponse predict(TorchRequest runRequest, CallConfig callConfig) throws Exception {
        TorchResponse runResponse = new TorchResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }


    public EasyRecPredictProtos.PBResponse predict(EasyRecRequest runRequest) throws Exception {
        return predict(runRequest, null);
    }

    public EasyRecPredictProtos.PBResponse predict(EasyRecRequest runRequest, CallConfig callConfig) throws Exception {
        EasyRecPredictProtos.PBResponse runResponse = null;
        byte[] result = this.predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse = EasyRecPredictProtos.PBResponse.parseFrom(result);
        }

        return runResponse;
    }

    public TorchRecPredictProtos.PBResponse predict(TorchRecRequest runRequest) throws Exception {
        return predict(runRequest, null);
    }

    public TorchRecPredictProtos.PBResponse predict(TorchRecRequest runRequest, CallConfig callConfig) throws Exception {
        TorchRecPredictProtos.PBResponse runResponse = null;
        byte[] result = this.predict(runRequest.getRequest().toByteArray(), callConfig);
        if (result != null) {
            runResponse = TorchRecPredictProtos.PBResponse.parseFrom(result);
        }

        return runResponse;
    }

    public String predict(String requestContent) throws Exception {
        return predict(requestContent, null);
    }

    public String predict(String requestContent, CallConfig callConfig) throws Exception {
        byte[] result = predict(requestContent.getBytes(StandardCharsets.UTF_8), callConfig);
        if (result != null) {
            return new String(result, StandardCharsets.UTF_8);
        }
        return null;
    }

    private void handleRetryOrFailure(CompletableFuture<byte[]> futureResponse, Exception exception,
                                      int currentRetry, String url, byte[] requestData) {
        String errorMessage;
        if (exception instanceof HttpException) {
            int statusCode = ((HttpException) exception).getCode();
            this.errorCode = statusCode;
            errorMessage = String.format("URL: %s, Status Code:: %d, Message: %s", url, statusCode, exception.getMessage());
        } else {
            this.errorCode = -1;
            errorMessage = String.format("URL: %s, Message: %s", url, (exception.getMessage() == null) ? exception : exception.getMessage());
        }
        this.errorMessage = errorMessage;

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

    /**
     * HTTP Tracing only instruments the synchronous {@link #predict(byte[])}
     * path; predictAsync does not create timeline events.
     */
    private CompletableFuture<byte[]> predictAsyncInternal(byte[] requestData, int currentRetry, String lastUrl) {
        CompletableFuture<byte[]> futureResponse = new CompletableFuture<>();
        try {
            // Generate the HTTP POST request with signatures. The body has
            // already been compressed by predictAsync(byte[]).
            final SimpleHttpRequest request = generateSignature(requestData, lastUrl, EMPTY_CALL_CONFIG, false);
            final String requestUri = request.getUri().toString();
            FutureCallback<SimpleHttpResponse> retryCallback = new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    // Response decompression must not run on the HttpClient
                    // reactor thread: a large body would stall all in-flight
                    // requests of this client.
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return handleResponse(response);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }).whenComplete((responseContent, ex) -> {
                        if (ex == null) {
                            errorCode = 200;
                            errorMessage = null;
                            futureResponse.complete(responseContent);
                            return;
                        }
                        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                                ? ex.getCause() : ex;
                        Exception failure = (cause instanceof Exception)
                                ? (Exception) cause : new RuntimeException(cause);
                        CompletableFuture.runAsync(() -> handleRetryOrFailure(
                                futureResponse, failure, currentRetry, requestUri, requestData));
                    });
                }

                @Override
                public void failed(Exception ex) {
                    CompletableFuture.runAsync(() -> handleRetryOrFailure(
                            futureResponse, ex, currentRetry, requestUri, requestData));
                }

                @Override
                public void cancelled() {
                    futureResponse.cancel(true);
                }
            };
            httpSupport.getClient().execute(request, retryCallback);
        } catch (Exception ex) {
            futureResponse.completeExceptionally(ex);
        }
        return futureResponse;
    }

    public CompletableFuture<byte[]> predictAsync(byte[] requestContent) {
        try {
            return predictAsyncInternal(prepareRequestBody(requestContent), 0, "");
        } catch (Exception e) {
            CompletableFuture<byte[]> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
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
            requestData = requestContent.getJSON().getBytes(StandardCharsets.UTF_8);
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

        predictAsync(requestContent.getBytes(StandardCharsets.UTF_8))
            .thenApply(result -> {
                return new String(result, StandardCharsets.UTF_8);
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


    @Override
    protected PredictClient self() {
        return this;
    }

    @Override
    public PredictClient setRetryCount(int retryCount) {
        return super.setRetryCount(retryCount);
    }

    @Override
    public PredictClient addExtraHeaders(Map<String, String> extraHeaders) {
        return super.addExtraHeaders(extraHeaders);
    }

    @Override
    public PredictClient setHttpClient(CloseableHttpAsyncClient httpClient) {
        return super.setHttpClient(httpClient);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
