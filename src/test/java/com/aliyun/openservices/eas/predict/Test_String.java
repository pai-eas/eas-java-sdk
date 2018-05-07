package com.aliyun.openservices.eas.predict;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;

public class Test_String {
    public static void main(String[] args) throws Exception{
    		//启动并初始化客户端
        PredictClient client = new PredictClient(new HttpConfig());
        /*
         * 如果Internet Endpoint为http://eas-shanghai.alibaba-inc.com/api/predict/credit
         * 则client.setEndpoint("eas-shanghai.alibaba-inc.com");
         * client.setModelName("credit")
         * client.setToken需要根据服务具体生成的token设置
         */
        client.setToken("NGNmNjQyZGJiYjQwNDliOTE1NTQ0ZGM1M2FjYjdjZWRlOGE2ZjRhZg==");
        client.setEndpoint("eas-shanghai.alibaba-inc.com");
        client.setModelName("credit");

        //输入字符串定义
        String request = "[{\"money_credit\": 3000000}, {\"money_credit\": 10000}]";
        System.out.println(request);
        	
        //通过eas返回字符串
        String response = client.predict(request);
        System.out.println(response);

		//关闭客户端
        client.shutdown();
        return;
    }
}
