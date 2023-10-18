package com.aliyun.openservices.eas.predict.http;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.eas.predict.queue_client.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.java_websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client for accessing prediction service by creating a fixed size connection pool to perform the
 * request through established persistent connections.
 */
public class QueueClient {
    public static String HeaderRequestId = "X-Eas-Queueservice-Request-Id";
    public static String HeaderAuthorization = "Authorization";
    public static String HeaderRedisUid = "X-EAS-QueueService-Redis-Uid";
    public static String HeaderRedisGid = "X-EAS-QueueService-Redis-Gid";

    private static Log log = LogFactory.getLog(QueueClient.class);
    public ReentrantLock lock = new ReentrantLock();
    public WebSocketClient webSocketClient = null;
    private String baseUrl = "";
    private QueueUser user = null;
    private CloseableHttpAsyncClient httpclient = null;
    private int retryCount = 5;
    private boolean websocketWatch = false;
    private String prioHeader = null;

    public QueueClient() {
    }

    public QueueClient(
        String endpoint, String queueName, String token, HttpConfig httpConfig, QueueUser user) {
        this(String.format("%s/api/predict/%s", endpoint, queueName), token, httpConfig, user);
    }

    public QueueClient(
        String url, String token, HttpConfig httpConfig, QueueUser user) {
        baseUrl = url;
        if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            baseUrl = String.join("", "http://", baseUrl);
        }

        try {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
            PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
            cm.setMaxTotal(httpConfig.getMaxConnectionCount());
            cm.setDefaultMaxPerRoute(httpConfig.getMaxConnectionPerRoute());
            IOReactorConfig config =
                IOReactorConfig.custom()
                    .setTcpNoDelay(true)
                    .setSoTimeout(httpConfig.getReadTimeout())
                    .setSoReuseAddress(true)
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setIoThreadCount(httpConfig.getIoThreadNum())
                    .setSoKeepAlive(httpConfig.isKeepAlive())
                    .build();
            final RequestConfig requestConfig =
                RequestConfig.custom()
                    .setConnectTimeout(httpConfig.getConnectTimeout())
                    .setSocketTimeout(httpConfig.getReadTimeout())
                    .build();
            httpclient =
                HttpAsyncClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultIOReactorConfig(config)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            httpclient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.user = user;
        this.user.setToken(token);
        websocketWatch = true;
    }

    /**
     * add identity info into headers
     *
     * @param headers http request headers
     * @return headers with identity
     */
    private Map<String, String> withIdentity(Map<String, String> headers) {
        headers.put(HeaderAuthorization, user.getToken());
        headers.put(HeaderRedisUid, user.getUid());
        headers.put(HeaderRedisGid, user.getGid());
        return headers;
    }

    private void withPriority(HttpUriRequest request, Long priority) throws Exception {
        if (priority > 0) {
            if (prioHeader == null) {
                prioHeader = attributes().getString("meta.header.priority");
            }
            if (prioHeader != null) {
                request.setHeader(prioHeader, Long.toString(priority));
            }
        }
    }

    private HttpUriRequest buildRequest(String method, Map<String, String> queryParams)
        throws Exception {
        String uri = createURI(baseUrl, queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        HttpUriRequest request = null;
        if (method.equals("DELETE")) {
            // create HttpDelete
            request = new HttpDelete(uri);
        } else if (method.equals("POST")) {
            // create HttpPost
            request = new HttpPost(uri);
        } else if (method.equals("PUT")) {
            // create HttpPut
            request = new HttpPut(uri);
        } else if (method.equals("GET")) {
            // create HttpGet
            request = new HttpGet(uri);
        }
        headers.forEach(request::setHeader);
        return request;
    }

    /**
     * common code for excute a http request
     *
     * @param request http request
     * @return HttpResponse
     */
    private HttpResponse doRequest(HttpUriRequest request) throws Exception {
        String uri = request.getURI().toString();
        try {
            Future<HttpResponse> future = httpclient.execute(request, null);
            HttpResponse response = future.get();
            if (future.isDone()) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 && statusCode >= 300) {
                    String errorMessage = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    throw new HttpException(
                        statusCode,
                        String.format(
                            "visiting: %s, unexpected status code: %d, message: %s",
                            uri, statusCode, errorMessage));
                }
                return response;
            } else {
                throw new HttpException(-1, "request failed!");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private HttpResponse retryRequest(HttpUriRequest request) throws Exception {
        for (int i = 1; i <= retryCount; ++i) {
            try {
                HttpResponse response = doRequest(request);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    return response;
                } else if (i == retryCount && response != null && response.getEntity() != null) {
                    log.warn(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
                }
            } catch (Exception e) {
                if (i == retryCount) {
                    log.error(e.getMessage());
                    throw e;
                } else {
                    log.debug(e.getMessage());
                }
            }
        }
        return null;
    }

    private String createURI(String baseUrl, Map<String, String> queryParams) throws Exception {
        URIBuilder ub = new URIBuilder(baseUrl);
        queryParams.forEach(ub::addParameter);
        return ub.build().toString();
    }

    /**
     * common code for indexes related request
     *
     * @param indexes array of data index
     * @param method  http request method: "DELETE", "PUT"
     * @return content of httpResponse body
     */
    private String processIndexes(long[] indexes, String method) throws Exception {
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_indexes_", StringUtils.join(ArrayUtils.toObject(indexes), ","));
                }
            };
        HttpResponse response = retryRequest(buildRequest(method, queryParams));
        if (response != null && response.getEntity() != null) {
            return IOUtils.toString(response.getEntity().getContent());
        }
        return "";
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

        HttpResponse response = retryRequest(buildRequest("GET", queryParams));
        if (response != null) {
            String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            return JSONObject.parseObject(content.trim());
        }
        return JSONObject.parseObject("{}");
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
            log.warn("Invalid value of priority, should be a non-negative number");
            return -1L;
        }
        if (tags != null && !tags.isEmpty()) {
            queryParams.putAll(tags);
        }

        HttpResponse response = retryRequest(buildRequest("GET", queryParams));
        if (response != null) {
            String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            return Long.parseLong(content.trim());
        }
        return -1L;
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

        HttpResponse response = retryRequest(buildRequest("GET", queryParams));
        if (response != null) {
            String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            return JSONObject.parseObject(content.trim());
        }
        return JSONObject.parseObject("{}");
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
        doRequest(buildRequest("DELETE", queryParams));
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
        doRequest(buildRequest("POST", queryParams));
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
        Map<String, String> queryParams = new HashMap<String, String>();
        if (priority > 0) {
            queryParams.put("_priority_", Long.toString(priority));
        } else if (priority < 0) {
            log.warn("Invalid value of priority, should be a non-negative number, now set to normal priority: 0");
        }
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(
                (key, value) -> {
                    queryParams.put(key, value);
                });
        }
        String uri = createURI(baseUrl, queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());

        // create HttpPost
        HttpPost request = new HttpPost(uri);
        headers.forEach(request::setHeader);
        request.setEntity(new NByteArrayEntity(data));
        withPriority(request, priority);

        for (int i = 1; i <= retryCount; ++i) {
            try {
                HttpResponse response = doRequest(request);
                // check response statusCode
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    // get requestId from response header
                    Header[] headerOfRequestId = response.getHeaders(HeaderRequestId);
                    if (headerOfRequestId == null || headerOfRequestId.length < 1) {
                        Header[] all_headers = response.getAllHeaders();
                        log.error("Failed to get Header with Request-Id, all headers of response are:");
                        for (Header header : all_headers) {
                            log.error(header.getName() + ": " + header.getValue());
                        }
                        String errorMessage = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                        throw new HttpException(500, errorMessage);
                    }
                    String requestId = headerOfRequestId[0].getValue();

                    return Pair.of(Long.valueOf(content), requestId);
                } else {
                    String errorMessage = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    throw new HttpException(statusCode, errorMessage);
                }
            } catch (Exception e) {
                if (i == retryCount) {
                    log.error(e.getMessage());
                    throw e;
                } else {
                    log.debug(e.getMessage());
                }
            }
        }
        return Pair.of(0L, "");
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
        return put(data.getBytes(), tags);
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
        Map<String, String> queryParams =
            new HashMap<String, String>() {
                {
                    put("_index_", Long.toString(index));
                    put("_length_", Long.toString(length));
                    put("_timeout_", String.format("%ds", timeout));
                    put("_raw_", Boolean.toString(false));
                    put("_auto_delete_", Boolean.toString(autoDelete));
                }
            };
        if (tags != null && !tags.isEmpty()) {
            queryParams.putAll(tags);
        }
        String uri = createURI(baseUrl, queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Accept", "application/vnd.google.protobuf");

        // create HttpGet
        HttpGet request = new HttpGet(uri);
        headers.forEach(request::setHeader);

        HttpResponse response = retryRequest(request);
        if (response != null) {
            byte[] content = IOUtils.toByteArray(response.getEntity().getContent());
            return new DataFrameList().decode(content).getList();
        } else {
            return new DataFrame[0];
        }
    }

    /**
     * get the data in queue service by index
     *
     * @param index data index of long
     * @return DataFrame array
     */
    public DataFrame[] getByIndex(long index) throws Exception {
        return get(index, 1L, 0, true, null);
    }

    /**
     * get the data in queue service by index
     *
     * @param requestId requestId
     * @return DataFrame array
     */
    public DataFrame[] getByRequestId(String requestId) throws Exception {
        Map<String, String> tags =
            new HashMap<String, String>() {
                {
                    put("requestId", requestId);
                }
            };
        return get(0L, 1L, 0, true, tags);
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
        WatchConfig watchConfig = new WatchConfig(true, 5);
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
        if (queueTags != null && !queueTags.isEmpty()) {
            queryParams.putAll(queueTags);
        }
        String uri = createURI(baseUrl, queryParams);
        uri = uri.replaceFirst("http", "ws");
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Accept", "application/vnd.google.protobuf");

        if (watchConfig.getReConInterval() < 0) {
            log.warn("Invalid value of reConnect interval. The value should be a non-negative number, set as default value: " + watchConfig.DefaultReConnectInterval);
            watchConfig.setReConInterval(watchConfig.DefaultReConnectInterval);
        }
        if (!watchConfig.isUnLimitedReCon() && watchConfig.getReConCnt() < 0) {
            log.warn("Invalid value of reConnect count. The value should be a non-negative number, set as default value: " + watchConfig.DefaultReConnectCnt);
            watchConfig.setReConCnt(watchConfig.DefaultReConnectCnt);
        }
        return new WebSocketWatcher(this, new URI(uri), headers, watchConfig);
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
            formData = formData.substring(1, formData.length());
        }

        String uri = createURI(baseUrl, queryParams);
        Map<String, String> headers = withIdentity(new HashMap<String, String>());
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        HttpPut request = new HttpPut(uri);
        headers.forEach(request::setHeader);
        request.setEntity(new NByteArrayEntity(formData.getBytes()));

        HttpResponse response = retryRequest(request);
        if (response != null && response.getEntity() != null) {
            return IOUtils.toString(response.getEntity().getContent());
        }
        return "";
    }

    /**
     * clear all data in queue service
     */
    public void clear() throws Exception {
        JSONObject attrs = this.attributes();
        if (attrs.containsKey("stream.lastEntry")) {
            this.truncate(Long.parseLong(attrs.getString("stream.lastEntry")) + 1);
        }
    }

    /**
     * close QueueClient
     */
    public void shutdown() {
        try {
            if (httpclient != null) {
                httpclient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
