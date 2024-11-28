package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.Compressor;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos;
import com.aliyun.openservices.eas.predict.proto.TorchRecPredictProtos;
import com.aliyun.openservices.eas.predict.request.TorchDataType;
import com.aliyun.openservices.eas.predict.request.TorchRequest;
import com.aliyun.openservices.eas.predict.response.TorchResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Test_Torch_Map_Inputs {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static TorchRequest buildPredictRequest() {
        TorchRequest request = new TorchRequest();
        float[] content = new float[6656];
        for (int i = 0; i < content.length; i++) {
            content[i] = (float) 1.0;
        }
        int[] content_i = new int[13312];
        for (int i = 0; i < content_i.length; i++) {
            content_i[i] = 1;
        }

        long[] content_l = new long[13312];
        for (int i = 0; i < content_l.length; i++) {
            content_l[i] = 1;
        }


        request.addFeedMap("float_features", TorchDataType.DT_FLOAT, new long[]{512,13}, content);
        request.addFeedMap("id_list_features.lengths", TorchDataType.DT_INT32, new long[]{512,26}, content_i);
        request.addFeedMap("id_list_features.values", TorchDataType.DT_INT64, new long[]{13312}, content_l);


        return request;
    }

    public static void main(String[] args) throws Exception {
        PredictClient client = InitClient();
        client.setToken("tokenGeneratedFromService");
        client.setEndpoint("cn-beijing.pai-eas.aliyuncs.com");
        client.setModelName("test_dlrm");
        client.setIsCompressed(false);
        client.setCompressor(Compressor.Snappy);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            TorchResponse response = null;
            try {
                response = client.predict(buildPredictRequest());

                for (Map.Entry<String, TorchPredictProtos.ArrayProto> entry : response.getMapOutputs().entrySet()) {

                    System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Spend Time: " + (endTime - startTime) + "ms");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");
        client.shutdown();
    }
}