package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.request.*;
import com.aliyun.openservices.eas.predict.response.*;
import com.taobao.vipserver.client.core.VIPClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by xiping.zk on 2018/07/25.
 */
public class PredictClient {
    final private int endpointRetryCount = 10;
    private static Log log = LogFactory.getLog(PredictClient.class);
    private CloseableHttpAsyncClient httpclient = null;
    private String token = null;
    private String modelName = null;
    private String endpoint = null;
    private boolean isCompressed = false;
    HashMap<String, String> mapHeader = null;
    private int retryCount = 3;
    private String contentType = "application/octet-stream";
    private int errorCode = 400;
    private String errorMessage;
    private String vipSrvEndPoint = null;
    private String directEndPoint = null;
    private int requestTimeout = 0;

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

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public PredictClient createChlidClient(String token, String endPoint,
            String modelName) {
        PredictClient client = new PredictClient();
        client.setHttp(this.httpclient).setToken(token).setEndpoint(endPoint)
                .setModelName(modelName);
        return client;
    }

    public PredictClient createChlidClient() {
        PredictClient client = new PredictClient();
        client.setHttp(this.httpclient).setToken(this.token)
                .setModelName(this.modelName);
        if (this.vipSrvEndPoint != null) {
            client.setVIPServer(this.vipSrvEndPoint);
        } else if (this.directEndPoint != null) {
            client.setDirectEndpoint(this.directEndPoint);
        } else {
            client.setEndpoint(this.endpoint);
        }
        return client;
    }

    private String getUrl(String lastUrl) throws Exception {
        String endpoint = this.endpoint;
        String url = "";
        for (int i = 0; i < endpointRetryCount; i++) {
            if (vipSrvEndPoint != null) {
                endpoint = VIPClient.srvHost(vipSrvEndPoint).toInetAddr();
                url = "http://" + endpoint + "/api/predict/" + modelName;
                if (VIPClient.srvHosts(vipSrvEndPoint).size() < 2) {
                    return url;
                }
                // System.out.println("URL: " + url + " LastURL: " + lastUrl);
                if (!url.equals(lastUrl)) {
                    return url;
                }
            } else if (directEndPoint != null) {
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
        if(result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public String predict(String requestContent) throws Exception{
        byte[] result = predict(requestContent.getBytes());
        if (result != null) {
            return new String(result);
        }
        return null;
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
