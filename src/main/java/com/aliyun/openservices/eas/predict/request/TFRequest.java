package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.PredictProtos.ArrayShape;
import com.aliyun.openservices.eas.predict.proto.PredictProtos.ArrayDataType;
import com.aliyun.openservices.eas.predict.proto.PredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.PredictProtos.PredictRequest;
import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by yaozheng.wyz on 2017/10/24.
 */
public class TFRequest {
    private PredictRequest.Builder request = PredictRequest.newBuilder();
    private static Log log = LogFactory.getLog(TFRequest.class);
    public void setSignatureName(String value) {
        request.setSignatureName(value);
    }
    public void addFetch(String value) { request.addOutputFilter(value); }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, float[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_FLOAT) {
            requestProto.setDtype(ArrayDataType.DT_FLOAT);
        } else if (dataType == TFDataType.DT_COMPLEX64) {
            requestProto.setDtype(ArrayDataType.DT_COMPLEX64);
        } else if (dataType == TFDataType.DT_BFLOAT16) {
            requestProto.setDtype(ArrayDataType.DT_BFLOAT16);
        } else if (dataType == TFDataType.DT_HALF) {
            requestProto.setDtype(ArrayDataType.DT_HALF);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addFloatVal(content[i]);
        request.putInputs(inputname, requestProto.build());
    }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, double[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_DOUBLE) {
            requestProto.setDtype(ArrayDataType.DT_DOUBLE);
        } else if (dataType == TFDataType.DT_COMPLEX128) {
            requestProto.setDtype(ArrayDataType.DT_COMPLEX128);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addDoubleVal(content[i]);
        request.putInputs(inputname, requestProto.build());
    }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, int[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_INT32) {
            requestProto.setDtype(ArrayDataType.DT_INT32);
        } else if (dataType == TFDataType.DT_UINT8) {
            requestProto.setDtype(ArrayDataType.DT_UINT8);
        } else if (dataType == TFDataType.DT_INT16) {
            requestProto.setDtype(ArrayDataType.DT_INT16);
        } else if (dataType == TFDataType.DT_INT8) {
            requestProto.setDtype(ArrayDataType.DT_INT8);
        } else if (dataType == TFDataType.DT_QINT8) {
            requestProto.setDtype(ArrayDataType.DT_QINT8);
        } else if (dataType == TFDataType.DT_QUINT8) {
            requestProto.setDtype(ArrayDataType.DT_QUINT8);
        } else if (dataType == TFDataType.DT_QINT32) {
            requestProto.setDtype(ArrayDataType.DT_QINT32);
        } else if (dataType == TFDataType.DT_QINT16) {
            requestProto.setDtype(ArrayDataType.DT_QINT16);
        } else if (dataType == TFDataType.DT_QUINT16) {
            requestProto.setDtype(ArrayDataType.DT_QUINT16);
        } else if (dataType == TFDataType.DT_UINT16) {
            requestProto.setDtype(ArrayDataType.DT_UINT16);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++) {
            arrayShape.addDim(shape[i]);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addIntVal(content[i]);
        request.putInputs(inputname, requestProto.build());
    }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, String[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_STRING) {
            requestProto.setDtype(ArrayDataType.DT_STRING);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addStringVal(ByteString.copyFromUtf8(content[i]));
        request.putInputs(inputname, requestProto.build());
    }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, long[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_INT64) {
            requestProto.setDtype(ArrayDataType.DT_INT64);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addInt64Val(content[i]);
        request.putInputs(inputname, requestProto.build());
    }

    public void addFeed(String inputname, TFDataType dataType, long[] shape, boolean[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TFDataType.DT_BOOL) {
            requestProto.setDtype(ArrayDataType.DT_BOOL);
        } else {
            log.error("call addFeed Error: TFDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TFDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeArrayShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addBoolVal(content[i]);
        request.putInputs(inputname, requestProto.build());
    }

    public PredictRequest getRequest() {
        return request.build();
    }
}
