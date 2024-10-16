package com.aliyun.openservices.eas.predict.response;

import com.aliyun.openservices.eas.predict.proto.QueueServiceProtos;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.ArrayProto;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.PredictResponse;
import shade.protobuf
        .ByteString;
import shade.protobuf
        .InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TorchResponse {
    private static Log log = LogFactory.getLog(TorchResponse.class);
    private PredictResponse response = null;

    public void setContentValues(byte[] content) {
        try {
            response = PredictResponse.parseFrom(content);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public List<Long> getTensorShape(int index) {
        if (response != null) {
            if (response.getOutputsCount() <= index) {
                log.error("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
                throw new RuntimeException("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
            }
            ArrayProto responseProto = response.getOutputs(index);
            return responseProto.getArrayShape().getDimList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Float> getFloatVals(int index) {
        if (response != null) {
            if (response.getOutputsCount() <= index) {
                log.error("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
                throw new RuntimeException("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
            }
            ArrayProto responseProto = response.getOutputs(index);
            return responseProto.getFloatValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
    }

    public List<Double> getDoubleVals(int index) {
        if (response != null) {
            if (response.getOutputsCount() <= index) {
                log.error("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
                throw new RuntimeException("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
            }
            ArrayProto responseProto = response.getOutputs(index);
            return responseProto.getDoubleValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Double>();
        }
    }

    public List<Integer> getIntVals(int index) {
        if (response != null) {
            if (response.getOutputsCount() <= index) {
                log.error("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
                throw new RuntimeException("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
            }
            ArrayProto responseProto = response.getOutputs(index);
            return responseProto.getIntValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Integer>();
        }
    }


    public List<Long> getInt64Vals(int index) {
        if (response != null) {
            if (response.getOutputsCount() <= index) {
                log.error("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
                throw new RuntimeException("Output_filter should not have more tensors than model's outputs: " + response.getOutputsCount());
            }
            ArrayProto responseProto = response.getOutputs(index);
            return responseProto.getInt64ValList();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public Map<String, ArrayProto> getMapOutputs(){
        return response.getMapOutputsMap();
    }
}
