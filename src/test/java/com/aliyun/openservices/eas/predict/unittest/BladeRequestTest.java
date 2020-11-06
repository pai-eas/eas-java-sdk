package com.aliyun.openservices.eas.predict.unittest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;
import shade.blade.protobuf.ByteString;

import com.aliyun.openservices.eas.predict.proto.BladeProtos.Array;
import com.aliyun.openservices.eas.predict.proto.BladeProtos.Request;
import com.aliyun.openservices.eas.predict.request.BladeRequest;
import com.aliyun.openservices.eas.predict.request.BladeDataType;

public class BladeRequestTest {
  public static BladeRequest buildBladeRequest() {
    BladeRequest request = new BladeRequest();
    request.setSignatureName("predict");
    String nameInDlModel = "input";
    String content = "Hello World";
    long ctentLen = content.length();
    long [] shape = null;
    // The 1st feed
    shape = new long[] {1, ctentLen};
    byte [] ctentBytes = content.getBytes(StandardCharsets.UTF_8);
    request.addFeed(ctentBytes, shape, nameInDlModel + "_1st");
    // The 2nd feed
    String ctentBatch4 = content + content + content + content;
    shape = new long[] {4, ctentLen};
    ctentBytes = ctentBatch4.getBytes(StandardCharsets.UTF_8);
    request.addFeed(ctentBytes,
                    4, shape, "blobName", nameInDlModel + "_2nd");
    // The 3rd feed
    request.addFeed(nameInDlModel + "_3rd", BladeDataType.DT_STRING, shape, ctentBytes);
    return request;
  }

  @Test
  public void testStringVal() {
    Request request = buildBladeRequest().getRequest();
    Array input1st = request.getInputs(0);
    Array input2nd = request.getInputs(1);
    Array input3rd = request.getInputs(2);
    List<shade.blade.protobuf.ByteString> bytes1st = input1st.getStringValList();
    List<shade.blade.protobuf.ByteString> bytes2nd = input2nd.getStringValList();
    List<shade.blade.protobuf.ByteString> bytes3rd = input3rd.getStringValList();
    assertEquals("failure - lengths not correct", 11, bytes1st.size());
    assertEquals("failure - lengths not correct", 44, bytes2nd.size());
    assertEquals("failure - lengths not correct", 44, bytes3rd.size());
    String content = "Hello World";
    assertEquals("failure - values not equal", content, new String(bytes1st.get(0).toByteArray()));
    String ctentBatch4 = content + content + content + content;
    assertEquals("failure - values not equal", ctentBatch4, new String(bytes2nd.get(0).toByteArray()));
    assertEquals("failure - values not equal", ctentBatch4, new String(bytes3rd.get(0).toByteArray()));

    assertEquals("failure - batchsize mis-match", input1st.getBatchsize(), 1);
    assertEquals("failure - batchsize mis-match", input2nd.getBatchsize(), 4);
    assertEquals("failure - batchsize mis-match", input3rd.getBatchsize(), 4);

    assertFalse("failure - has blob name", input1st.hasBladeBlobName());
    assertEquals("failure - blob name mis-match", "blobName", input2nd.getBladeBlobName());
    assertFalse("failure - has blob name", input3rd.hasBladeBlobName());

    assertEquals("failure - input name mis-match", "input_1st", input1st.getNameInDlModel());
    assertEquals("failure - input name mis-match", "input_2nd", input2nd.getNameInDlModel());
    assertEquals("failure - input name mis-match", "input_3rd", input3rd.getNameInDlModel());

    Long len = (long)content.length();
    Long [] expectShape = new Long[]{1L, len};
    List<Long> dimList = input1st.getShape().getDimList();
    Long [] actualShape = new Long[dimList.size()];
    actualShape = dimList.toArray(actualShape);
    assertArrayEquals("failure - shape mis-match", expectShape, actualShape);
 
    expectShape = new Long[]{4L, len};
    dimList = input2nd.getShape().getDimList();
    actualShape = new Long[dimList.size()];
    actualShape = dimList.toArray(actualShape);
    assertArrayEquals("failure - shape mis-match", expectShape, actualShape);

    expectShape = new Long[]{4L, len};
    dimList = input3rd.getShape().getDimList();
    actualShape = new Long[dimList.size()];
    actualShape = dimList.toArray(actualShape);
    assertArrayEquals("failure - shape mis-match", expectShape, actualShape);
  }
}
