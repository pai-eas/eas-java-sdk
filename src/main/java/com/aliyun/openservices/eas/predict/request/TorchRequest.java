package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.ArrayShape;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.ArrayDataType;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.PredictRequest;
import shade.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;


public class TorchRequest {
    private PredictRequest.Builder request = PredictRequest.newBuilder();

    private static Log log = LogFactory.getLog(TFRequest.class);


    public void setDebugLevel(int level) {
        request.setDebugLevel(level);
    }

    public void addFetch(int value) {
        request.addOutputFilter(value);
    }

    public void addFeed(int index, TorchDataType dataType, long[] shape, float[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_FLOAT) {
            requestProto.setDtype(ArrayDataType.DT_FLOAT);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (float v : content) {
            requestProto.addFloatVal(v);
        }
        request.addInputs(index, requestProto.build());
    }

    public void addFeed(int index, TorchDataType dataType, long[] shape, double[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_DOUBLE) {
            requestProto.setDtype(ArrayDataType.DT_DOUBLE);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (double v : content) {
            requestProto.addDoubleVal(v);
        }
        request.addInputs(index, requestProto.build());
    }

    public void addFeed(int index, TorchDataType dataType, long[] shape, int[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_INT32) {
            requestProto.setDtype(ArrayDataType.DT_INT32);
        } else if (dataType == TorchDataType.DT_UINT8) {
            requestProto.setDtype(ArrayDataType.DT_UINT8);
        } else if (dataType == TorchDataType.DT_INT16) {
            requestProto.setDtype(ArrayDataType.DT_INT16);
        } else if (dataType == TorchDataType.DT_INT8) {
            requestProto.setDtype(ArrayDataType.DT_INT8);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (int value : content) {
            requestProto.addIntVal(value);
        }
        request.addInputs(index, requestProto.build());
    }


    public void addFeed(int index, TorchDataType dataType, long[] shape, long[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_INT64) {
            requestProto.setDtype(ArrayDataType.DT_INT64);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long value : shape) {
            arrayShape.addDim(value);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (long l : content) {
            requestProto.addInt64Val(l);
        }
        request.addInputs(index, requestProto.build());
    }

    public void addFeedMap(String index, TorchDataType dataType, long[] shape, long[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_INT64) {
            requestProto.setDtype(ArrayDataType.DT_INT64);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long value : shape) {
            arrayShape.addDim(value);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (long l : content) {
            requestProto.addInt64Val(l);
        }
        request.putMapInputs(index, requestProto.build());
    }

    public void addFeedMap(String index, TorchDataType dataType, long[] shape, float[] content) {
        System.out.print("add feed");
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_FLOAT) {
            requestProto.setDtype(ArrayDataType.DT_FLOAT);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (float v : content) {
            requestProto.addFloatVal(v);
        }
        request.putMapInputs(index, requestProto.build());
    }

    public void addFeedMap(String index, TorchDataType dataType, long[] shape, double[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_DOUBLE) {
            requestProto.setDtype(ArrayDataType.DT_DOUBLE);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch!");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch!");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (double v : content) {
            requestProto.addDoubleVal(v);
        }
        request.putMapInputs(index, requestProto.build());
    }

    public void addFeedMap(String index, TorchDataType dataType, long[] shape, int[] content) {
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        if (dataType == TorchDataType.DT_INT32) {
            requestProto.setDtype(ArrayDataType.DT_INT32);
        } else if (dataType == TorchDataType.DT_UINT8) {
            requestProto.setDtype(ArrayDataType.DT_UINT8);
        } else if (dataType == TorchDataType.DT_INT16) {
            requestProto.setDtype(ArrayDataType.DT_INT16);
        } else if (dataType == TorchDataType.DT_INT8) {
            requestProto.setDtype(ArrayDataType.DT_INT8);
        } else {
            log.error("call addFeed Error: TorchDataType and content mismatch");
            throw new RuntimeException("call addFeed Error: TorchDataType and content mismatch");
        }
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (long l : shape) {
            arrayShape.addDim(l);
        }
        requestProto.mergeArrayShape(arrayShape.build());
        for (int value : content) {
            requestProto.addIntVal(value);
        }
        request.putMapInputs(index, requestProto.build());
    }


    public PredictRequest getRequest() {
        return request.build();
    }
}
