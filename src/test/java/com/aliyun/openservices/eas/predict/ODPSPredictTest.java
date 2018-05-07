package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.odps.predict.PredictClient;
import com.aliyun.openservices.odps.predict.PredictConfig;
import com.aliyun.openservices.odps.predict.codec.JsonFeatureValue;
import com.aliyun.openservices.odps.predict.codec.KVJsonRequest;
import com.aliyun.openservices.odps.predict.codec.KVJsonResponse;
import com.aliyun.openservices.odps.predict.request.JsonPredictRequest;

import java.util.Map;

public class ODPSPredictTest {
	public static void main(String[] args) {
		PredictConfig config = new PredictConfig(
		// 服务endpoint，这里引入两个模板变量${region}和${model}，当用同一个
		// 客户端访问多个服务的时候，这两个模板变量可以帮助客户端访问不同的endpoint。
		// 这里的模板设计是为了无缝兼容老版本PAI在线预测服务而引入的。
				"http://eas-shanghai.alibaba-inc.com/api/predict/pai_demo_xp",
//				"http://127.0.0.1:8081",
				new com.aliyun.openservices.odps.predict.auth.Account(
						// 配置EAS的认证，EAS用于指定认证方式，
						// 3da86f351316b83cssfa27ad21edad80b 用来指定认证的touken
						"EAS",
						"MmYwMjMwZTc3MzI1NzU4ZDM2YTkxOGU2OWFjMGQ3YWU1MTBhNDE2OA=="));
		PredictClient client = new PredictClient(config);

		String modelName = "pai_demo_xp";
		String project = "shanghai";
		
		String[] inputKey = {"featrue_1", "featrue_2", "featrue_3"};
		double[] inputValue = {10.7, 1, 2.5};
		
		KVJsonRequest request = new KVJsonRequest();
		Map<String, JsonFeatureValue> row = request.addRow();
		
		for (int i = 0; i < inputKey.length; i++)
		{
			JsonFeatureValue param = new JsonFeatureValue(inputValue[i]);
			  System.out.println("inputKey = " + inputKey[i]
					  + ", inputValue = " + param.getDataValue());
			row.put(inputKey[i], param);
		}
		
		KVJsonResponse response = null;
		try {
			JsonPredictRequest req = new JsonPredictRequest(project,										
					modelName,
					request);
			System.out.println(req.getInput().toString());
			response = client.syncPredict(req);

			System.out.println(response.getOutputs().get(0).getOutputValue());
			
			
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		System.exit(0);
	}
}
