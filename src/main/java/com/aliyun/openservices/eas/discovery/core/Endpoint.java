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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Endpoint)) {
            return false;
        }
        Endpoint other = (Endpoint) o;
        return port == other.port
                && weight == other.weight
                && isValid == other.isValid
                && (ip == null ? other.ip == null : ip.equals(other.ip))
                && (app == null ? other.app == null : app.equals(other.app));
    }

    @Override
    public int hashCode() {
        int result = ip == null ? 0 : ip.hashCode();
        result = 31 * result + port;
        result = 31 * result + weight;
        result = 31 * result + (app == null ? 0 : app.hashCode());
        result = 31 * result + (isValid ? 1 : 0);
        return result;
    }
}
