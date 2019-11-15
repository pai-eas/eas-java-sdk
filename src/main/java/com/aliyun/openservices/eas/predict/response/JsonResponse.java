package com.aliyun.openservices.eas.predict.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.google.common.reflect.TypeToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonResponse {
	public static class ResponseItem {
		private String outputLabel;
		private Map<String, Object> outputValue;
		private Map<String, Double> outputMulti;

		public String getOutputLabel() {
			return outputLabel;
		}

		public void setOutputLabel(String outputLabel) {
			this.outputLabel = outputLabel;
		}

		public Map<String, Object> getOutputValue() {
			return outputValue;
		}

		public void setOutputValue(Map<String, Object> outputValue) {
			this.outputValue = outputValue;
		}

		public Map<String, Double> getOutputMulti() {
			return outputMulti;
		}

		public void setOutputMulti(Map<String, Double> outputMulti) {
			this.outputMulti = outputMulti;
		}
	}

	public void setContentValues(byte[] content) throws Exception {
		try {
			Map<String, Serializable> output = JSON.parseObject(new String(content),
					new TypeToken<Map<String, Serializable>>(){}.getType());
			List<HashMap<String, Serializable>> outputRecords = JSON.parseObject(
					output.get("outputs").toString(),
					new TypeToken<List<HashMap<String, Serializable>>>(){}.getType());
			for (int i = 0; i< outputRecords.size(); i++) {
				ResponseItem item = new ResponseItem();
				HashMap<String, Serializable> outputRecord = outputRecords.get(i);
				item.setOutputLabel(outputRecord.get("outputLabel").toString());
				Map<String, Double> outputMulti = JSON.parseObject(
						outputRecord.get("outputMulti").toString(),
						new TypeToken<Map<String, Double>>(){}.getType());
				item.setOutputMulti(outputMulti);
				Map<String, Object> outputValue = JSON.parseObject(
						outputRecord.get("outputValue").toString(),
						new TypeToken<Map<String, Object>>(){}.getType());
				item.setOutputValue(outputValue);
				outputs.add(item);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<ResponseItem> outputs = new ArrayList<>();

	public List<ResponseItem> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<ResponseItem> outputs) {
		this.outputs = outputs;
	}
}
