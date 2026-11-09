package com.aliyun.openservices.eas.predict.http;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.eas.predict.queue_client.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.net.URIBuilder;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client for accessing prediction service by creating a fixed size connection pool to perform the
 * request through established persistent connections.
 */
public class QueueClient extends AbstractEasClient<QueueClient> {
    public static String HeaderRequestId = "X-Eas-Queueservice-Request-Id";
    public static String HeaderAuthorization = "Authorization";
    public static String HeaderRedisUid = "X-EAS-QueueService-Redis-Uid";
    public static String HeaderRedisGid = "X-EAS-QueueService-Redis-Gid";
    public static String HeaderAccessRear = "X-EAS-QueueService-Access-Rear";

    private static Log log = LogFactory.getLog(QueueClient.class);
    public ReentrantLock lock = new ReentrantLock();
    public volatile WebSocketClient webSocketClient = null;
    private String baseUrl = "";
    private QueueUser user = null;
    private volatile String prioHeader = null;
    private volatile WebSocketWatcher activeWatcher = null;
    private int requestTimeout = 0;

    public QueueClient() {
        this.httpConfig = new HttpConfig();
        this.httpSupport = HttpClientSupport.create(this.httpConfig);
        this.retryCount = 5;
        this.requestTimeout = this.httpConfig.getRequestTimeout();
        this.user = new QueueUser();
    }

    public QueueClient(
        String endpoint, String queueName, String token, HttpConfig httpConfig, QueueUser user) {
        this(String.format("%s/api/predict/%s", endpoint, queueName), token, httpConfig, user);
    }

    public QueueClient(
        String endpoint, String queueName, String token, HttpConfig httpConfig, QueueUser user,
        CloseableHttpAsyncClient httpClient) {
        this(String.format("%s/api/predict/%s", endpoint, queueName), token, httpConfig, user, httpClient);
    }

    public QueueClient(
        String url, String token, HttpConfig httpConfig, QueueUser user) {
        this(url, token, httpConfig, user, null);
    }

    /**
     * Creates a queue client using an externally built HttpClient 5 async
     * client. The provided client is used as-is: the SDK neither starts nor
     * closes it, and the connection-related settings of {@code httpConfig}
     * are ignored.
     */
    public QueueClient(
        String url, String token, HttpConfig httpConfig, QueueUser user,
        CloseableHttpAsyncClient httpClient) {
        this.httpConfig = httpConfig != null ? httpConfig : new HttpConfig();
        this.retryCount = 5;
        this.requestTimeout = this.httpConfig.getRequestTimeout();
        baseUrl = url == null ? "" : url;
        if (!baseUrl.isEmpty()
                && !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            baseUrl = "https://" + baseUrl;
        }
        this.httpSupport = httpClient != null
                ? HttpClientSupport.withExternalClient(this.httpConfig, httpClient)
                : HttpClientSupport.create(this.httpConfig);
        // Copy the caller-provided identity so that reusing one QueueUser for
        // multiple clients (with different tokens) cannot overwrite another
        // client's credentials.
        QueueUser source = user != null ? user : new QueueUser();
        this.user = new QueueUser(source.getUid(), source.getGid());
        this.user.setToken(token);
    }




    /**
     * add identity info into headers
     *
     * @param headers http request headers
     * @return headers with identity
     */
    private Map<String, String> withIdentity(Map<String, String> headers) {
        headers.putAll(this.extraHeaders);
        if (traceId != null && !traceId.isEmpty()) {
            headers.put(TRACE_ID_HEADER, traceId);
        }
        headers.put(HeaderAuthorization, user.getToken());
        headers.put(HeaderRedisUid, user.getUid());
        headers.put(HeaderRedisGid, user.getGid());
        return headers;
    }

    /**
     * Adds user tags while never allowing them to overwrite the reserved
     * queue-service control parameters (keys starting with underscore).
     */
    private static void putUserParams(Map<String, String> target, Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("_")) {
                log.warn("Ignoring reserved queue parameter from user tags: " + key);
                continue;
            }
            target.put(key, entry.getValue());
        }
    }

    private void withPriority(SimpleHttpRequest request, Long priority) throws Exception {
        if (priority > 0) {
            if (prioHeader == null) {
                synchronized (this) {
                    if (prioHeader == null) {
                        prioHeader = attributes().getString("meta.header.priority");
                    }
                }
            }
            if (prioHeader != null) {
                request.setHeader(prioHeader, Long.toString(priority));
            }
        }
    }

    private SimpleHttpRequest buildRequest(String method, Map<String, String> queryParams)
        throws Exception {
        String uri = createURI(baseUri(), queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        SimpleHttpRequest request = SimpleHttpRequest.create(method, uri);
        headers.forEach(request::setHeader);
        return request;
    }

    /**
     * common code for excute a http request
     *
     * @param request http request
     * @return HttpResponse
     */
    private SimpleHttpResponse doRequest(SimpleHttpRequest request) throws Exception {
        Future<SimpleHttpResponse> future = httpSupport.getClient()
                .execute(request, (FutureCallback<SimpleHttpResponse>) null);
        try {
            if (requestTimeout > 0) {
                return future.get(requestTimeout, TimeUnit.MILLISECONDS);
            }
            return future.get();
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new HttpException(-1, "queue request timed out after " + requestTimeout + " ms");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Executes the request and returns a 2xx response. Non-2xx responses are
     * thrown as {@link HttpException}; they must never be converted into an
     * empty result by the caller. All failures are retried, matching the
     * default retry behavior of the client.
     */
    private SimpleHttpResponse retryRequest(SimpleHttpRequest request) throws Exception {
        Exception lastFailure = null;
        for (int i = 0; i <= retryCount; ++i) {
            SimpleHttpResponse response;
            try {
                response = doRequest(request);
            } catch (HttpException e) {
                if (i == retryCount) {
                    log.warn("Queue request failed with status " + e.getCode()
                            + " on attempt " + (i + 1) + ": " + e.getMessage());
                    throw e;
                }
                lastFailure = e;
                log.warn("Queue request failed with status " + e.getCode()
                        + " on attempt " + (i + 1) + ": " + e.getMessage());
                continue;
            } catch (Exception e) {
                if (i == retryCount) {
                    log.warn("Queue request failed on attempt " + (i + 1) + ": " + e.getMessage());
                    throw e;
                }
                lastFailure = e;
                log.debug("Queue request failed on attempt " + (i + 1) + ": " + e.getMessage());
                continue;
            }

            int statusCode = response.getCode();
            if (statusCode / 100 == 2) {
                return response;
            }
            String body = responseBody(response);
            HttpException failure = new HttpException(statusCode, body);
            if (i == retryCount) {
                log.warn("Queue request failed with status " + statusCode
                        + " on attempt " + (i + 1) + ": " + body);
                throw failure;
            }
            lastFailure = failure;
            log.warn("Queue request failed with status " + statusCode
                    + " on attempt " + (i + 1) + ": " + body);
        }
        throw lastFailure != null ? lastFailure : new HttpException(-1, "queue request failed");
    }

    private static String responseBody(SimpleHttpResponse response) {
        byte[] body = response == null ? null : response.getBodyBytes();
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static void requireBody(SimpleHttpResponse response) throws HttpException {
        if (response == null || response.getBodyBytes() == null) {
            throw new HttpException(-1, "queue service returned an empty response body");
        }
    }

    private String createURI(String baseUrl, Map<String, String> queryParams) throws Exception {
        URIBuilder ub = new URIBuilder(baseUrl);
        queryParams.forEach(ub::addParameter);
        return ub.build().toString();
    }

    /**
     * Effective base URI: baseUrl plus the client-level request path (for
     * queue services exposed under a custom path).
     */
    private String baseUri() {
        ensureConfigured();
        return baseUrl + normalizeRequestPath(requestPath);
    }

    /**
     * Effective base URI for one call: the per-call path (CallConfig) takes
     * precedence over the client-level request path.
     */
    private String baseUri(CallConfig callConfig) {
        ensureConfigured();
        String path = callConfig != null && callConfig.getRequestPath() != null
                ? callConfig.getRequestPath() : requestPath;
        return baseUrl + normalizeRequestPath(path);
    }

    private void ensureConfigured() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException(
                    "queue endpoint is not configured; use the QueueClient(endpoint, queueName, ...) constructor");
        }
    }

    private static String normalizeRequestPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.charAt(0) == '/' ? path : "/" + path;
    }

    private static void validateIndexes(long[] indexes) {
        if (indexes == null || indexes.length == 0) {
            throw new IllegalArgumentException("indexes must not be null or empty");
        }
    }

    /**
     * common code for indexes related request
     *
     * @param indexes array of data index
     * @param method  http request method: "DELETE", "PUT"
     * @return content of httpResponse body
     */
    private String processIndexes(long[] indexes, String method) throws Exception {
        validateIndexes(indexes);
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_indexes_", StringUtils.join(ArrayUtils.toObject(indexes), ","));
                }
            };
        SimpleHttpResponse response = retryRequest(buildRequest(method, queryParams));
        return responseBody(response);
    }

    /**
     * get the attributes of a queue service
     *
     * @return JSONObject containing the attributes of queue service
     */
    public JSONObject attributes() throws Exception {
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_attrs_", Boolean.toString(true));
                }
            };

        SimpleHttpResponse response = retryRequest(buildRequest("GET", queryParams));
        requireBody(response);
        String content = responseBody(response).trim();
        if (content.isEmpty()) {
            throw new HttpException(-1, "queue service returned an empty attributes body");
        }
        return JSONObject.parseObject(content);
    }

    /**
     * count the number of data in the queue
     *
     * @param tags customized parameters
     * @return count
     */
    public long count(Map<String, String> tags) throws Exception {
        return count(0L, tags);
    }

    /**
     * count the number of data in the queue, support priority
     *
     * @param priority data priority
     * @param tags     customized parameters
     * @return count
     */
    public long count(long priority, Map<String, String> tags) throws Exception {
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_count_", Boolean.toString(true));
                }
            };
        if (priority > 0) {
            queryParams.put("_priority_", Long.toString(priority));
        } else if (priority < 0) {
            throw new IllegalArgumentException("priority must be a non-negative number");
        }
        putUserParams(queryParams, tags);

        SimpleHttpResponse response = retryRequest(buildRequest("GET", queryParams));
        requireBody(response);
        String content = responseBody(response).trim();
        if (content.isEmpty()) {
            throw new HttpException(-1, "queue service returned an empty count body");
        }
        try {
            return Long.parseLong(content);
        } catch (NumberFormatException e) {
            throw new HttpException(-1, "invalid count body: " + content);
        }
    }

    /**
     * get the wait info of data in the queue
     *
     * @param index data index
     * @return JSONObject containing 'IsPending' and 'WaitCount'
     */
    public JSONObject search(long index) throws Exception {
        if (index <= 0) {
            log.warn("Invalid search index: " + index);
            return JSONObject.parseObject("{}");
        }
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_search_", Boolean.toString(true));
                    put("_index_", Long.toString(index));
                }
            };

        SimpleHttpResponse response = retryRequest(buildRequest("GET", queryParams));
        requireBody(response);
        String content = responseBody(response).trim();
        if (content.isEmpty()) {
            throw new HttpException(-1, "queue service returned an empty search body");
        }
        return JSONObject.parseObject(content);
    }

    /**
     * delete data whose indexes are smaller than the specified index from a queue service
     *
     * @param index data index
     */
    public void truncate(long index) throws Exception {
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_index_", Long.toString(index));
                    put("_trunc_", Boolean.toString(true));
                }
            };
        retryRequest(buildRequest("DELETE", queryParams));
    }

    public void end(boolean force) throws Exception {
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_eos_", Boolean.toString(true));
                }
            };
        if (force) {
            queryParams.put("_force_", Boolean.toString(true));
        }
        retryRequest(buildRequest("POST", queryParams));
    }

    /**
     * put data into queue service
     *
     * @param data     data of byte array
     * @param priority data priority
     * @param tags     customized queryParams
     * @return index, requestId
     */
    public Pair<Long, String> put(byte[] data, long priority, Map<String, String> tags)
        throws Exception {
        return put(data, priority, tags, null);
    }

    /**
     * put data into queue service, with per-call configuration.
     *
     * @param data       data of byte array
     * @param priority   data priority
     * @param tags       customized queryParams
     * @param callConfig per-call overrides: request path (for queue services
     *                   exposed under a custom path), extra query params and
     *                   custom HTTP headers; the per-call path takes
     *                   precedence over the client-level request path
     * @return index, requestId
     */
    public Pair<Long, String> put(byte[] data, long priority, Map<String, String> tags,
        CallConfig callConfig) throws Exception {
        Map<String, String> queryParams = new HashMap<String, String>();
        if (priority > 0) {
            queryParams.put("_priority_", Long.toString(priority));
        } else if (priority < 0) {
            log.warn("Invalid value of priority, should be a non-negative number, now set to normal priority: 0");
        }
        putUserParams(queryParams, tags);
        if (callConfig != null) {
            putUserParams(queryParams, callConfig.getQueryParams());
        }
        String uri = createURI(baseUri(callConfig), queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        if (callConfig != null && callConfig.getHeaders() != null) {
            for (Map.Entry<String, String> entry : callConfig.getHeaders().entrySet()) {
                headers.put(entry.getKey(), entry.getValue());
            }
        }

        SimpleHttpRequest request = SimpleHttpRequest.create("POST", uri);
        headers.forEach(request::setHeader);
        request.setBody(data, null);
        withPriority(request, priority);

        // A put is not idempotent. Retrying after the request has been sent
        // can enqueue the same payload multiple times, so a single attempt is
        // made and every failure is surfaced to the caller.
        SimpleHttpResponse response = doRequest(request);
        int statusCode = response.getCode();
        if (statusCode / 100 != 2) {
            throw new HttpException(statusCode, responseBody(response));
        }
        String content = responseBody(response).trim();
        Header[] headerOfRequestId = response.getHeaders(HeaderRequestId);
        if (headerOfRequestId == null || headerOfRequestId.length < 1) {
            throw new HttpException(statusCode,
                    "response is missing " + HeaderRequestId + " header");
        }
        if (content.isEmpty()) {
            throw new HttpException(statusCode, "queue service returned an empty index body");
        }
        long index;
        try {
            index = Long.parseLong(content);
        } catch (NumberFormatException e) {
            throw new HttpException(statusCode, "invalid queue index body: " + content);
        }
        return Pair.of(index, headerOfRequestId[0].getValue());
    }

    /**
     * put data into a queue service
     *
     * @param data data of byte array
     * @param tags customized queryParams
     * @return index, requestId
     */
    public Pair<Long, String> put(byte[] data, Map<String, String> tags) throws Exception {
        return put(data, 0L, tags);
    }

    /**
     * put data into a queue service
     *
     * @param data data of String
     * @param tags customized queryParams
     * @return index, requestId
     */
    public Pair<Long, String> put(String data, Map<String, String> tags) throws Exception {
        return put(data.getBytes(StandardCharsets.UTF_8), tags);
    }

    /**
     * put data into a queue service with priority
     *
     * @param data     data of String
     * @param priority data priority
     * @param tags     customized queryParams
     * @return index, requestId
     */
    public Pair<Long, String> put(String data, long priority, Map<String, String> tags)
        throws Exception {
        return put(data.getBytes(StandardCharsets.UTF_8), priority, tags);
    }

    /**
     * put data into a queue service with priority and per-call configuration
     *
     * @param data       data of String
     * @param priority   data priority
     * @param tags       customized queryParams
     * @param callConfig per-call overrides
     * @return index, requestId
     */
    public Pair<Long, String> put(String data, long priority, Map<String, String> tags,
        CallConfig callConfig) throws Exception {
        return put(data.getBytes(StandardCharsets.UTF_8), priority, tags, callConfig);
    }

    /**
     * get the data in queue service
     *
     * @param index      data index of long
     * @param length     the lengh of data to get
     * @param timeout    timeout in seconds
     * @param autoDelete whether to delete the data after getting it
     * @param tags       customized queryParams
     * @return DataFrame array
     */
    public DataFrame[] get(
        long index, long length, int timeout, boolean autoDelete, Map<String, String> tags)
        throws Exception {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be a non-negative number");
        }
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_index_", Long.toString(index));
                    put("_length_", Long.toString(length));
                    put("_timeout_", String.format("%ds", timeout));
                    put("_raw_", Boolean.toString(false));
                    // Never let the server delete before the response has been
                    // decoded successfully; delete explicitly below.
                    put("_auto_delete_", Boolean.toString(false));
                }
            };
        putUserParams(queryParams, tags);
        String uri = createURI(baseUri(), queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Accept", "application/vnd.google.protobuf");

        SimpleHttpRequest request = SimpleHttpRequest.create("GET", uri);
        headers.forEach(request::setHeader);

        SimpleHttpResponse response = retryRequest(request);
        requireBody(response);
        DataFrame[] frames = new DataFrameList().decode(response.getBodyBytes()).getList();
        if (autoDelete && frames.length > 0) {
            for (DataFrame frame : frames) {
                delete(frame.getIndex());
            }
        }
        return frames;
    }

    /**
     * get the data in queue service by index. The returned data is verified to
     * have the requested index before it is deleted.
     *
     * @param index data index of long
     * @return DataFrame array containing at most one exact match
     */
    public DataFrame[] getByIndex(long index) throws Exception {
        DataFrame[] frames = get(index, 1L, 0, false, null);
        for (DataFrame frame : frames) {
            if (frame.getIndex() == index) {
                delete(index);
                return new DataFrame[]{frame};
            }
        }
        return new DataFrame[0];
    }

    /**
     * get the data in queue service by requestId. The returned data is
     * verified to carry the requested requestId tag before it is deleted.
     *
     * @param requestId requestId
     * @return DataFrame array containing at most one exact match
     */
    public DataFrame[] getByRequestId(String requestId) throws Exception {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("requestId", requestId);
        DataFrame[] frames = get(0L, 1L, 0, false, tags);
        for (DataFrame frame : frames) {
            if (frame.getTags() != null && requestId.equals(frame.getTags().get("requestId"))) {
                delete(frame.getIndex());
                return new DataFrame[]{frame};
            }
        }
        return new DataFrame[0];
    }

    /**
     * delete data by index
     *
     * @param index data index of long
     * @return the result of delete ,OK or other
     */
    public String delete(long index) throws Exception {
        long[] indexes = new long[1];
        indexes[0] = index;
        return processIndexes(indexes, "DELETE");
    }

    /**
     * delete multiple data by indexes
     *
     * @param indexes data indexes of long array
     * @return the result of delete ,OK or fail
     */
    public String delete(long[] indexes) throws Exception {
        return processIndexes(indexes, "DELETE");
    }

    /**
     * create a watcher to consume streaming data from queue service
     *
     * @param index      position of starting to watch
     * @param window     the size of the data sending window, that is the maximum uncommitted data length
     * @param indexOnly  the returned dataframe only contains index and tags, no data content
     * @param autoCommit automatic commit the data after consuming it
     * @param tags       custom configuration parameters, support for configuring parameters added in queryParams
     * @return WebSocketWatcher
     */
    public WebSocketWatcher watch(
        long index, long window, boolean indexOnly, boolean autoCommit, Map<String, String> tags)
        throws Exception {
        WatchConfig watchConfig = new WatchConfig(true, WatchConfig.DefaultReConnectInterval);
        return watch(index, window, indexOnly, autoCommit, tags, watchConfig);
    }

    /**
     * create a watcher to consume streaming data from queue service
     *
     * @param index       position of starting to watch
     * @param window      the size of the data sending window, that is the maximum uncommitted data length
     * @param indexOnly   the returned dataframe only contains index and tags, no data content
     * @param autoCommit  automatic commit the data after consuming it
     * @param queueTags   support for configuring parameters added in queryParams
     * @param watchConfig support for configuring reConnect Count, reConnect Interval and whether unlimited reConnect
     * @return WebSocketWatcher
     */
    public WebSocketWatcher watch(
        long index, long window, boolean indexOnly, boolean autoCommit, Map<String, String> queueTags, WatchConfig watchConfig)
        throws Exception {
        if (watchConfig == null) {
            watchConfig = new WatchConfig();
        }
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_watch_", Boolean.toString(true));
                    put("_index_", Long.toString(index));
                    put("_window_", Long.toString(window));
                    put("_index_only_", Boolean.toString(indexOnly));
                    put("_auto_commit_", Boolean.toString(autoCommit));
                }
            };
        putUserParams(queryParams, queueTags);
        String uri = createURI(baseUri(), queryParams);
        uri = uri.replaceFirst("http", "ws");
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Accept", "application/vnd.google.protobuf");

        if (watchConfig.getReConInterval() <= 0) {
            log.warn("Invalid reConnect interval, using default: " + WatchConfig.DefaultReConnectInterval);
            watchConfig.setReConInterval(WatchConfig.DefaultReConnectInterval);
        }
        if (!watchConfig.isInfinityReConnect() && watchConfig.getReConCnt() < 0) {
            log.warn("Invalid reConnect count, using default: " + WatchConfig.DefaultReConnectCnt);
            watchConfig.setReConCnt(WatchConfig.DefaultReConnectCnt);
        }
        return new WebSocketWatcher(this, new URI(uri), headers, watchConfig);
    }

    /**
     * Registers the single active watcher of this client.
     */
    public void registerWatcher(WebSocketWatcher watcher) {
        lock.lock();
        try {
            if (activeWatcher != null) {
                throw new IllegalStateException("Another watcher is already running");
            }
            activeWatcher = watcher;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the active watcher if it is the given one.
     */
    public void unregisterWatcher(WebSocketWatcher watcher) {
        lock.lock();
        try {
            if (activeWatcher == watcher) {
                activeWatcher = null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * commit data by index, confirm that the data has been consumed and then delete the data
     *
     * @param index data index of long
     * @return the result of commit ,OK or other
     */
    public String commit(long index) throws Exception {
        long[] indexes = new long[1];
        indexes[0] = index;
        return processIndexes(indexes, "PUT");
    }

    /**
     * commit data by indexes
     *
     * @param indexes data index of long array
     * @return the result of commit ,OK or other
     */
    public String commit(long[] indexes) throws Exception {
        return processIndexes(indexes, "PUT");
    }

    /**
     * negative commit data by indexes
     *
     * @param index data index
     * @return the result of negative commit ,OK or other
     */
    public String negative(long index, String code, String reason) throws Exception {
        long[] indexes = new long[1];
        indexes[0] = index;
        return negative(indexes, code, reason);
    }

    /**
     * negative commit data by indexes
     *
     * @param indexes data indexes
     * @param code    error code, see https://help.aliyun.com/zh/pai/user-guide/queue-service-subscription-push?spm=a2c4g.11186623.0.0.4ca721afPjhmQH#fcbcad2003pdm
     * @param reason  reason of error
     * @return the result of negative commit ,OK or other
     */
    public String negative(long[] indexes, String code, String reason) throws Exception {
        validateIndexes(indexes);
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_indexes_", StringUtils.join(ArrayUtils.toObject(indexes), ","));
                    put("_negative_", Boolean.toString(true));
                }
            };

        Map<String, String> dataParams = new HashMap<>();
        if (code != null && !code.isEmpty()) {
            dataParams.put("_code_", code);
        }
        if (reason != null && !reason.isEmpty()) {
            dataParams.put("_reason_", reason);
        }
        String formData = createURI("", dataParams);
        if (formData.length() > 0) {
            formData = formData.substring(1);
        }

        String uri = createURI(baseUri(), queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        SimpleHttpRequest request = SimpleHttpRequest.create("PUT", uri);
        headers.forEach(request::setHeader);
        request.setBody(formData.getBytes(StandardCharsets.UTF_8), null);

        SimpleHttpResponse response = retryRequest(request);
        return responseBody(response);
    }

    /**
     * clear all data in queue service
     */
    public void clear() throws Exception {
        JSONObject attrs = this.attributes();
        String lastEntry = attrs.getString("stream.lastEntry");
        if (lastEntry != null) {
            this.truncate(Long.parseLong(lastEntry) + 1);
        }
    }

    /**
     * close QueueClient, including an active watcher.
     */
    @Override
    protected QueueClient self() {
        return this;
    }

    @Override
    public QueueClient setRetryCount(int retryCount) {
        return super.setRetryCount(retryCount);
    }

    @Override
    public QueueClient addExtraHeaders(Map<String, String> extraHeaders) {
        return super.addExtraHeaders(extraHeaders);
    }

    @Override
    public QueueClient setHttpClient(CloseableHttpAsyncClient httpClient) {
        return super.setHttpClient(httpClient);
    }

    @Override
    public void shutdown() {
        WebSocketWatcher watcher = activeWatcher;
        if (watcher != null) {
            watcher.close();
        }
        super.shutdown();
    }
}
