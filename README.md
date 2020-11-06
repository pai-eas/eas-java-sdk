
# 添加依赖包
编写Java客户端代码使用Maven管理项目，用户需在pom.xml文件中添加客户端所需的依赖包，名为eas-sdk，目前最新release版本为2.0.3，具体代码如下：

```
<dependency>
  <groupId>com.aliyun.openservices.eas</groupId>
  <artifactId>eas-sdk</artifactId>
  <version>2.0.3</version>
</dependency>
```

# Java SDK调用接口说明
|类|主要接口|描述|
|-----|------|------|
|PredictClient|PredictClient(HttpConfig httpConfig)|PredictClient类构造器|
||setToken(String token)|设置Http请求的token|
||setModelName(String modelName)|设置请求的在线预测服务的模型名字|
||setEndpoint(String endpoint)|设置请求服务的host和port，格式"host:port"|
||setDirectEndpoint(String endpoint)|设置通过高速直连通道访问服务的endpoint，如pai-eas-vpc.cn-shanghai.aliyuncs.com ,该调用方式适用于公共云上在用户vpc中通过高速直连 |
||setRetryCount(boolean int retryCount)|设置失败重试次数|
||setTracing(HashMap<String, String> mapHeader)|设置http Header是否需要返回，输入为Header字典，方法执行完后会自动写入字典|
||setContentType(String contentType)|设置httpclient的content类型，默认为"application/octet-stream"|
||createChildClient(String token, String endpoint, String modelname)|创建子Client对象，共用父Client对象的线程池，用于多线程预测|
||createChildClient()|创建子Client对象，共用父Client对象的线程池以及设置，用于多线程预测|
||predict(TFRequest runRequest)|向在线预测服务提交一个Tensorflow的请求|
||predict(String requestContent)|向在线预测服务提交一个字符串请求|
||predict(byte[] requestContent)|向在线预测服务提交一个byte数组请求|
|HttpConfig|setIoThreadNum(int ioThreadNum)|设置http请求的io线程数，默认值为2|
||setReadTimeout(int readTimeout)|设置Socket的setSoTimeout，默认值5000，表示5s|
||setConnectTimeout(int connectTimeout)|设置连接超时时间，默认值5000，表示5s|
||setMaxConnectionCount(int maxConnectionCount)|设置最大连接数，默认值1000|
||setMaxConnectionPerRoute(int maxConnectionPerRoute)|设置每个路由上最大的默认连接数，默认值1000|
||setKeepAlive(boolean keepAlive)|设置http服务的keep-alive|
||getErrorCode()|返回最近一次调用的状态码|
||getErrorMessage()|返回最近一次调用的状态信息|
|TFRequest|void setSignatureName(String value)|如果请求的在线服务的模型为Tensorflow的SavedModel格式时，设置请求模型的signatureDef的name|
||void addFetch(String value)|请求Tensorflow的在线服务模型时，设置需要获得的输出Tensor的别名|
||void addFeed(String inputName, TFDataType dataType, long[]shape, ?[]content)|请求Tensorflow的在线预测服务模型时，设置需要输入的Tensor，inputName表示输入Tensor的别名，dataType表示输入Tensor的DataType， shape表示输入Tensor的TensorShape，content表示输入Tensor的内容（一维数组展开表示）。如果输入Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，content中的元素类型**?**为float，当DataType为DT_COMPLEX64时，content中相邻两个float元素依次表示复数的实部和虚部；如果输入Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，content中的元素类型**?**为double，当DataType为DT_COMPLEX128时，content中相邻两个double元素依次表示复数的实部和虚部。如果输入Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，content中的元素类型**?**为int；如果输入Tensor的DataType为DT_INT64，content中的元素类型**?**为long；如果输入Tensor的DataType为DT_STRING，content中的元素类型**?**为String；如果输入Tensor的DataType为DT_BOOL，content中的元素类型**?**为boolean；|
|TFResponse|getTensorShape(String outputname)|获得别名为ouputname的输出Tensor的TensorShape|
||getFloatVals(String outputname)|如果输出Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，调用该函数获得名字为ouputname的输出Tensor的data|
||getDoubleVals(String outputname)|如果输出Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，调用该函数获得名字为ouputname的输出Tensor的data|
||getIntVals(String outputname)|如果输出Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，调用该函数获得名字为ouputname的输出Tensor的data|
||getStringVals(String outputname)|如果输出Tensor的DataType为DT_STRING，调用该函数获得名字为ouputname的输出Tensor的data|
||getInt64Vals(String outputname)|如果输出Tensor的DataType为DT_INT64，调用该函数获得名字为ouputname的输出Tensor的data|
||getBoolVals(String outputname)|如果输出Tensor的DataType为DT_BOOL，调用该函数获得名字为ouputname的输出Tensor的data|

# 程序示例

## 字符串输入输出程序示例

对于自定义Processor用户而言，通常采用字符串进行服务的输入输出调用(如pmml模型服务的调用)，具体的demo程序如下：

```java
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;

public class Test_String {
    public static void main(String[] args) throws Exception{
	// 启动并初始化客户端, client对象需要共享，千万不可每个请求都创建一个client对象。
        PredictClient client = new PredictClient(new HttpConfig());
        client.setToken("YWFlMDYyZDNmNTc3M2I3MzMwYmY0MmYwM2Y2MTYxMTY4NzBkNzdjOQ==");                         
        // 如果要使用网络直连功能，需使用setDirectEndpoint方法
        // 如 client.setDirectEndpoint("pai-eas-vpc.cn-shanghai.aliyuncs.com");
        // 网络直连需打通在EAS控制台开通，提供用于访问EAS服务的源vswitch，打通后可绕过网关以软负载的方式直接访问服务的实例，以实现更好的稳定性和性能
	// 注：普通网关访问时请使用以用户uid为开头的endpoint，在eas控制台服务的调用信息中可查到。直连访问时请使用如上的pai-eas-vpc.{region_id}.aliyuncs.com的域名进行访问。
        client.setEndpoint("1828488879222746.vpc.cn-shanghai.pai-eas.aliyuncs.com");
        client.setModelName("scorecard_pmml_example");

        //输入字符串定义
        String request = "[{\"money_credit\": 3000000}, {\"money_credit\": 10000}]";
        System.out.println(request);

        //通过eas返回字符串
        try {
        	String response = client.predict(request);
        	System.out.println(response);
        } catch(Exception e) {
        	e.printStackTrace();
        }

        //关闭客户端
        client.shutdown();
        return;
    }
}
```

其中：
* 通过PredictClient创建客户端服务对象，如需在程序中使用多个服务，可创建多个client对象；
* 在建立了PredictClient对象之后，需为其设置Token、Endpoint以及ModelName；
* 采用String类型的request作为输入，通过client.predict发送http请求，并返回response；
* shuntdown方法关闭客户端client；


## Tensorflow输入输出程序示例

TF用户可以使用TFRequest与TFResponse作为数据的输入输出格式，具体demo示例如下：

```java
import java.util.List;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.request.TFDataType;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.TFResponse;

public class Test_TF {
    public static TFRequest buildPredictRequest() {
        TFRequest request = new TFRequest();
        request.setSignatureName("predict_images");
        float[] content = new float[784];
        for (int i = 0; i < content.length; i++)
            content[i] = (float)0.0;
        request.addFeed("images", TFDataType.DT_FLOAT, new long[]{1, 784}, content);
        request.addFetch("scores");
        return request;
    }

    public static void main(String[] args) throws Exception{
        PredictClient client = new PredictClient(new HttpConfig());
        
        // 如果要使用网络直连功能，需使用setDirectEndpoint方法
        // 如 client.setDirectEndpoint("pai-eas-vpc.cn-shanghai.aliyuncs.com");
        // 网络直连需打通在EAS控制台开通，提供用于访问EAS服务的源vswitch，打通后可绕过网关以软负载的方式直接访问服务的实例，以实现更好的稳定性和性能
	// 注：普通网关访问时请使用以用户uid为开头的endpoint，在eas控制台服务的调用信息中可查到。直连访问时请使用如上的pai-eas-vpc.{region_id}.aliyuncs.com的域名进行访问。
        client.setEndpoint("1828488879222746.vpc.cn-shanghai.pai-eas.aliyuncs.com");
        client.setModelName("mnist_saved_model_example");
        client.setToken("YTg2ZjE0ZjM4ZmE3OTc0NzYxZDMyNmYzMTJjZTQ1YmU0N2FjMTAyMA==");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
		    try {
            	TFResponse response = client.predict(buildPredictRequest());
            	List<Float> result = response.getFloatVals("scores");
            	System.out.print("Predict Result: [");
            	for (int j = 0; j < result.size(); j++) {
                	System.out.print(result.get(j).floatValue());
                	if (j != result.size() -1)
                    	System.out.print(", ");
            	}
            	System.out.print("]\n");
			} catch(Exception e) {
				e.printStackTrace();
			}
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Spend Time: " + (endTime - startTime) + "ms");
        client.shutdown();
    }
}
```
其中：
* 通过PredictClient创建客户端服务对象，如需在程序中使用多个服务，可创建多个client对象；
* 在建立了PredictClient对象之后，需为其设置Token、Endpoint以及ModelName；
* 输入输出格式采用TFRequest类与TFResponse类进行封装，详细方法说明请参见上面的接口说明；
* shuntdown方法关闭客户端client；
