package com.aliyun.openservices.eas.predict.queue_client;

public class WatchConfig {
    public static int DefaultReConnectCnt = 20;
    public static int DefaultReConnectInterval = 5;

    private int reConCnt = DefaultReConnectCnt;
    private int reConInterval = DefaultReConnectInterval;
    private boolean infinityReConnect;

    public WatchConfig() {
    }

    public WatchConfig(int reConCnt, int reConInterval) {
        setReConCnt(reConCnt);
        setReConInterval(reConInterval);
    }

    public WatchConfig(boolean infinityReConnect, int reConInterval) {
        this.infinityReConnect = infinityReConnect;
        setReConInterval(reConInterval);
    }

    public int getReConCnt() {
        return reConCnt;
    }

    public void setReConCnt(int reConCnt) {
        this.reConCnt = Math.max(0, reConCnt);
    }

    public int getReConInterval() {
        return reConInterval;
    }

    public void setReConInterval(int reConInterval) {
        this.reConInterval = Math.max(1, reConInterval);
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
