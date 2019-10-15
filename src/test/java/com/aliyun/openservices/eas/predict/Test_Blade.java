package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.BladeDataType;
import com.aliyun.openservices.eas.predict.request.BladeRequest;
import com.aliyun.openservices.eas.predict.response.BladeResponse;

import java.util.List;

public class Test_Blade {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig(10, 100, 10, 10, 10, 1000));
    }

    public static BladeRequest buildBladeRequest() {
        BladeRequest request = new BladeRequest();
        request.setSignatureName("predict");
        int batchsize = 1;

        float[] content = new float[batchsize*224*224*3];
        for (int i = 0; i < content.length; ++i) {
            content[i] = 0.0f;
        }
        long[] shape = new long[] {batchsize, 224, 224, 3};
        //request.addFeed(content_0, shape_0,"inputs");
        request.addFeed("inputs", BladeDataType.DT_FLOAT, shape, content);
        request.addFetch("outputs");
        return request;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setToken("tokenGeneratedFromService");
        client.setEndpoint("eas-shanghai.alibaba-inc.com");
        client.setModelName("test_blade_java_client");
        client.setIsCompressed(false);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            BladeResponse response = null;
            try {
                response = client.predict(buildBladeRequest());
                List<Float> result = response.getFloatValsByBladeBlobName("outputs");
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
