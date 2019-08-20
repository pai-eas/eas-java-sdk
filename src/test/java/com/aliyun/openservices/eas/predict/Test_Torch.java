package com.aliyun.openservices.eas.predict;

import java.util.List;

import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.request.TorchDataType;
import com.aliyun.openservices.eas.predict.request.TorchRequest;
import com.aliyun.openservices.eas.predict.response.TorchResponse;

public class Test_Torch {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static TorchRequest buildPredictRequest() {
        TorchRequest request = new TorchRequest();
        float[] content = new float[150528];
        for (int i = 0; i < content.length; i++) {
            content[i] = (float) 0.5;
        }
        request.addFeed(0, TorchDataType.DT_FLOAT, new long[]{1, 3, 224, 224}, content);
//        request.addFeed(1, TorchDataType.DT_FLOAT, new long[]{1, 3, 224, 224}, content);
//        request.addFetch(0);
        return request;
    }

    public static void main(String[] args) throws Exception {
        PredictClient client = InitClient();
//        client.setToken("NGFmYzA1YTRlZmE0NDkwYjBmMGI1NjgxOGNmMzk4ODMyOGZhNjdkMg==");
        client.setEndpoint("eas-beijing.alibaba-inc.com");
        client.setModelName("testpytorch_sdk");
        client.setIsCompressed(false);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            TorchResponse response = null;
            try {
                response = client.predict(buildPredictRequest());
                List<Float> result = response.getFloatVals(0);
//                System.out.print(response.getTensorShape(0));
                System.out.print("Predict Result: [");
                for (int j = 0; j < result.size(); j++) {
                    System.out.print(result.get(j).floatValue());
                    if (j != result.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.print("]\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");
        client.shutdown();
    }
}