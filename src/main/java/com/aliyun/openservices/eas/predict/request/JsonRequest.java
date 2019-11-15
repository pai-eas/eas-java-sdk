package com.aliyun.openservices.eas.predict.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

public class JsonRequest {
	private List<Map<String, JsonFeatureValue>> inputs = new ArrayList<Map<String, JsonFeatureValue>>();

	public List<Map<String, JsonFeatureValue>> getInputs() {
		return inputs;
	}

	public String getJSON() throws JSONException, IOException {
		ArrayList<JSONObject> inputJsonList = new ArrayList<JSONObject>();
		for(Map<String, JsonFeatureValue> input : inputs) {
			JSONObject item = new JSONObject();
			for (Map.Entry<String, JsonFeatureValue> entry : input.entrySet()) {
				item.put(entry.getKey(), entry.getValue());
			}
			inputJsonList.add(item);
		}
		JSONObject input = new JSONObject();
		input.put("inputs", inputJsonList);
		return JSON.toJSONString(input);
	}
	
	public void setInputs(List<Map<String, JsonFeatureValue>> inputs) {
		this.inputs = inputs;
	}
	
	public Map<String, JsonFeatureValue> addRow() {
		Map<String, JsonFeatureValue> newMap = new HashMap<String, JsonFeatureValue>();
		inputs.add(newMap);
		return newMap;
	}
}
