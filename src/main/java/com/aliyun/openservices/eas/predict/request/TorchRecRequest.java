package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.TorchRecPredictProtos;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class TorchRecRequest {

    public TorchRecPredictProtos.PBRequest.Builder request = TorchRecPredictProtos.PBRequest.newBuilder();

    public TorchRecRequest() {
    }
    public void setDebugLevel(int level) {
        request.setDebugLevel(level);
    }
    public void setFaissNeighNum(int k) {
        request.setFaissNeighNum(k);
    }

    public TorchRecPredictProtos.PBFeature addFeature(Object value, String dtype) {
        dtype = dtype.toUpperCase();
        TorchRecPredictProtos.PBFeature.Builder featBuilder = TorchRecPredictProtos.PBFeature.newBuilder();

        if (value == null ||
                (value instanceof List && ((List<?>) value).isEmpty()) ||
                (value instanceof Map && ((Map<?, ?>) value).isEmpty())) {
            return featBuilder.setStringFeature("").build();
        } else if (dtype.equals("STRING")) {
            return featBuilder.setStringFeature((String) value).build();
        } else if (dtype.equals("FLOAT")) {
            return featBuilder.setFloatFeature((Float) value).build();
        } else if (dtype.equals("DOUBLE")) {
            return featBuilder.setDoubleFeature((Double) value).build();
        } else if (dtype.equals("BIGINT") || dtype.equals("INT64")) {
            return featBuilder.setLongFeature((Long) value).build();
        } else if (dtype.equals("INT")) {
            return featBuilder.setIntFeature((Integer) value).build();
        } else if (isListType(dtype)) {
            if (!(value instanceof List)) {
                throw new IllegalArgumentException("Expected value to be a list for LIST<xxx>/ARRAY<xxx> dtype");
            }
            return addToListField(featBuilder, value, dtype);

        } else if (isMapType(dtype)) {
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Expected value to be a dict for MAP<xxx,xxx> dtype");
            }

            return addToMapField(featBuilder, value, dtype);
        } else if (dtype.equals("ARRAY<ARRAY<FLOAT>>") || dtype.equals("LIST<LIST<FLOAT>>")) {
            if (!(value instanceof List) || !((List<?>) value).stream().allMatch(v -> v instanceof List)) {
                throw new IllegalArgumentException("Expected value to be a list of lists for ARRAY<ARRAY<FLOAT>>/LIST<LIST<FLOAT>> dtype");
            }
            List<List<?>> listOfLists = (List<List<?>>) value;
            TorchRecPredictProtos.FloatLists.Builder floatListsBuilder = TorchRecPredictProtos.FloatLists.newBuilder();
            for (List<?> sublist : listOfLists) {
                TorchRecPredictProtos.FloatList.Builder floatListBuilder = TorchRecPredictProtos.FloatList.newBuilder();
                for (Object subValue : sublist) {
                    floatListBuilder.addFeatures((Float) subValue);
                }
                floatListsBuilder.addLists(floatListBuilder);

            }
            return featBuilder.setFloatLists(floatListsBuilder).build();

        } else {

            throw new IllegalArgumentException("unsupported dtype: " + dtype);
        }

    }

    private TorchRecPredictProtos.PBFeature addToListField(TorchRecPredictProtos.PBFeature.Builder featBuilder, Object value, String dtype) {
        if (dtype.equals("LIST<STRING>") || dtype.equals("ARRAY<STRING>")) {
            TorchRecPredictProtos.StringList.Builder stringListBuilder = TorchRecPredictProtos.StringList.newBuilder();
            for (Object obj : (List<?>) value)  {
                stringListBuilder.addFeatures((String) obj);
            }
            return featBuilder.setStringList(stringListBuilder).build();
        } else if (dtype.equals("LIST<FLOAT>") || dtype.equals("ARRAY<FLOAT>")) {
            TorchRecPredictProtos.FloatList.Builder floatListBuilder = TorchRecPredictProtos.FloatList.newBuilder();
            for (Object obj : (List<?>) value)  {
                floatListBuilder.addFeatures((Float) obj);
            }
            return featBuilder.setFloatList(floatListBuilder).build();
        } else if (dtype.equals("LIST<DOUBLE>") || dtype.equals("ARRAY<DOUBLE>")) {
            TorchRecPredictProtos.DoubleList.Builder doubleListBuilder = TorchRecPredictProtos.DoubleList.newBuilder();
            for (Object obj : (List<?>) value)  {
                doubleListBuilder.addFeatures((Double) obj);
            }
            return featBuilder.setDoubleList(doubleListBuilder).build();
        } else if (dtype.equals("LIST<BIGINT>") || dtype.equals("LIST<INT64>") ||
                dtype.equals("ARRAY<BIGINT>") || dtype.equals("ARRAY<INT64>")) {
            TorchRecPredictProtos.LongList.Builder longListBuilder = TorchRecPredictProtos.LongList.newBuilder();
            for (Object obj : (List<?>) value)  {
                longListBuilder.addFeatures((Long) obj);
            }
            return featBuilder.setLongList(longListBuilder).build();
        } else if (dtype.equals("LIST<INT>") || dtype.equals("ARRAY<INT>")) {
            TorchRecPredictProtos.IntList.Builder intListBuilder = TorchRecPredictProtos.IntList.newBuilder();
            for (Object obj : (List<?>) value)  {
                intListBuilder.addFeatures((Integer) obj);
            }
            return featBuilder.setIntList(intListBuilder).build();
        }
        else {
            throw new IllegalArgumentException("unsupported dtype: " + dtype);
        }
    }

    private TorchRecPredictProtos.PBFeature addToMapField(TorchRecPredictProtos.PBFeature.Builder featBuilder, Object value, String dtype) {
        Map<Object, Object> mapValue = (Map<Object, Object>) value;
        // int->others
        if (dtype.equals("MAP<INT,INT>")) {
            TorchRecPredictProtos.IntIntMap.Builder Builder = TorchRecPredictProtos.IntIntMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Integer) entry.getKey(), (Integer) entry.getValue());
            }
            return featBuilder.setIntIntMap(Builder).build();
        }else if (dtype.equals("MAP<INT,INT64>") || dtype.equals("MAP<INT,BIGINT>")) {
            TorchRecPredictProtos.IntLongMap.Builder Builder = TorchRecPredictProtos.IntLongMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Integer) entry.getKey(), (Long) entry.getValue());
            }
            return featBuilder.setIntLongMap(Builder).build();
        }else if (dtype.equals("MAP<INT,STRING>")) {
            TorchRecPredictProtos.IntStringMap.Builder Builder = TorchRecPredictProtos.IntStringMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Integer) entry.getKey(), (String) entry.getValue());
            }
            return featBuilder.setIntStringMap(Builder).build();
        }else if (dtype.equals("MAP<INT,FLOAT>")) {
            TorchRecPredictProtos.IntFloatMap.Builder Builder = TorchRecPredictProtos.IntFloatMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Integer) entry.getKey(), (Float) entry.getValue());
            }
            return featBuilder.setIntFloatMap(Builder).build();
        }else if (dtype.equals("MAP<INT,DOUBLE>")) {
            TorchRecPredictProtos.IntDoubleMap.Builder Builder = TorchRecPredictProtos.IntDoubleMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Integer) entry.getKey(), (Double) entry.getValue());
            }
            return featBuilder.setIntDoubleMap(Builder).build();
        }

        // int64->others
        else if (dtype.equals("MAP<INT64,INT>") || dtype.equals("MAP<BIGINT,INT>")) {
            TorchRecPredictProtos.LongIntMap.Builder Builder = TorchRecPredictProtos.LongIntMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Long) entry.getKey(), (Integer) entry.getValue());
            }
            return featBuilder.setLongIntMap(Builder).build();
        }else if (dtype.equals("MAP<INT64,INT64>") || dtype.equals("MAP<INT64,BIGINT>") || dtype.equals("MAP<BIGINT,INT64>") || dtype.equals("MAP<BIGINT,BIGINT>")) {
            TorchRecPredictProtos.LongLongMap.Builder Builder = TorchRecPredictProtos.LongLongMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Long) entry.getKey(), (Long) entry.getValue());
            }
            return featBuilder.setLongLongMap(Builder).build();
        }else if (dtype.equals("MAP<INT64,STRING>") || dtype.equals("MAP<BIGINT,STRING>")) {
            TorchRecPredictProtos.LongStringMap.Builder Builder = TorchRecPredictProtos.LongStringMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Long) entry.getKey(), (String) entry.getValue());
            }
            return featBuilder.setLongStringMap(Builder).build();
        }else if (dtype.equals("MAP<INT64,FLOAT>") || dtype.equals("MAP<BIGINT,FLOAT>")) {
            TorchRecPredictProtos.LongFloatMap.Builder Builder = TorchRecPredictProtos.LongFloatMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Long) entry.getKey(), (Float) entry.getValue());
            }
            return featBuilder.setLongFloatMap(Builder).build();
        }else if (dtype.equals("MAP<INT64,DOUBLE>") || dtype.equals("MAP<BIGINT,DOUBLE>")) {
            TorchRecPredictProtos.LongDoubleMap.Builder Builder = TorchRecPredictProtos.LongDoubleMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((Long) entry.getKey(), (Double) entry.getValue());
            }
            return featBuilder.setLongDoubleMap(Builder).build();
        }

        // string -> others
        else if (dtype.equals("MAP<STRING,INT>")) {
            TorchRecPredictProtos.StringIntMap.Builder Builder = TorchRecPredictProtos.StringIntMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((String) entry.getKey(), (Integer) entry.getValue());
            }
            return featBuilder.setStringIntMap(Builder).build();
        }else if (dtype.equals("MAP<STRING,INT64>") || dtype.equals("MAP<STRING,BIGINT>")) {
            TorchRecPredictProtos.StringLongMap.Builder Builder = TorchRecPredictProtos.StringLongMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((String) entry.getKey(), (Long) entry.getValue());
            }
            return featBuilder.setStringLongMap(Builder).build();
        }else if (dtype.equals("MAP<STRING,STRING>")) {
            TorchRecPredictProtos.StringStringMap.Builder Builder = TorchRecPredictProtos.StringStringMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((String) entry.getKey(), (String) entry.getValue());
            }
            return featBuilder.setStringStringMap(Builder).build();
        }else if (dtype.equals("MAP<STRING,FLOAT>")) {
            TorchRecPredictProtos.StringFloatMap.Builder Builder = TorchRecPredictProtos.StringFloatMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((String) entry.getKey(), (Float) entry.getValue());
            }
            return featBuilder.setStringFloatMap(Builder).build();
        }else if (dtype.equals("MAP<STRING,Double>")) {
            TorchRecPredictProtos.StringDoubleMap.Builder Builder = TorchRecPredictProtos.StringDoubleMap.newBuilder();
            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Builder.putMapField((String) entry.getKey(), (Double) entry.getValue());
            }
            return featBuilder.setStringDoubleMap(Builder).build();
        }

        else {
            throw new IllegalArgumentException("unsupported dtype: " + dtype);
        }
    }

    private boolean isListType(String dtype) {
        List<String> validTypes = Arrays.asList(
                "LIST<FLOAT>",
                "LIST<STRING>",
                "LIST<DOUBLE>",
                "LIST<INT>",
                "LIST<INT64>",
                "LIST<BIGINT>",
                "ARRAY<FLOAT>",
                "ARRAY<STRING>",
                "ARRAY<DOUBLE>",
                "ARRAY<INT>",
                "ARRAY<INT64>",
                "ARRAY<BIGINT>"
        );
        return validTypes.contains(dtype);
    }

    private boolean isMapType(String dtype) {
        List<String> validTypes = Arrays.asList(
                "MAP<INT,INT>","MAP<INT,INT64>","MAP<INT,BIGINT>","MAP<INT,STRING>","MAP<INT,FLOAT>","MAP<INT,DOUBLE>" ,
                "MAP<INT64,INT>","MAP<INT64,INT64>","MAP<INT64,BIGINT>","MAP<INT64,STRING>","MAP<INT64,FLOAT>","MAP<INT64,DOUBLE>" ,
                "MAP<BIGINT,INT>","MAP<BIGINT,INT64>","MAP<BIGINT,BIGINT>","MAP<BIGINT,STRING>","MAP<BIGINT,FLOAT>","MAP<BIGINT,DOUBLE>",
                "MAP<STRING,INT>","MAP<STRING,INT64>","MAP<STRING,BIGINT>","MAP<STRING,STRING>","MAP<STRING,FLOAT>","MAP<STRING,DOUBLE>"
        );
        return validTypes.contains(dtype);
    }

    public void addUserFeature(String key, Object value, String dtype) {
        request.putUserFeatures(key, addFeature(value, dtype));
    }


    public void addContextFeature(String key, Object value, String dtype) {
        TorchRecPredictProtos.PBFeature feat = addFeature(value, dtype);
        TorchRecPredictProtos.ContextFeatures.Builder ctxBuilder ;
        if(request.getContextFeaturesMap().containsKey(key)){
            ctxBuilder = request.getContextFeaturesMap().get(key).toBuilder();
        }
        else{
            ctxBuilder = TorchRecPredictProtos.ContextFeatures.newBuilder();
        }
        ctxBuilder.addFeatures(feat);
        request.putContextFeatures(key, ctxBuilder.build());
    }

    public void addItemFeature(String key, Object value, String dtype) {
        TorchRecPredictProtos.PBFeature feat = addFeature(value, dtype);
        TorchRecPredictProtos.ContextFeatures.Builder ctxBuilder ;
        if(request.getItemFeaturesMap().containsKey(key)){
            ctxBuilder = request.getItemFeaturesMap().get(key).toBuilder();
        }
        else{
            ctxBuilder = TorchRecPredictProtos.ContextFeatures.newBuilder();
        }
        ctxBuilder.addFeatures(feat);
        request.putItemFeatures(key, ctxBuilder.build());
    }

    public void addMetaData(String key, String value) {
        if (key != null && value != null) {
            request.putMetaData(key, value);
        }
    }

    public void appendItemId(String itemId) {
        request.addItemIds(itemId);
    }


    public TorchRecPredictProtos.PBRequest getRequest() {
        return request.build();
    }
}
