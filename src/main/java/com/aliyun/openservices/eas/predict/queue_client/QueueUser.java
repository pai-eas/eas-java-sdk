package com.aliyun.openservices.eas.predict.queue_client;

public class QueueUser {
  private String uid;
  private String gid;
  private String token;

  public QueueUser(String uid, String gid, String token) {
    this.uid = uid;
    this.gid = gid;
    this.token = token;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getGid() {
    return gid;
  }

  public void setGid(String gid) {
    this.gid = gid;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
