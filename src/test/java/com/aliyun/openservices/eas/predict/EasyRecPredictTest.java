package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.proto.EasyRecPredictProtos;
import com.aliyun.openservices.eas.predict.request.EasyRecRequest;

import java.util.Map;

/**
 * Created by bruceding.jing on 2022/08/02.
 */
public class EasyRecPredictTest {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static EasyRecRequest buildPredictRequest() {
        EasyRecRequest easyrecRequest = new EasyRecRequest();
        easyrecRequest.addUserFeature("clk_gid_list", "60165");
        easyrecRequest.appendItemId("60165");
        return easyrecRequest;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setEndpoint("1466725745529307.ap-southeast-1.pai-eas.aliyuncs.com");
        client.setModelName("tmp_ctr_deep_v1a");
        client.setToken("N2U1MTU4N2E1NDZjYWJkODljZGM3ZTZmNmZkYWEyZGRkZjZhNzJhNw==");
        client.setRequestTimeout(100000);

        testInvoke(client);
        testDebugLevel(client);
        client.shutdown();
    }

    public static void testInvoke(PredictClient client) throws Exception {
        long startTime = System.currentTimeMillis();
        EasyRecPredictProtos.PBResponse response = client.predict(buildPredictRequest());
        for (Map.Entry<String, EasyRecPredictProtos.Results> entry : response.getResultsMap().entrySet()) {
            String key = entry.getKey();
            EasyRecPredictProtos.Results value = entry.getValue();
            System.out.print("key: " + key);
            for (int i = 0; i < value.getScoresCount(); i++) {
                System.out.format(" value: %.4f ", value.getScores(i));
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");

    }

    public static void testDebugLevel(PredictClient client) throws Exception {
        long startTime = System.currentTimeMillis();
        EasyRecRequest request = buildPredictRequest();
        request.setDebugLevel(1);
        EasyRecPredictProtos.PBResponse response = client.predict(request);
        Map<String, String> genFeas = response.getGenerateFeaturesMap();
        for(String itemId: genFeas.keySet()) {
            System.out.println(itemId);
            System.out.println(genFeas.get(itemId));
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");

    }
}
