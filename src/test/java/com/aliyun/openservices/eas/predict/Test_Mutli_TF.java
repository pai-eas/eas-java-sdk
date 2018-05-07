package com.aliyun.openservices.eas.predict;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.request.TFDataType;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.TFResponse;

class ThreadClient2 implements Runnable {
    private String name;
    PredictClient client;

    public ThreadClient2(String name, PredictClient client) {
        this.name = name;
        this.client = client;
    }

    public static TFRequest buildPredictRequest() {
        TFRequest request = new TFRequest();
        request.setSignatureName("predict_images");
        float[] content = new float[784];
        for (int i = 0; i < content.length; i++)
            content[i] = (float)(Math.random() * 2);
        request.addFeed("images", TFDataType.DT_FLOAT, new long[]{1, 784}, content);
        request.addFetch("scores");
        return request;
    }

    public void run() {
        while (true) {
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
    }
}

public class Test_Mutli_TF {
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
        client.setToken("Yzg4YWQ3ODRkMmY5NTA5MTJlZjlmOTVhYzNhMGEzZjI5MjQyZjI2MQ==");
        client.setEndpoint("eas-pre.alibaba-inc.com");
        client.setModelName("tf_serving");
        client.setIsCompressed(false);
        long startTime = System.currentTimeMillis();

        ArrayList<Thread> thread_list = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new ThreadClient2("Thread_" + i, client));
            t.start();
            thread_list.add(t);
        }
        for (int j = 0; j < thread_list.size(); j++)
            thread_list.get(j).join();


        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");

        client.shutdown();
    }
}