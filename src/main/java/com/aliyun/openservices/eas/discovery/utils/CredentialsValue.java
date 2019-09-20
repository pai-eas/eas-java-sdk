package com.aliyun.openservices.eas.discovery.utils;

public class CredentialsValue {

    private volatile String accessKey;

    private volatile String secretKey;


    public CredentialsValue() {
        super();
        // TODO Auto-generated constructor stub
    }

    public CredentialsValue(String accessKey, String secretKey) {
        super();
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

}
