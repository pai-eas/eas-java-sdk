package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.predict.request.*;
import com.aliyun.openservices.eas.predict.response.*;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author jiankeng.pt
 * @date 2021/09/10
 */

/**
 * ODLPredictClient is created for accessing prediction service.
 * User can specify a shard_key, at backend it will split service into
 * several sub-services according the shard_key, and the client will
 * send request to one sub-service according hash(shard_key) % var_shard_count.
 */
public class ODLPredictClient {
    private static final int hashBlocksCount = 1000;
    private List<PredictClient> clients;
    private int varShardCount = 1;
    private HttpConfig httpConfig;

    public ODLPredictClient() {
        Init();
    }

    /**
     * Create an ODLPredictClient.
     *
     * @param varShardCount sharding count of the embedding variable.
     */
    public ODLPredictClient(int varShardCount) {
        this.varShardCount = varShardCount;
        Init();
    }

    /**
     * Create an ODLPredictClient.
     *
     * @param httpConfig http configuration
     */
    public ODLPredictClient(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
        Init();
    }

    public ODLPredictClient(HttpConfig httpConfig, int varShardCount) {
        this.httpConfig = httpConfig;
        this.varShardCount = varShardCount;
        Init();
    }

    private void Init() {
        clients = new ArrayList<PredictClient>();
        for (int i = 0; i < varShardCount; i++) {
            clients.add(new PredictClient(this.httpConfig));
        }
    }

    private int GetServiceId(String key) {
        long hashValue = Hashing.farmHashFingerprint64().hashBytes(key.getBytes()).asLong();
        hashValue &= Long.MAX_VALUE;
        return (int) (hashValue % this.hashBlocksCount % this.varShardCount);
    }

    private int GetServiceId(long key) {
        return (int) (key % this.hashBlocksCount % this.varShardCount);
    }

    public ODLPredictClient setToken(String token) {
        if (token == null || token.length() > 0) {
            for (PredictClient client : this.clients) {
                client.setToken(token);
            }
        }
        return this;
    }

    public ODLPredictClient setRequestTimeout(int requestTimeout) {
        for (PredictClient client : this.clients) {
            client.setRequestTimeout(requestTimeout);
        }
        return this;
    }

    public ODLPredictClient setModelName(String modelName) {
        this.clients.get(0).setModelName(modelName);
        for (int i = 1; i < this.clients.size(); i++) {
            this.clients.get(i).setModelName(modelName + "_" + Integer.toString(i));
        }
        return this;
    }

    public ODLPredictClient setEndpoint(String endpoint) {
        for (PredictClient client : this.clients) {
            client.setEndpoint(endpoint);
        }
        return this;
    }

    public ODLPredictClient setVIPServer(String vipSrvEndPoint) {
        if (vipSrvEndPoint == null || vipSrvEndPoint.length() > 0) {
            for (PredictClient client : this.clients) {
                client.setVIPServer(vipSrvEndPoint);
            }
        }
        return this;
    }

    public ODLPredictClient setDirectEndpoint(String directEndpoint) {
        if (directEndpoint == null || directEndpoint.length() > 0) {
            for (PredictClient client : this.clients) {
                client.setDirectEndpoint(directEndpoint);
            }
        }
        return this;
    }

    public ODLPredictClient setIsCompressed(boolean isCompressed) {
        for (PredictClient client : this.clients) {
            client.setIsCompressed(isCompressed);
        }
        return this;
    }

    public ODLPredictClient setRetryCount(int retryCount) {
        for (PredictClient client : this.clients) {
            client.setRetryCount(retryCount);
        }
        return this;
    }

    public ODLPredictClient setTracing(HashMap<String, String> mapHeader) {
        for (PredictClient client : this.clients) {
            client.setTracing(mapHeader);
        }
        return this;
    }

    public ODLPredictClient setContentType(String contentType) {
        for (PredictClient client : this.clients) {
            client.setContentType(contentType);
        }
        return this;
    }

    public BladeResponse predict(BladeRequest runRequest) throws Exception {
        return this.clients.get(0).predict(runRequest);
    }

    public BladeResponse predict(BladeRequest runRequest, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public BladeResponse predict(BladeRequest runRequest, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public TFResponse predict(TFRequest runRequest) throws Exception {
        return this.clients.get(0).predict(runRequest);
    }

    public TFResponse predict(TFRequest runRequest, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public TFResponse predict(TFRequest runRequest, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public CaffeResponse predict(CaffeRequest runRequest) throws Exception {
        return this.clients.get(0).predict(runRequest);
    }

    public CaffeResponse predict(CaffeRequest runRequest, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public CaffeResponse predict(CaffeRequest runRequest, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public JsonResponse predict(JsonRequest requestContent)
            throws Exception {
        return this.clients.get(0).predict(requestContent);
    }

    public JsonResponse predict(JsonRequest requestContent, String shardKey)
            throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public JsonResponse predict(JsonRequest requestContent, long shardKey)
            throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public TorchResponse predict(TorchRequest runRequest) throws Exception {
        return this.clients.get(0).predict(runRequest);
    }

    public TorchResponse predict(TorchRequest runRequest, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public TorchResponse predict(TorchRequest runRequest, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(runRequest);
    }

    public String predict(String requestContent) throws Exception {
        return this.clients.get(0).predict(requestContent);
    }

    public String predict(String requestContent, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public String predict(String requestContent, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public byte[] predict(byte[] requestContent) throws Exception {
        return this.clients.get(0).predict(requestContent);
    }

    public byte[] predict(byte[] requestContent, String shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public byte[] predict(byte[] requestContent, long shardKey) throws Exception {
        return this.clients.get(GetServiceId(shardKey)).predict(requestContent);
    }

    public void shutdown() {
        for (PredictClient client : this.clients) {
            client.shutdown();
        }
    }
}
