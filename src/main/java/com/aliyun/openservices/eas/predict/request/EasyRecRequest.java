package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.EasyRecPredictProtos;

import java.util.List;


public class EasyRecRequest {
    public static final String CTRL_B = "\u0002";

    public static final String HA3_MULTI_SPLIT = "\u001D";
    public static final String MULTI_SPLIT = "|";
    private String separator = CTRL_B;
    private EasyRecPredictProtos.PBRequest.Builder request = EasyRecPredictProtos.PBRequest.newBuilder();

    public EasyRecRequest() {
    }
    public void setDebugLevel(int level) {
        request.setDebugLevel(level);
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void addUserFeature(String key, String value) {
        request.putUserFeatures(key, EasyRecPredictProtos.PBFeature.newBuilder().setStringFeature(value).build());
    }

    public void addUserFeature(String key, float value) {
        request.putUserFeatures(key, EasyRecPredictProtos.PBFeature.newBuilder().setFloatFeature(value).build());
    }

    public void addUserFeature(String key, long value) {
        request.putUserFeatures(key, EasyRecPredictProtos.PBFeature.newBuilder().setLongFeature(value).build());
    }

    public void addUserFeature(String key, int value) {
        request.putUserFeatures(key, EasyRecPredictProtos.PBFeature.newBuilder().setIntFeature(value).build());
    }

    public void appendUserFeatureString(String featureStr) {
        String[] userFeas = featureStr.split(this.separator);
        for (String fea : userFeas) {
            if (! fea.contains(":")) {
                continue;
            }

            String[] feaList = fea.split(":");
            String value = EasyRecRequest.buildValue(feaList);
            this.addUserFeature(feaList[0], value);
        }
    }

    public static String buildValue(String[] values) {
        int len = 2;
        // the first one is the key
        if (values.length < len) {
            return "";
        }

        // the first one is the key
        String joinStr = values[1];
        if (values.length > len) {
            StringBuilder buffer = new StringBuilder();
            for(int i = 1; i < values.length; i++) {
                buffer.append(values[i]);
                if (i < values.length -1) {
                    buffer.append(":");
                }
            }
            joinStr = buffer.toString();
        }

        // match feature maps user have to replace Common.HA3_MULTI_SPLIT with ","
        if (joinStr.indexOf(MULTI_SPLIT) > 0 &&
                joinStr.indexOf(EasyRecRequest.HA3_MULTI_SPLIT) > 0) {
            joinStr = joinStr.replace(EasyRecRequest.HA3_MULTI_SPLIT, ",");
        }
        return joinStr;
    }

    public void addContextFeature(String key, List<Object> contextFeatures) {
        EasyRecPredictProtos.ContextFeatures.Builder ctxBuilder = EasyRecPredictProtos.ContextFeatures.newBuilder();
        for (Object fea : contextFeatures) {
            if (fea instanceof String) {
                ctxBuilder.addFeatures(EasyRecPredictProtos.PBFeature.newBuilder().setStringFeature((String) fea).build());
            } else if (fea instanceof Integer) {
                ctxBuilder.addFeatures(EasyRecPredictProtos.PBFeature.newBuilder().setIntFeature((Integer) fea).build());
            } else if (fea instanceof  Float) {
                ctxBuilder.addFeatures(EasyRecPredictProtos.PBFeature.newBuilder().setFloatFeature((Float) fea).build());
            } else if (fea instanceof  Long) {
                ctxBuilder.addFeatures(EasyRecPredictProtos.PBFeature.newBuilder().setLongFeature((Long) fea).build());
            }
        }
        request.putContextFeatures(key, ctxBuilder.build());
    }


    public void appendContextFeatureString(String contextStr) {
        String[] ctxFeas = contextStr.split(this.separator);
        for (String fea : ctxFeas) {
            if (! fea.contains(":")) {
                continue;
            }
            String[] feaList = fea.split(":");
            EasyRecPredictProtos.ContextFeatures.Builder ctxBuilder =
                    EasyRecPredictProtos.ContextFeatures.newBuilder();
            for (int i = 1; i < feaList.length; i++) {
                ctxBuilder.addFeatures(EasyRecPredictProtos.PBFeature.newBuilder().setStringFeature(feaList[i]).build() );
            }
            request.putContextFeatures(feaList[0], ctxBuilder.build());
        }
    }

    public void appendItemId(String itemId) {
        request.addItemIds(itemId);
    }
    public void appendItemStr(String itemIdsStr) {
        String[] itemIds = itemIdsStr.split(",");
        for (String itemId : itemIds) {
            request.addItemIds(itemId);
        }
    }

    public EasyRecPredictProtos.PBRequest getRequest() {
        return request.build();
    }
}
