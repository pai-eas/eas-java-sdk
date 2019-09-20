package com.aliyun.openservices.eas.discovery.core;

import com.alibaba.fastjson.annotation.JSONField;


public class Endpoint {
    private boolean isValid = true;
    @JSONField(name = "app")
    private String app;

    @JSONField(name = "ip")
    private String ip;

    @JSONField(name = "port")
    private int port;


    @JSONField(name = "weight")
    private int weight;

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        this.isValid = valid;
    }

    @Override
    public String toString() {
        return this.toInetAddr();
    }

    public String getApp() {
        return this.app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String toInetAddr() {
        try {
            return getIp() + ":" + getPort();
        } catch (NumberFormatException e) {
            return getIp();
        }
    }

    public int getWeight() {
        return this.weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
