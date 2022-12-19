package com.aliyun.openservices.eas.predict.queue_client;

import com.aliyun.openservices.eas.predict.proto.QueueServiceProtos.DataFrameListProto;
import com.aliyun.openservices.eas.predict.proto.QueueServiceProtos.DataFrameProto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import shade.protobuf.InvalidProtocolBufferException;

public class DataFrameList {
  private DataFrameListProto.Builder df_list_prot = DataFrameListProto.newBuilder();
  private static Log log = LogFactory.getLog(DataFrameList.class);
  private DataFrame[] data_frame_list = null;
  private String type;

  public DataFrameList() {
    this.type = "protobuffer";
  }

  public DataFrameList(DataFrame[] df_list) {
    this.df_list_prot.clear();
    this.type = "protobuffer";
    for (DataFrame df : df_list) {
      this.df_list_prot.addIndex(df.getDataFrameProto());
    }
    this.data_frame_list = new DataFrame[df_list.length];
    for (int i = 0; i < df_list.length; ++i) {
      this.data_frame_list[i] = df_list[i];
    }
  }

  public void setType(String type) {
    this.type = type;
  }

  public DataFrameListProto getDataFrameListProto() {
    return this.df_list_prot.build();
  }

  public byte[] encode() {
    return this.df_list_prot.build().toByteArray();
  }

  public DataFrameList decode(byte[] serialized_df_list_proto) {
    try {
      if (serialized_df_list_proto == null) {}

      DataFrameListProto df_list = DataFrameListProto.parseFrom(serialized_df_list_proto);
      this.df_list_prot.clear();
      this.df_list_prot.mergeFrom(df_list);
      this.data_frame_list = new DataFrame[df_list.getIndexCount()];
      int i = 0;
      for (DataFrameProto df : df_list.getIndexList()) {
        this.data_frame_list[i] = new DataFrame(df);
        ++i;
      }
    } catch (InvalidProtocolBufferException e) {
      log.error("Invalid Protocol Buffer", e);
    }
    return this;
  }

  public DataFrame[] getList() {
    if (data_frame_list == null) {
      log.warn("DataFrame Array is empty");
      this.data_frame_list = new DataFrame[0];
    }
    return this.data_frame_list;
  }
}
