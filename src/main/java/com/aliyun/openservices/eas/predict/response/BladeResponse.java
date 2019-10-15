package com.aliyun.openservices.eas.predict.response;

import com.aliyun.openservices.eas.predict.proto.BladeProtos.Response;
import shade.blade.protobuf.ByteString;
import shade.blade.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class BladeResponse {
    private static Log log = LogFactory.getLog(BladeResponse.class);
    private Response response = null;

    public void setContentValues(byte[] content) {
        try {
            response = Response.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public List<Long> getShapeByNameInDlModel(String nameInDlModel) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getNameInDlModel().equals(nameInDlModel)) {
                    return response.getOutputs(i).getShape().getDimList();
                }
            }
            log.error("request failed: can't get response by NameInDlModel " + nameInDlModel);
            return new ArrayList<Long>();
        } else {
            log.error("request failed: can't get response by NameInDlModel " + nameInDlModel);
            return new ArrayList<Long>();
        }
    }

    public List<Long> getShapeByBladeBlobName(String bladeBlobName) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getBladeBlobName().equals(bladeBlobName)) {
                    return response.getOutputs(i).getShape().getDimList();
                }
            }
            log.error("request failed: can't get response by BladeBlobName " + bladeBlobName);
            return new ArrayList<Long>();
        } else {
            log.error("request failed: can't get response by BladeBlobName " + bladeBlobName);
            return new ArrayList<Long>();
        }
    }

    public List<Float> getFloatValsByNameInDlModel(String nameInDlModel) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getNameInDlModel().equals(nameInDlModel)) {
                    return response.getOutputs(i).getFloatValList();
                }
            }
            log.error("Not Found output name : " + nameInDlModel);
            return new ArrayList<Float>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
    }

    public List<Float> getFloatValsByBladeBlobName(String bladeBlobName) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getBladeBlobName().equals(bladeBlobName)) {
                    return response.getOutputs(i).getFloatValList();
                }
            }
            log.error("Not Found output name : " + bladeBlobName);
            return new ArrayList<Float>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Float>();
        }
    }

    public List<Integer> getInt32ValsByNameInDlModel(String nameInDlModel) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getNameInDlModel().equals(nameInDlModel)) {
                    return response.getOutputs(i).getIntValList();
                }
            }
            log.error("Not Found output name : " + nameInDlModel);
            return new ArrayList<Integer>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Integer>();
        }
    }

    public List<Integer> getInt32ValsByBladeBlobName(String bladeBlobName) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getBladeBlobName().equals(bladeBlobName)) {
                    return response.getOutputs(i).getIntValList();
                }
            }
            log.error("Not Found output name : " + bladeBlobName);
            return new ArrayList<Integer>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Integer>();
        }
    }

    public List<Long> getInt64ValsByNameInDlModel(String nameInDlModel) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getNameInDlModel().equals(nameInDlModel)) {
                    return response.getOutputs(i).getInt64ValList();
                }
            }
            log.error("Not Found output name : " + nameInDlModel);
            return new ArrayList<Long>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<Long> getInt64ValsByBladeBlobName(String bladeBlobName) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getBladeBlobName().equals(bladeBlobName)) {
                    return response.getOutputs(i).getInt64ValList();
                }
            }
            log.error("Not Found output name : " + bladeBlobName);
            return new ArrayList<Long>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<Long>();
        }
    }

    public List<String> getStringValsByNameInDlModel(String nameInDlModel) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getNameInDlModel().equals(nameInDlModel)) {
                    List<ByteString> res = response.getOutputs(i).getStringValList();
                    List<String> result = new ArrayList<String>();
                    for (int j = 0; j < res.size(); j++) {
                        result.add(res.get(j).toStringUtf8());
                    }
                    return result;
                }
            }
            log.error("Not Found output with NameInDlModel: " + nameInDlModel);
            return new ArrayList<String>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<String>();
        }
    }

    public List<String> getStringValsByBladeBlobName(String bladeBlobName) {
        if (response != null) {
            for (int i = 0; i < response.getOutputsCount(); ++i) {
                if (response.getOutputs(i).getBladeBlobName().equals(bladeBlobName)) {
                    List<ByteString> res = response.getOutputs(i).getStringValList();
                    List<String> result = new ArrayList<String>();
                    for (int j = 0; j < res.size(); j++) {
                        result.add(res.get(j).toStringUtf8());
                    }
                    return result;
                }
            }
            log.error("Not Found output with BladeBlobName: " + bladeBlobName);
            return new ArrayList<String>();
        } else {
            log.error("request failed: can't get response");
            return new ArrayList<String>();
        }
    }

}
