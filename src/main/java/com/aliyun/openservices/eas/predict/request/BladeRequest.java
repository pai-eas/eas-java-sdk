package com.aliyun.openservices.eas.predict.request;

import com.aliyun.openservices.eas.predict.proto.BladeProtos.DataType;
import com.aliyun.openservices.eas.predict.proto.BladeProtos.Shape;
import com.aliyun.openservices.eas.predict.proto.BladeProtos.Array;
import com.aliyun.openservices.eas.predict.proto.BladeProtos.OutputInfo;
import com.aliyun.openservices.eas.predict.proto.BladeProtos.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import shade.blade.protobuf.ByteString;

/**
 * Created by evanlu.lyf on 2019/10/14.
 */
public class BladeRequest {
    private Request.Builder request = Request.newBuilder();
    private static Log log = LogFactory.getLog(BladeRequest.class);

    public void setSignatureName(String signatureName) {
        request.setTfSignatureName(signatureName);
    }

    private void fillFloatTypeContent(float[] content, long volume, Array.Builder requestArray) {
        for (int i = 0; i < volume; ++i) {
            requestArray.addFloatVal(content[i]);
        }
    }

    private void fillInt32TypeContent(int[] content, long volume, Array.Builder requestArray) {
        for (int i = 0; i < volume; ++i) {
            requestArray.addIntVal(content[i]);
        }
    }

    private void fillInt64TypeContent(long[] content, long volume, Array.Builder requestArray) {
        for (int i = 0; i < volume; ++i) {
            requestArray.addInt64Val(content[i]);
        }
    }

    private void fillStringTypeContent(byte[] content, long volume, Array.Builder requestArray) {
        for (int i = 0; i < volume; ++i) {
            requestArray.addStringVal(ByteString.copyFromUtf8(content.toString()));
        }
    }

    private long getVolumeAndSetArrayShape(int batchSize, long[] shape, Shape.Builder arrayShape) {
        if (batchSize <= 0) {
            log.error("call addFeed Error: got invalid batch size " + batchSize);
            throw new RuntimeException("call addFeed Error: got invalid batch size " + batchSize);
        }
        long volume = 1;
        for (int i = 0; i < shape.length; ++i) {
            volume *= shape[i];
            arrayShape.addDim(shape[i]);
        }
        return volume;
    }

    private long makeArrayBuilderAndReturnVolume(Array.Builder requestArray, long contentLen, int batchSize,
                                                 long [] shape, String nameInDlModel) {
        Shape.Builder arrayShape = Shape.newBuilder();
        long volume = getVolumeAndSetArrayShape(batchSize, shape, arrayShape);
        if (contentLen < volume) {
            log.error("call addFeed Error: array length should be larger than flattern size of shape");
            throw new RuntimeException("call addFeed Error: array length should be larger than flattern size of shape");
        }
        requestArray.mergeShape(arrayShape.build());
        requestArray.setNameInDlModel(nameInDlModel);
        requestArray.setBatchsize(batchSize);
        return volume;
    }

    private long makeArrayBuilderAndReturnVolume(Array.Builder requestArray, long contentLen, int batchSize, 
                                long [] shape, String bladeBlobName, String nameInDlModel) {
        Shape.Builder arrayShape = Shape.newBuilder();
        long volume = getVolumeAndSetArrayShape(batchSize, shape, arrayShape);
        if (contentLen < volume) {
            log.error("call addFeed Error: array length should be larger than flattern size of shape");
            throw new RuntimeException("call addFeed Error: array length should be larger than flattern size of shape");
        }
        requestArray.mergeShape(arrayShape.build());
        requestArray.setBladeBlobName(bladeBlobName);
        requestArray.setNameInDlModel(nameInDlModel);
        requestArray.setBatchsize(batchSize);
        return volume;
    }

    /**
     * add float typed feed
     */
    public void addFeed(float[] content, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillFloatTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(float[] content, int batchSize, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillFloatTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(float[] content, int batchSize, long[] shape, String bladeBlobName, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, bladeBlobName, nameInDlModel);
        fillFloatTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    /**
     * support EAS default addFeed API
     */
    public void addFeed(String nameInDlModel, BladeDataType dataType, long[] shape, float[] content) {
        if (dataType != BladeDataType.DT_FLOAT) {
            log.error("call addFeed Error: BladeDataType and content mis-match.");
            throw new RuntimeException("call addFeed Error: BladeDataType and content mis-match.");
        }
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillFloatTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }


    /**
     * add int typed feed
     */
    public void addFeed(int[] content, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt32TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(int[] content, int batchSize, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt32TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(int[] content, int batchSize, long[] shape, String bladeBlobName, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, bladeBlobName, nameInDlModel);
        fillInt32TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    /**
     * support EAS default addFeed API
     */
    public void addFeed(String nameInDlModel, BladeDataType dataType, long[] shape, int[] content) {
        if (dataType != BladeDataType.DT_INT32) {
            log.error("call addFeed Error: BladeDataType and content mis-match.");
            throw new RuntimeException("call addFeed Error: BladeDataType and content mis-match.");
        }
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt32TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }


    /**
     * add long typed feed
     */
    public void addFeed(long[] content, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt64TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(long[] content, int batchSize, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt64TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(long[] content, int batchSize, long[] shape, String bladeBlobName, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, bladeBlobName, nameInDlModel);
        fillInt64TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    /**
     * support EAS default addFeed API
     */
    public void addFeed(String nameInDlModel, BladeDataType dataType, long[] shape, long[] content) {
        if (dataType != BladeDataType.DT_INT64) {
            log.error("call addFeed Error: BladeDataType and content mis-match.");
            throw new RuntimeException("call addFeed Error: BladeDataType and content mis-match.");
        }
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillInt64TypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }


    /**
     * add string typed feed
     */
    public void addFeed(byte[] content, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillStringTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(byte[] content, int batchSize, long[] shape, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillStringTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    public void addFeed(byte[] content, int batchSize, long[] shape, String bladeBlobName, String nameInDlModel) {
        Array.Builder requestArray = Array.newBuilder();
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, bladeBlobName, nameInDlModel);
        fillStringTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }

    /**
     * support EAS default addFeed API
     */
    public void addFeed(String nameInDlModel, BladeDataType dataType, long[] shape, byte[] content) {
        if (dataType != BladeDataType.DT_STRING) {
            log.error("call addFeed Error: BladeDataType and content mis-match.");
            throw new RuntimeException("call addFeed Error: BladeDataType and content mis-match.");
        }
        Array.Builder requestArray = Array.newBuilder();
        int batchSize = (int) shape[0];
        long volume = makeArrayBuilderAndReturnVolume(requestArray, content.length, batchSize, shape, nameInDlModel);
        fillStringTypeContent(content, volume, requestArray);
        request.addInputs(requestArray.build());
    }


    /**
     * add Fetch by name
     */
    public void addFetch(String nameInDlmodel) {
        OutputInfo.Builder outputInfo = OutputInfo.newBuilder();
        outputInfo.setNameInDlModel(nameInDlmodel);
        outputInfo.setDataType(DataType.DT_UNKNOWN);
        request.addOutputInfo(outputInfo.build());
    }

    public void addFetch(String bladeBlobName, String nameInDlmodel) {
        OutputInfo.Builder outputInfo = OutputInfo.newBuilder();
        outputInfo.setBladeBlobName(bladeBlobName);
        outputInfo.setNameInDlModel(nameInDlmodel);
        outputInfo.setDataType(DataType.DT_UNKNOWN);
        request.addOutputInfo(outputInfo.build());
    }

    public void addFetch(String nameInDlmodel, BladeDataType dataType) {
        OutputInfo.Builder outputInfo = OutputInfo.newBuilder();
        outputInfo.setNameInDlModel(nameInDlmodel);
        if (dataType == BladeDataType.DT_UNKNOWN) {
            outputInfo.setDataType(DataType.DT_UNKNOWN);
        }
        else if (dataType == BladeDataType.DT_FLOAT) {
            outputInfo.setDataType(DataType.DT_FLOAT);
        }
        else if (dataType == BladeDataType.DT_INT32) {
            outputInfo.setDataType(DataType.DT_INT32);
        }
        else if (dataType == BladeDataType.DT_INT64) {
            outputInfo.setDataType(DataType.DT_INT64);
        }
        else if (dataType == BladeDataType.DT_STRING) {
            outputInfo.setDataType(DataType.DT_STRING);
        }
        else {
            log.error("call addFeed Error: got invalid output DataType");
            throw new RuntimeException("call addFeed Error: got invalid output DataType");
        }
        request.addOutputInfo(outputInfo.build());
    }

    public void addFetch(String bladeBlobName, String nameInDlmodel, BladeDataType dataType) {
        OutputInfo.Builder outputInfo = OutputInfo.newBuilder();
        outputInfo.setBladeBlobName(bladeBlobName);
        outputInfo.setNameInDlModel(nameInDlmodel);
        if (dataType == BladeDataType.DT_UNKNOWN) {
            outputInfo.setDataType(DataType.DT_UNKNOWN);
        }
        else if (dataType == BladeDataType.DT_FLOAT) {
            outputInfo.setDataType(DataType.DT_FLOAT);
        }
        else if (dataType == BladeDataType.DT_INT32) {
            outputInfo.setDataType(DataType.DT_INT32);
        }
        else if (dataType == BladeDataType.DT_INT64) {
            outputInfo.setDataType(DataType.DT_INT64);
        }
        else if (dataType == BladeDataType.DT_STRING) {
            outputInfo.setDataType(DataType.DT_STRING);
        }
        else {
            log.error("call addFeed Error: got invalid output DataType");
            throw new RuntimeException("call addFeed Error: got invalid output DataType");
        }
        request.addOutputInfo(outputInfo.build());
    }


    public Request getRequest() {
        return request.build();
    }
}
