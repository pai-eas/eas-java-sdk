package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.CaffeRequest;
import com.aliyun.openservices.eas.predict.response.CaffeResponse;

import java.util.List;

/**
 * Created by yaozheng.wyz on 2017/11/27.
 */
public class CaffePredictTest {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static CaffeRequest buildPredictRequest() {
        CaffeRequest request = new CaffeRequest();
        float[] content = new float[3 * 227 * 227];
        for (int i = 0; i < content.length; i++)
            content[i] = (float)1.0;
        request.addFeed("data", new long[]{1, 3, 227, 227}, content);
        request.addFetch("prob");
        return request;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setToken("ZGI0MmQ3NjM1OTAwODMwZGIxMDkwMGQ4ZGE5NmM4NTVkM2E1Y2ZmNw==");
        client.setEndpoint("eas-shanghai.alibaba-inc.com");
        client.setModelName("caffe_serving");
        client.setIsCompressed(false);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CaffeResponse response = client.predict(buildPredictRequest());
            List<Float> result = response.getVals("prob");
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
