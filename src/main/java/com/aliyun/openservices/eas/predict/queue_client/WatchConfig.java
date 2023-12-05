package com.aliyun.openservices.eas.predict.queue_client;

public class WatchConfig {
    public static int DefaultReConnectCnt = 20;
    public static int DefaultReConnectInterval = 5;

    private int reConCnt;
    private int reConInterval;
    private boolean infinityReConnect;

    public WatchConfig() {
        this.reConCnt = DefaultReConnectCnt;
        this.reConInterval = DefaultReConnectInterval;
        this.infinityReConnect = false;
    }

    public WatchConfig(int reConCnt, int reConInterval) {
        this.reConCnt = reConCnt;
        this.reConInterval = reConInterval;
    }

    public WatchConfig(boolean infinityReConnect, int reConInterval) {
        this.infinityReConnect = infinityReConnect;
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
        return infinityReConnect;
    }

    public void setUnLimitedReCon(boolean unLimitedReCon) {
        this.infinityReConnect = unLimitedReCon;
    }

    public boolean isInfinityReConnect() {
        return infinityReConnect;
    }

    public void setInfinityReConnect(boolean infinityReConnect) {
        this.infinityReConnect = infinityReConnect;
    }
}
