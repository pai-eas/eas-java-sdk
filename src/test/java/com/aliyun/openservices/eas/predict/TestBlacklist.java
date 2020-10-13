package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.TFDataType;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.TFResponse;

import java.util.List;

/**
 * Created by yaozheng.wyz on 2020/9/27.
 */
public class TestBlacklist {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig(10, 10, 10, 10, 10, 10));
    }

    public static TFRequest buildPredictRequest() {
        TFRequest request = new TFRequest();
        request.setSignatureName("predict_ctr");
        String[] content = new String[1];
        content[0] = "1002;0;d1a51189;08facbad;50e219e0;ecad2386;7801e8d9;07d7df22;ab9d1c61;0;fbc6a77e;0e88538f;0;16920;320;50;1899;0;431;100075;117";
        request.addFeed("request", TFDataType.DT_STRING, new long[]{1}, content);
        request.addFetch("scores");
        return request;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setToken("YjFkYmJmOTc4NTIxYjdmMzMwM2RmZmNlYjdiZTdiZmE4NTdmZWE3ZQ==");
        client.setVIPServer("ctr-model.zhangbei.eas.vipserver");
        client.setModelName("ctr_model");
        client.startBlacklistMechanism(5, 5, 5);
        client.setIsCompressed(false);

        while (true) {
            long startTime = System.currentTimeMillis();
            TFResponse response = null;
            try {
                response = client.predict(buildPredictRequest());
                List<Float> result = response.getFloatVals("scores");
                System.out.print("Predict Result: [");
                for (int j = 0; j < result.size(); j++) {
                    System.out.print(result.get(j).floatValue());
                    if (j != result.size() - 1)
                        System.out.print(", ");
                }
                System.out.print("]\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Spend Time: " + (endTime - startTime) + "ms");
        }

        //client.shutdown();
    }
}
