package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.JsonFeatureValue;
import com.aliyun.openservices.eas.predict.request.JsonRequest;

import java.util.Map;

public class Test_Merge {
	public static void main(String[] args) throws Exception {		
		// 启动并初始化客户端
		PredictClient client = new PredictClient(new HttpConfig());
 
		// model1 配置 JSON
		String model1_token = "MmYwMjMwZTc3MzI1NzU4ZDM2YTkxOGU2OWFjMGQ3YWU1MTBhNDE2OA==";
		String model1_endpoint = "eas-shanghai.alibaba-inc.com";
		String model1_name = "pai_demo_xp";

		String[] inputKey = { "featrue_1", "featrue_2", "featrue_3" }; // 输入表的列名
		double[] inputValue = { 10.7, 1, 2.5 }; // 输入表列对应的值

		JsonRequest model1_request = new JsonRequest();
		Map<String, JsonFeatureValue> row = model1_request.addRow();
		for (int i = 0; i < inputKey.length; i++) {
			JsonFeatureValue param = new JsonFeatureValue(inputValue[i]);
			row.put(inputKey[i], param);
		}

		// model2 配置 String
		String model2_token = "NGNmNjQyZGJiYjQwNDliOTE1NTQ0ZGM1M2FjYjdjZWRlOGE2ZjRhZg==";
		String model2_endpoint = "eas-shanghai.alibaba-inc.com";
		String model2_name = "credit";

		String model2_request = "[{\"money_credit\": 3000000}, {\"money_credit\": 10000}]";

		// 预测
		System.out.println("model1的输入：" + model1_request.getJSON());
		System.out.println("model2的输入：" + model2_request);

		/*
		JsonResponse model1_response = client
								.createChlidClient(model1_token, model1_endpoint, model1_name)
								.predict(model1_request);

		String model2_response = client
								.createChlidClient(model2_token, model2_endpoint, model2_name)
								.predict(model2_request);
		
		System.out.println("model1的输出："
				+ model1_response.getOutputs().get(0).getOutputValue());
		System.out.println("model2的输出：" + model2_response);
		*/

		// 关闭客户端
		client.shutdown();
		return;
	}
}
