package com.aliyun.openservices.eas.predict;
import java.util.List;

import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.request.TFDataType;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.TFResponse;

public class Test_TF {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static TFRequest buildPredictRequest() {
        TFRequest request = new TFRequest();
        request.setSignatureName("predict_images");
        float[] content = new float[784];
        for (int i = 0; i < content.length; i++)
            content[i] = (float)0.0;
        request.addFeed("images", TFDataType.DT_FLOAT, new long[]{1, 784}, content);
        request.addFetch("scores");
        return request;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setToken("NjZkOXFlNGIxYjE1Y2U5MzllMWJiYjEzYmUwYWYzY2M0YjIwYjRlNg==");
        client.setEndpoint("eas-shanghai.alibaba-inc.com");
        client.setModelName("tf_serving_test");
        client.setIsCompressed(false);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            TFResponse response = client.predict(buildPredictRequest());
            List<Float> result = response.getFloatVals("scores");
            System.out.print("Predict Result: [");
            for (int j = 0; j < result.size(); j++) {
                System.out.print(result.get(j).floatValue());
                if (j != result.size() -1)
                    System.out.print(", ");
            }
            System.out.print("]\n");
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");
        client.shutdown();
    }
}