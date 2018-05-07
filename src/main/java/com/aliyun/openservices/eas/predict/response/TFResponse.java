package com.aliyun.openservices.eas.predict.response;

import com.aliyun.openservices.eas.predict.proto.PredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.PredictProtos.PredictResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaozheng.wyz on 2017/10/24.
 */
public class TFResponse {
    private static Log log = LogFactory.getLog(TFResponse.class);
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

    public List<Long> getTensorShape(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name: " + outputname);
                throw new RuntimeException("Not Found output name: " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getArrayShape().getDimList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Float> getFloatVals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getFloatValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
    }

    public List<Double> getDoubleVals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getDoubleValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Double>();
        }
    }

    public List<Integer> getIntVals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getIntValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Integer>();
        }
    }

    public List<String> getStringVals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            List<ByteString> res = responseProto.getStringValList();
            List<String> result = new ArrayList<String>();
            for (int i = 0; i < res.size(); i++) {
                result.add(res.get(i).toStringUtf8());
            }
            return result;
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<String>();
        }
    }

    public List<Long> getInt64Vals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getInt64ValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Boolean> getBoolVals(String outputname) {
        if (response != null) {
            if (!response.containsOutputs(outputname)) {
                log.error("Not Found output name : " + outputname);
                throw new RuntimeException("Not Found output name : " + outputname);
            }
            ArrayProto responseProto = response.getOutputsMap().get(outputname);
            return responseProto.getBoolValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Boolean>();
        }
    }
}
