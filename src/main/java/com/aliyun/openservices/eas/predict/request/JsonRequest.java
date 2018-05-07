package com.aliyun.openservices.eas.predict.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonRequest {
	private List<Map<String, JsonFeatureValue>> inputs = new ArrayList<Map<String, JsonFeatureValue>>();
	private ObjectMapper defaultObjectMapper = new ObjectMapper();
	
	public List<Map<String, JsonFeatureValue>> getInputs() {
		return inputs;
	}

	public String getJSON() throws JsonGenerationException, JsonMappingException, IOException {
		return defaultObjectMapper.writeValueAsString(inputs);
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
