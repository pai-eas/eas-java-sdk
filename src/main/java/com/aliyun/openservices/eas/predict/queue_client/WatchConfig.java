package com.aliyun.openservices.eas.predict.queue_client;

public class WatchConfig {
    public static int DefaultReConnectCnt = 20;
    public static int DefaultReConnectInterval = 5;

    private int reConCnt;
    private int reConInterval;
    private boolean unLimitedReCon;

    public WatchConfig() {
        this.reConCnt = DefaultReConnectCnt;
        this.reConInterval = DefaultReConnectInterval;
        this.unLimitedReCon = false;
    }

    public WatchConfig(int reConCnt, int reConInterval) {
        this.reConCnt = reConCnt;
        this.reConInterval = reConInterval;
    }

    public WatchConfig(boolean unLimitedReCon, int reConInterval) {
        this.unLimitedReCon = unLimitedReCon;
        this.reConInterval = reConInterval;
    }

    public int getReConCnt() {
        return reConCnt;
    }

    public void setReConCnt(int reConCnt) {
        this.reConCnt = reConCnt;
    }

    public int getReConInterval() {
        return reConInterval;
    }

    public void setReConInterval(int reConInterval) {
        this.reConInterval = reConInterval;
    }

    public boolean isUnLimitedReCon() {
        return unLimitedReCon;
    }

    public void setUnLimitedReCon(boolean unLimitedReCon) {
        this.unLimitedReCon = unLimitedReCon;
    }
}
