package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.request.JsonFeatureValue;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.response.JsonResponse;

import java.util.Map;

public class Test_PAI {
	public static JsonRequest buildOrderPredictJsonRequest() throws Exception {
		// build predict request
		JsonRequest request = new JsonRequest();
		try {
			Map<String, com.aliyun.openservices.eas.predict.request.JsonFeatureValue> row = request.addRow();
			row.put("pay_time_diff", new JsonFeatureValue(10L));
			row.put("consign_time_diff", new JsonFeatureValue(10L));
			row.put("sign_time_diff", new JsonFeatureValue(10L));
			row.put("case_create_time_diff", new JsonFeatureValue(10L));
			row.put("rank", new JsonFeatureValue(1L));
			row.put("create_amt", new JsonFeatureValue(0.0));
			row.put("is_case", new JsonFeatureValue("1"));
			row.put("is_succ_end", new JsonFeatureValue("1"));
			row.put("has_return", new JsonFeatureValue("0"));
			row.put("has_proof", new JsonFeatureValue("0"));
			row.put("has_refund", new JsonFeatureValue("1"));
			row.put("rights_type_id", new JsonFeatureValue("-1"));
			row.put("problem_desc_id", new JsonFeatureValue("?"));
			row.put("reason_id", new JsonFeatureValue("-1"));
			row.put("pay_status", new JsonFeatureValue(0L));
			row.put("logistics_status", new JsonFeatureValue(2L));
			row.put("refund_status", new JsonFeatureValue(1L));
			row.put("ls_time_diff", new JsonFeatureValue(0L));
			row.put("if_remind_shipment", new JsonFeatureValue("1"));
			row.put("im_efct_pv", new JsonFeatureValue(0L));
			row.put("s_complaint_ratio_90d", new JsonFeatureValue(0.0));
			row.put("s_dispute_ratio_90d", new JsonFeatureValue(0.0));
			row.put("from_value", new JsonFeatureValue("alxm"));
			row.put("has_consign", new JsonFeatureValue("1"));
			row.put("has_ls", new JsonFeatureValue("1"));
			row.put("has_sign", new JsonFeatureValue("1"));
			row.put("sentiment_list", new JsonFeatureValue("?"));
			row.put("case_rank", new JsonFeatureValue(1L));
			row.put("is_case_end", new JsonFeatureValue(1L));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return request;
	}

	public static void main(String[] args) throws Exception {
		//启动并初始化客户端
		PredictClient client = new PredictClient(new HttpConfig());
		 /*
         * 如果Internet Endpoint为http://eas-shanghai.alibaba-inc.com/api/predict/credit
         * 则client.setEndpoint("eas-shanghai.alibaba-inc.com");
         * client.setModelName("credit")
         * client.setToken需要根据服务具体生成的token设置
         */

		client.setEndpoint("eas-shanghai.alibaba-inc.com")
				.setModelName("pai_model_test");
		
		/*
		String[] inputKey = { "featrue_1", "featrue_2", "featrue_3" };	//输入表的列名
		double[] inputValue = {10.7, 1, 2.5};	//输入表列对应的值
		
		//将每列对应输入request
		JsonRequest request = new JsonRequest();
		Map<String, JsonFeatureValue> row = request.addRow();
		for (int i = 0; i < inputKey.length; i++) {
			JsonFeatureValue param = new JsonFeatureValue(inputValue[i]);
			row.put(inputKey[i], param);
		}*/

		JsonRequest request = buildOrderPredictJsonRequest();
		//输入为JsonRequest类型
		System.out.println(request.getJSON());


		client.setRetryCount(0);
		//返回为JsonResponse类型
		JsonResponse response = client.predict(request);
//		JsonResponse response = client.SetVerb("GET").predict(request);
		System.out.println(response.getOutputs().get(0).getOutputLabel());
		System.out.println(response.getOutputs().get(0).getOutputValue());
		
/*		
		HashMap<String, String> mapHeader = new HashMap<String, String>();
		JsonResponse response = client.setTracing(mapHeader).predict(request);	
		for (Map.Entry<String, String> entry : mapHeader.entrySet()) { 
			  System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
			}
*/		
		
		//System.out.println(response.getOutputs().get(0).getOutputValue());
		
		
		//关闭客户端
		client.shutdown();
		return;
	}
} 
