package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.request.CaffeRequest;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.request.TorchRequest;
import com.aliyun.openservices.eas.predict.response.CaffeResponse;
import com.aliyun.openservices.eas.predict.response.JsonResponse;
import com.aliyun.openservices.eas.predict.response.TFResponse;
import com.aliyun.openservices.eas.predict.response.TorchResponse;
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
import org.codehaus.jackson.map.ObjectMapper;
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
    private static Log log = LogFactory.getLog(PredictClient.class);
    private CloseableHttpAsyncClient httpclient = null;
    private String token = null;
    private String modelName = null;
    private String endpoint = null;
    private boolean isCompressed = false;
    HashMap<String, String> mapHeader = null;
    private int retryCount = 3;
    private String[][] configs;
    private int configCount = -1;
    private int heartCount = 0;
    private int heartLimit = 1000;
    private String contentType = "application/octet-stream";
    private int errorCode = 400;
    private String errorMessage;
    private String vipSrvEndPoint = null;
    private String directEndPoint = null;
    private int requestTimeout = 0;
    ObjectMapper defaultObjectMapper = new ObjectMapper();

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

    private void changeConfig(int i) {
        this.token = configs[i][0];
        this.endpoint = configs[i][1];
        this.modelName = configs[i][2];
    }

    public PredictClient setToken(String token) {
        this.token = token;
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

    public PredictClient setMultiConfig(String[][] configs) {
        this.configs = configs.clone();
        this.configCount = 0;
        changeConfig(configCount);
        return this;
    }

    public PredictClient setMultiConfig(String[][] configs, int heartLimit) {
        this.configs = configs.clone();
        this.configCount = 0;
        changeConfig(configCount);
        this.heartLimit = heartLimit;
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

    private String buildUri() {
        return "http://" + endpoint + "/api/predict/" + modelName;
    }

    private HttpPost generateSignature(byte[] requestContent) throws Exception {
        if (vipSrvEndPoint != null) {
            setEndpoint(VIPClient.srvHost(vipSrvEndPoint).toInetAddr());
        } else if (directEndPoint != null) {
            setEndpoint(DiscoveryClient.srvHost(this.modelName).toInetAddr());
        }

        HttpPost request = new HttpPost(buildUri());
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

        String auth = "POST" + "\n" + md5Content + "\n"
                + "application/octet-stream" + "\n" + currentTime + "\n"
                + "/api/predict/" + modelName;
        if (token != null) {
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
        byte[] result = predict(defaultObjectMapper.writeValueAsString(requestContent).getBytes());

        JsonResponse jsonResponse = null;
        if (result != null) {
            jsonResponse = defaultObjectMapper.readValue(result, 0,
                    result.length, JsonResponse.class);
        }
        return jsonResponse;
    }

    public String predict(String requestContent) throws Exception{
        byte[] result = predict(requestContent.getBytes());
        if (result != null) {
            return new String(result);
        }
        return null;
    }

    public byte[] predict(byte[] requestContent) throws Exception{
        HttpPost request = generateSignature(requestContent);
        if (configCount >= 0) {
            heartCount++;
            if (heartCount >= heartLimit) {
                heartCount = 0;
                configCount = 0;
                changeConfig(configCount);
                request = generateSignature(requestContent);
            }
        }
        byte[] content = null;
        for (int i = 0; i <= retryCount; i++) {
            try {
                content = getContent(request);
                break;
            } catch (Exception e) {
                if (configCount >= 0) {
                    configCount = ++configCount % configs.length;
                    changeConfig(configCount);
                    request = generateSignature(requestContent);
                }
                if (i == retryCount) {
                    log.error("Exception Error: " + e.getMessage());
                    throw e;
                } else {
                    log.debug("Exception Error: " + e.getMessage());
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

    public TorchResponse predict(TorchRequest runRequest) throws Exception {
        TorchResponse runResponse = new TorchResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if(result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

}

