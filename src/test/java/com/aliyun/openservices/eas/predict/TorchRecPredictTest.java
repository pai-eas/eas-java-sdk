package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.Compressor;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.proto.TorchRecPredictProtos;
import com.aliyun.openservices.eas.predict.request.TorchRecRequest;
import com.aliyun.openservices.eas.predict.proto.TorchPredictProtos.ArrayProto;

import java.util.*;


public class TorchRecPredictTest {
    public static PredictClient InitClient() {
        return new PredictClient(new HttpConfig());
    }

    public static TorchRecRequest buildPredictRequest() {
        TorchRecRequest TorchRecRequest = new TorchRecRequest();
        TorchRecRequest.appendItemId("7033");

        TorchRecRequest.addUserFeature("user_id", 33981,"int");

        ArrayList<Double> list = new ArrayList<>();
        list.add(0.24689289764507472);
        list.add(0.005758482924454689);
        list.add(0.6765301324940026);
        list.add(0.18137273055602343);
        TorchRecRequest.addUserFeature("raw_3", list,"List<double>");

        Map<Integer,Integer> myMap =new LinkedHashMap<>();
        myMap.put(866, 4143);
        myMap.put(1627, 2451);
        TorchRecRequest.addUserFeature("map_1", myMap,"map<int,int>");

        ArrayList<ArrayList<Float>> list2 = new ArrayList<>();
        ArrayList<Float> innerList1 = new ArrayList<>();
        innerList1.add(1.1f);
        innerList1.add(2.2f);
        innerList1.add(3.3f);
        list2.add(innerList1);
        ArrayList<Float> innerList2 = new ArrayList<>();
        innerList2.add(4.4f);
        innerList2.add(5.5f);
        list2.add(innerList2);
        TorchRecRequest.addUserFeature("click", list2,"list<list<float>>");

        TorchRecRequest.addContextFeature("id_2", list,"List<double>");
        TorchRecRequest.addContextFeature("id_2", list,"List<double>");

        System.out.println(TorchRecRequest.request);
        return TorchRecRequest;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = InitClient();
        client.setToken("tokenGeneratedFromService");
        client.setEndpoint("cn-beijing.pai-eas.aliyuncs.com");
        client.setModelName("test_torch_rec_multi_tower_din_gpu");
        client.setRequestTimeout(100000);


        testInvoke(client);
        testDebugLevel(client);
        client.shutdown();
    }

    public static void testInvoke(PredictClient client) throws Exception {
        long startTime = System.currentTimeMillis();
        TorchRecPredictProtos.PBResponse response = client.predict(buildPredictRequest());
        for (Map.Entry<String, ArrayProto> entry : response.getMapOutputsMap().entrySet()) {

            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");

    }

    public static void testDebugLevel(PredictClient client) throws Exception {
        long startTime = System.currentTimeMillis();
        TorchRecRequest request = buildPredictRequest();
        request.setDebugLevel(1);
        TorchRecPredictProtos.PBResponse response = client.predict(request);
        Map<String, String> genFeas = response.getGenerateFeaturesMap();
        for(String itemId: genFeas.keySet()) {
            System.out.println(itemId);
            System.out.println(genFeas.get(itemId));
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");

    }
}
