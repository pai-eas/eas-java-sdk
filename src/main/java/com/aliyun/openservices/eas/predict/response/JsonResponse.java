package com.aliyun.openservices.eas.predict.response;

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
	
	private List<ResponseItem> outputs;

	public List<ResponseItem> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<ResponseItem> outputs) {
		this.outputs = outputs;
	}
}
