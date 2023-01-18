package com.aliyun.openservices.eas.predict.queue_client;

import com.aliyun.openservices.eas.predict.proto.QueueServiceProtos.DataFrameProto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import shade.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.util.Map;

public class DataFrame {
  private DataFrameProto.Builder data_frame = DataFrameProto.newBuilder();
  private static Log log = LogFactory.getLog(DataFrame.class);

  public DataFrame() {}

  public DataFrame(DataFrameProto df) {
    data_frame.setIndex(df.getIndex());
    data_frame.putAllTags(df.getTagsMap());
    data_frame.setData(df.getData());
    data_frame.setMessage(df.getMessage());
  }

  public void setIndex(long index) {
    data_frame.setIndex(index);
  }

  public void setTags(String key, String val) {
    data_frame.putTags(key, val);
  }

  public void setAllTags(Map<String, String> map) {
    data_frame.putAllTags(map);
  }

  public void setMessage(String message) {
    data_frame.setMessage(message);
  }

  public long getIndex() {
    return data_frame.getIndex();
  }

  public Map<String, String> getTags() {
    return data_frame.getTagsMap();
  }

  public byte[] getData() {
    return data_frame.getData().toByteArray();
  }

  public String getMessage() {
    return data_frame.getMessage();
  }

  public DataFrameProto getDataFrameProto() {
    return data_frame.build();
  }

  public byte[] encode() {
    return data_frame.build().toByteArray();
  }

  public DataFrame decode(byte[] dfProtoBytes) {
    try {
      DataFrameProto df = DataFrameProto.parseFrom(dfProtoBytes);
      this.data_frame.clear();
      this.data_frame.mergeFrom(df);
    } catch (InvalidProtocolBufferException e) {
      log.error("Invalid Protocol Buffer", e);
    }
    return this;
  }

  public DataFrame decode(ByteBuffer dfProtoByteBuffer) {
    byte[] dfProtoBytes = new byte[dfProtoByteBuffer.remaining()];
    dfProtoByteBuffer.get(dfProtoBytes);
    return decode(dfProtoBytes);
  }
}
