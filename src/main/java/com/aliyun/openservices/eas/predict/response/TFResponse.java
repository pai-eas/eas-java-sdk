package com.aliyun.openservices.eas.predict.response;

import com.aliyun.openservices.eas.predict.proto.PredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.PredictProtos.PredictResponse;
import shade.protobuf
.ByteString;
import shade.protobuf
.InvalidProtocolBufferException;
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
    private String prefix = "";

    public void setContentValues(byte[] content) {
        try {
            response = PredictResponse.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setPrefix(String value) {
        this.prefix = value;
    }

    private String getOutputName(String name) {
        String output;
        if (prefix.isEmpty()) {
            output = name;
        } else if (prefix.endsWith("/")) {
            output = prefix + name;
        } else {
            output = prefix + "/" + name;
        }
        return output;
    }

    public List<Long> getTensorShape(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name: " + output);
                throw new RuntimeException("Not Found output name: " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getArrayShape().getDimList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Float> getFloatVals(String outputName) {
        String output = getOutputName(outputName);
        System.out.println("======" + output);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getFloatValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
    }

    public List<Double> getDoubleVals(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getDoubleValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Double>();
        }
    }

    public List<Integer> getIntVals(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getIntValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Integer>();
        }
    }

    public List<String> getStringVals(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
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

    public List<Long> getInt64Vals(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getInt64ValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Boolean> getBoolVals(String outputName) {
        String output = getOutputName(outputName);
        if (response != null) {
            if (!response.containsOutputs(output)) {
                log.error("Not Found output name : " + output);
                throw new RuntimeException("Not Found output name : " + output);
            }
            ArrayProto responseProto = response.getOutputsMap().get(output);
            return responseProto.getBoolValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Boolean>();
        }
    }
}
