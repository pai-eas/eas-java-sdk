package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.request.CaffeRequest;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.CaffeResponse;
import com.aliyun.openservices.eas.predict.response.JsonResponse;
import com.aliyun.openservices.eas.predict.response.TFResponse;
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
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by xiping.zk on 2018/07/10.
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
    private int errorCode = 0;
    private String errorMessage;
    private String vipSrvEndPoint = null;
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

    public PredictClient setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public PredictClient setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public PredictClient setVIPServer(String vipSrvEndPoint) {
        this.vipSrvEndPoint = vipSrvEndPoint;
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
        } else {
            client.setEndpoint(this.endpoint);
        }
        return client;
    }

    private String buildUri() {
        return "http://" + endpoint + "/api/predict/" + modelName;
    }

    private HttpPost generateSignature(byte[] requestContent) {
        if (vipSrvEndPoint != null) {
            try {
                setEndpoint(VIPClient.srvHost(vipSrvEndPoint).toInetAddr());
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        if (mapHeader != null)
            request.addHeader("Client-Timestamp",
                    String.valueOf(System.currentTimeMillis()));

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
            InterruptedException, ExecutionException {
        byte[] content = null;
        HttpResponse response = null;
        Future<HttpResponse> future = httpclient.execute(request, null);
        response = future.get();

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
                    if (isCompressed)
                        content = Snappy.uncompress(content);
                } else {
                    errorMessage = IOUtils.toString(response.getEntity()
                            .getContent(), "UTF-8");
                    throw new IOException("Status Code: " + errorCode
                            + " Predict Failed: " + errorMessage);
                }
            } catch (IllegalStateException e) {
                log.error("Illegal State", e);
            }
        } else if (future.isCancelled()) {
            log.error("request cancelled!", new Exception("Request cancelled"));
        } else {
            throw new IOException("request failed!");
        }
        return content;
    }

    public TFResponse predict(TFRequest runRequest) {
        TFResponse runResponse = new TFResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public CaffeResponse predict(CaffeRequest runRequest) {
        CaffeResponse runResponse = new CaffeResponse();
        byte[] result = predict(runRequest.getRequest().toByteArray());
        if (result != null) {
            runResponse.setContentValues(result);
        }
        return runResponse;
    }

    public JsonResponse predict(JsonRequest requestContent)
            throws JsonGenerationException, JsonMappingException, IOException {
        byte[] result = predict(defaultObjectMapper
                .writeValueAsBytes(requestContent));
        
        JsonResponse jsonResponse = null;
        if (result != null) {
            jsonResponse = defaultObjectMapper.readValue(result, 0,
                    result.length, JsonResponse.class);
        }
        return jsonResponse;
    }

    public String predict(String requestContent) {
        byte[] result = predict(requestContent.getBytes());
        if (result != null) {
            return new String(result);
        }
        return null;
    }

    public byte[] predict(byte[] requestContent) {
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
        int count = 0;
        for (int i = 0; i < retryCount; i++) {
            try {
                content = getContent(request);
            } catch (Exception e) {
                if (i == retryCount - 1) {
                    if (configCount < 0 || count >= configs.length - 1) {
                        log.error("Exception Error", e);
                    } else {
                        configCount = ++configCount % configs.length;
                        changeConfig(configCount);
                        request = generateSignature(requestContent);
                        i = -1;
                        count++;
                    }
                }
                continue;
            }
            break;
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

