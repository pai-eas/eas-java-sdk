package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.CaffePredictProtos.ArrayShape;
import com.aliyun.openservices.eas.predict.proto.CaffePredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.CaffePredictProtos.PredictRequest;

/**
 * Created by yaozheng.wyz on 2017/11/27.
 */
public class CaffeRequest {
    private PredictRequest.Builder request = PredictRequest.newBuilder();
    public void addFetch(String value) { request.addOutputFilter(value); }

    public void addFeed(String inputname, long[] shape, float[] content) {
        request.addInputName(inputname);
        ArrayProto.Builder requestProto = ArrayProto.newBuilder();
        ArrayShape.Builder arrayShape = ArrayShape.newBuilder();
        for (int i = 0; i < shape.length; i++)
            arrayShape.addDim(shape[i]);
        requestProto.mergeShape(arrayShape.build());
        for (int i = 0; i < content.length; i++)
            requestProto.addData(content[i]);
        request.addInputData(requestProto.build());
    }

    public PredictRequest getRequest() {
        return request.build();
    }
}
