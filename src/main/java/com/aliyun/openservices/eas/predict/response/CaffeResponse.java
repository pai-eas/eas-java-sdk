package com.aliyun.openservices.eas.predict.response;

import com.aliyun.openservices.eas.predict.proto.CaffePredictProtos.PredictResponse;
import com.aliyun.openservices.eas.predict.proto.CaffePredictProtos.ArrayProto;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaozheng.wyz on 2017/11/27.
 */
public class CaffeResponse {
    private static Log log = LogFactory.getLog(CaffeResponse.class);
    private PredictResponse response = null;

    public void setContentValues(byte[] content) {
        try {
            response = PredictResponse.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public List<Long> getBlobShape(String outputname) {
        if (response != null) {
            int i = 0;
            for (; i < response.getOutputNameCount(); i++) {
                if (outputname.equals(response.getOutputName(i))) {
                    ArrayProto responseProto = response.getOutputData(i);
                    return responseProto.getShape().getDimList();
                }
            }
            if (i == response.getOutputNameCount()) {
                log.error("Not Found output name: " + outputname);
                throw new RuntimeException("Not Found output name: " + outputname);
            }
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
        return new ArrayList<Long>();
    }
    public List<Float> getVals(String outputname) {
        if (response != null) {
            int i = 0;
            for (; i < response.getOutputNameCount(); i++) {
                if (outputname.equals(response.getOutputName(i))) {
                    ArrayProto responseProto = response.getOutputData(i);
                    return responseProto.getDataList();
                }
            }
            if (i == response.getOutputNameCount()) {
                log.error("Not Found output name: " + outputname);
                throw new RuntimeException("Not Found output name: " + outputname);
            }
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
        return new ArrayList<Float>();
    }
}
