package com.aliyun.openservices.eas.predict;
import java.util.Map;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.request.JsonFeatureValue;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.response.JsonResponse;

public class Test_PAI {
	public static void main(String[] args) throws Exception {
		//启动并初始化客户端
		PredictClient client = new PredictClient(new HttpConfig());
		 /*
         * 如果Internet Endpoint为http://eas-shanghai.alibaba-inc.com/api/predict/credit
         * 则client.setEndpoint("eas-shanghai.alibaba-inc.com");
         * client.setModelName("credit")
         * client.setToken需要根据服务具体生成的token设置
         */
		client.setToken("MmYwMjMwZTc3MzI1NzU4ZDM2YTkxOGU2OWFjMGQ3YWU1MTBhNDE2OA==");
		client.setEndpoint("eas-shanghai.alibaba-inc.com");
		client.setModelName("pai_demo_xp");

		String[] inputKey = { "featrue_1", "featrue_2", "featrue_3" };	//输入表的列名
		double[] inputValue = {10.7, 1, 2.5};	//输入表列对应的值
		
		//将每列对应输入request
		JsonRequest request = new JsonRequest();
		Map<String, JsonFeatureValue> row = request.addRow();
		for (int i = 0; i < inputKey.length; i++) {
			JsonFeatureValue param = new JsonFeatureValue(inputValue[i]);
			row.put(inputKey[i], param);
		}

		//输入为JsonRequest类型
		System.out.println(request.getJSON());
		
		//返回为JsonResponse类型
		JsonResponse response = client.predict(request);
		System.out.println(response.getOutputs().get(0).getOutputValue());

		//关闭客户端
		client.shutdown();
		return;
	}
} 
