package com.aliyun.openservices.eas.predict;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.JsonFeatureValue;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.request.TFDataType;
import com.aliyun.openservices.eas.predict.request.TFRequest;

import java.util.ArrayList;
import java.util.Map;

class ThreadClient implements Runnable {
	private int count;
    PredictClient client;
    String model1_token = "MmYwMjMwZTc3MzI1NzU4ZDM2YTkxOGU2OWFjMGQ3YWU1MTBhNDE2OA==";
	String model1_endpoint = "eas-shanghai.alibaba-inc.com";
	String model1_name = "pai_demo_xp";
	String model2_token = "NGNmNjQyZGJiYjQwNDliOTE1NTQ0ZGM1M2FjYjdjZWRlOGE2ZjRhZg==";
	String model2_endpoint = "eas-shanghai.alibaba-inc.com";
	String model2_name = "credit";
	String model3_token = "Yzg4YWQ3ODRkMmY5NTA5MTJlZjlmOTVhYzNhMGEzZjI5MjQyZjI2MQ==";
	String model3_endpoint = "eas-pre.alibaba-inc.com";
	String model3_name = "tf_serving";
    
    public ThreadClient(int count, PredictClient client) {
    		this.count = count;
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
    		// model1 配置 JSON
		String[] inputKey = { "featrue_1", "featrue_2", "featrue_3" }; // 输入表的列名
		double[] inputValue = { Math.random() * 100, Math.random() * 100, Math.random() * 100 }; // 输入表列对应的值

		JsonRequest model1_request = new JsonRequest();
		Map<String, JsonFeatureValue> row = model1_request.addRow();
		for (int i = 0; i < inputKey.length; i++) {
			JsonFeatureValue param = new JsonFeatureValue(inputValue[i]);
			row.put(inputKey[i], param);
		}

		// model2 配置 String
		String model2_request = "[{\"money_credit\": " + (int)(Math.random() * 1000000) + "}]";

		// model3 配置 TF
		TFRequest model3_request = buildPredictRequest();
		
		// 预测
      /*
		JsonResponse model1_response;
		try {
			model1_response = client
								.createChlidClient(model1_token, model1_endpoint, model1_name)
								.predict(model1_request);
			String model2_response = client
					.createChlidClient(model2_token, model2_endpoint, model2_name)
					.predict(model2_request);

			TFResponse model3_response = client
					.createChlidClient(model3_token, model3_endpoint, model3_name)
					.predict(model3_request);
			System.out.println(count
					+ " model1:" + model1_response.getOutputs().get(0).getOutputValue()
					+ " model2:" + model2_response
					+ " model3:" + model3_response.getFloatVals("scores").get(0));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

*/

    		

    }
}

public class Test_Mutli {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();

        long startTime = System.currentTimeMillis();

        ArrayList<Thread> thread_list = new ArrayList<Thread>();
        for (int i = 0; i < 1; i++) {
            Thread t = new Thread(new ThreadClient(i + 1, client));
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