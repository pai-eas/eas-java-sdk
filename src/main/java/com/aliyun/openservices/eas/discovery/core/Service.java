package com.aliyun.openservices.eas.discovery.core;

import com.alibaba.fastjson.annotation.JSONField;
import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Service {
    public static final String SPLITER = "@@";
    @JSONField(serialize = false)
    private String jsonFromServer = StringUtils.EMPTY;
    private String name;

    private String clusters;

    private long cacheMillis = Long.valueOf(System.getProperty("com.aliyun.eas.cachemillis", "1000"));

    public static class EndpointItems {
        @JSONField(name = "items")
        private List<Endpoint> items = new ArrayList<>();

        public List<Endpoint> getItems() {
            return new ArrayList<>(items);
        }

        public void setItems(List<Endpoint> endpoints) {
            this.items = endpoints;
        }
    }


    @JSONField(name = "endpoints")
    private EndpointItems endpoints = new EndpointItems();

    @JSONField(name = "endpoints")
    public EndpointItems getEndpointItems() {
        return this.endpoints;
    }

    @JSONField(name = "endpoints")
    public void setEndpointItems(EndpointItems endpoints) {
        this.endpoints = endpoints;
    }


    private long lastRefTime = System.currentTimeMillis();

    public Service() {
    }


    public Service(String key) {
        String[] keys = key.split(SPLITER);
        if (keys.length >= 4) {
            this.name = keys[0];
            this.clusters = keys[1];
        } else if (keys.length >= 3) {
            this.name = keys[0];
            this.clusters = keys[1];
        } else if (keys.length >= 2) {
            this.name = keys[0];
            this.clusters = keys[1];
        }

        this.name = keys[0];
    }

    public Service(String name, String clusters) {
        this.name = name;
        this.clusters = clusters;
    }

    @JSONField(serialize = false)
    public static String getKey(String name, String clusters) {
        if (!StringUtils.isEmpty(clusters)) {
            return name + SPLITER + clusters;
        }

        return name;
    }

    public int ipCount() {
        return endpoints.getItems().size();
    }

    public boolean expired() {
        return System.currentTimeMillis() - lastRefTime > cacheMillis;
    }

    public boolean isValid() {
        return endpoints.getItems() != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastRefTime() {
        return lastRefTime;
    }

    public void setLastRefTime(long lastRefTime) {
        this.lastRefTime = lastRefTime;
    }

    public String getClusters() {
        return clusters;
    }

    public void setClusters(String clusters) {
        this.clusters = clusters;
    }

    public long getCacheMillis() {
        return cacheMillis;
    }

    public void setCacheMillis(long cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    public List<Endpoint> getEndpoints() {
        return this.endpoints.getItems();
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints.setItems(endpoints);
    }

    public boolean validate() {
        if (CollectionUtils.isEmpty(endpoints.getItems())) {
            return false;
        }

        List<Endpoint> validEndpoints = new ArrayList<com.aliyun.openservices.eas.discovery.core.Endpoint>();
        for (Endpoint endpoint : endpoints.getItems()) {
            if (!endpoint.isValid()) {
                continue;
            }

            for (int i = 0; i < endpoint.getWeight(); i++) {
                validEndpoints.add(endpoint);
            }
        }

        return !CollectionUtils.isEmpty(validEndpoints);
    }

    @JSONField(serialize = false)
    public String getJsonFromServer() {
        return jsonFromServer;
    }

    public void setJsonFromServer(String jsonFromServer) {
        this.jsonFromServer = jsonFromServer;
    }

    @JSONField(serialize = false)
    public String getKey() {
        return getKey(name, clusters);
    }

    @Override
    public String toString() {
        return getKey();
    }
}
