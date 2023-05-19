package com.aliyun.openservices.eas.predict.queue_client;

import java.util.UUID;

public class QueueUser {
  public static String DefaultGroupName = "eas";
  private String uid;
  private String gid;
  private String token;

  public QueueUser(String uid, String gid) {
    this.uid = uid;
    this.gid = gid;
  }

  public QueueUser() {
    this.uid = UUID.randomUUID().toString();
    this.gid = DefaultGroupName;
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
