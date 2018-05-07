package com.aliyun.openservices.eas.predict.request;

public class JsonFeatureValue {
	private static final int TYPE_NULL = 0;
	private static final int TYPE_BOOL = 1;
	private static final int TYPE_INT32 = 10;
	private static final int TYPE_INT64 = 20;
	private static final int TYPE_FLOAT = 30;
	private static final int TYPE_DOUBLE = 40;
	private static final int TYPE_STRING = 50;

	private int dataType;
	private Object dataValue;

	public JsonFeatureValue(boolean value) {
		dataType = TYPE_BOOL;
		dataValue = value;
	}
	
	public JsonFeatureValue(int value) {
		dataType = TYPE_INT32;
		dataValue = value;
	}

	public JsonFeatureValue(long value) {
		dataType = TYPE_INT64;
		dataValue = value;
	}
	
	public JsonFeatureValue(float value) {
		dataType = TYPE_FLOAT;
		dataValue = value;
	}
	
	public JsonFeatureValue(double value) {
		dataType = TYPE_DOUBLE;
		dataValue = value;
	}

	public JsonFeatureValue(String value) {
		dataType = TYPE_STRING;
		dataValue = value;
	}
	
	public JsonFeatureValue(Object value) {
        if (value instanceof Boolean) {
            dataType = TYPE_BOOL;
        } else if (value instanceof Integer) {
            dataType = TYPE_INT32;
        } else if (value instanceof Long) {
            dataType = TYPE_INT64;
        } else if (value instanceof Float) {
            dataType = TYPE_FLOAT;
        } else if (value instanceof Double) {
            dataType = TYPE_DOUBLE;
        } else if (value instanceof String) {
            dataType = TYPE_STRING;
        } else {
        		dataType = TYPE_NULL;
        }
        dataValue = value;
	}

	public int getDataType() {
		return dataType;
	}

	public Object getDataValue() {
		return dataValue;
	}
}

