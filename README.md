
# 添加依赖包
编写Java客户端代码使用Maven管理项目，用户需在pom.xml文件中添加客户端所需的依赖包，名为eas-sdk，目前最新release版本为2.0.5，增加了 queue service 客户端功能，具体代码如下：

```
<dependency>
  <groupId>com.aliyun.openservices.eas</groupId>
  <artifactId>eas-sdk</artifactId>
  <version>2.0.5</version>
</dependency>
```
2.0.5 版本的 queue service 客户端功能额外添加了如下两个依赖包，为防止与用户依赖包版本冲突，用户需要自行修改至合适版本后添加：
```
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.1</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.1</version>
</dependency>
```

# Java SDK调用接口说明
| 类             | 主要接口                                                                                            | 描述                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|---------------|-------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PredictClient | PredictClient(HttpConfig httpConfig)                                                            | PredictClient类构造器                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|               | setToken(String token)                                                                          | 设置Http请求的token                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | setModelName(String modelName)                                                                  | 设置请求的在线预测服务的模型名字                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | setEndpoint(String endpoint)                                                                    | 设置请求服务的host和port，格式"host:port"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | setDirectEndpoint(String endpoint)                                                              | 设置通过高速直连通道访问服务的endpoint，如pai-eas-vpc.cn-shanghai.aliyuncs.com ,该调用方式适用于公共云上在用户vpc中通过高速直连                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | setRetryCount(boolean int retryCount)                                                           | 设置失败重试次数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | setTracing(HashMap<String, String> mapHeader)                                                   | 设置http Header是否需要返回，输入为Header字典，方法执行完后会自动写入字典                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|               | setContentType(String contentType)                                                              | 设置httpclient的content类型，默认为"application/octet-stream"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | createChildClient(String token, String endpoint, String modelname)                              | 创建子Client对象，共用父Client对象的线程池，用于多线程预测                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|               | createChildClient()                                                                             | 创建子Client对象，共用父Client对象的线程池以及设置，用于多线程预测                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | predict(TFRequest runRequest)                                                                   | 向在线预测服务提交一个Tensorflow的请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | predict(String requestContent)                                                                  | 向在线预测服务提交一个字符串请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | predict(byte[] requestContent)                                                                  | 向在线预测服务提交一个byte数组请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| HttpConfig    | setIoThreadNum(int ioThreadNum)                                                                 | 设置http请求的io线程数，默认值为2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | setReadTimeout(int readTimeout)                                                                 | 设置Socket的setSoTimeout，默认值5000，表示5s                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|               | setConnectTimeout(int connectTimeout)                                                           | 设置连接超时时间，默认值5000，表示5s                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|               | setMaxConnectionCount(int maxConnectionCount)                                                   | 设置最大连接数，默认值1000                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | setMaxConnectionPerRoute(int maxConnectionPerRoute)                                             | 设置每个路由上最大的默认连接数，默认值1000                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | setKeepAlive(boolean keepAlive)                                                                 | 设置http服务的keep-alive                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|               | getErrorCode()                                                                                  | 返回最近一次调用的状态码                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|               | getErrorMessage()                                                                               | 返回最近一次调用的状态信息                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| TFRequest     | void setSignatureName(String value)                                                             | 如果请求的在线服务的模型为Tensorflow的SavedModel格式时，设置请求模型的signatureDef的name                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | void addFetch(String value)                                                                     | 请求Tensorflow的在线服务模型时，设置需要获得的输出Tensor的别名                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | void addFeed(String inputName, TFDataType dataType, long[]shape, ?[]content)                    | 请求Tensorflow的在线预测服务模型时，设置需要输入的Tensor，inputName表示输入Tensor的别名，dataType表示输入Tensor的DataType， shape表示输入Tensor的TensorShape，content表示输入Tensor的内容（一维数组展开表示）。如果输入Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，content中的元素类型**?**为float，当DataType为DT_COMPLEX64时，content中相邻两个float元素依次表示复数的实部和虚部；如果输入Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，content中的元素类型**?**为double，当DataType为DT_COMPLEX128时，content中相邻两个double元素依次表示复数的实部和虚部。如果输入Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，content中的元素类型**?**为int；如果输入Tensor的DataType为DT_INT64，content中的元素类型**?**为long；如果输入Tensor的DataType为DT_STRING，content中的元素类型**?**为String；如果输入Tensor的DataType为DT_BOOL，content中的元素类型**?**为boolean； |
| TFResponse    | getTensorShape(String outputname)                                                               | 获得别名为ouputname的输出Tensor的TensorShape                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|               | getFloatVals(String outputname)                                                                 | 如果输出Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | getDoubleVals(String outputname)                                                                | 如果输出Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|               | getIntVals(String outputname)                                                                   | 如果输出Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | getStringVals(String outputname)                                                                | 如果输出Tensor的DataType为DT_STRING，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | getInt64Vals(String outputname)                                                                 | 如果输出Tensor的DataType为DT_INT64，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | getBoolVals(String outputname)                                                                  | 如果输出Tensor的DataType为DT_BOOL，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| QueueClient   | QueueClient(String endpoint, String queueName, String token, HttpConfig httpConfig)             | QueueClient 类的构造函数。<br />[endpoint]: 服务端的endpoint地址；<br />[queueName]:服务名字；<br />[token]:服务访问的token；<br />[httpConfig]:服务请求的配置；                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | attributes()                                                                                    | 获取队列服务的详细信息，包含如下字段：<br />meta.maxPayloadBytes	队列中允许的每个数据项的size上限；<br />meta.name	队列名；<br />stream.approxMaxLength	队列中能够存储的数据项的数量上限；<br />stream.firstEntry	队列中第一个数据项的index；<br />stream.lastEntry	队列中最后一个数据项的index；<br />stream.length	队列中当前存储的数据项的数量                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|               | put(byte[] data, Map<String, String> tags)                                                      | 将数据放入队列服务。<br />[data]: byte 数组类型；<br />[tags]: 自定义参数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|               | get(long index, long length, long timeout, boolean autoDelete, Map<String, String> tags)        | 获取队列服务中的数据。<br />[index]: 指定获取数据的起始index，如果为-1则读取最新的数据；<br />[length]: 获取的数据个数；<br />[timeout]: 超时时间，以秒为单位；<br />[autoDelete]: 获取数据后是否自动从队列中删除；<br />[tags]: 自定义参数，如指定 requestId                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | truncate(Long index)                                                                            | 删除队列服务中所有索引小于指定索引的数据                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | delete(Long index) <br /><br />delete(Long[] index)                                             | 删除队列服务中指定索引的数据                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | watch(long index, long window, boolean indexOnly, boolean autoCommit, Map<String, String> tags) | 订阅队列服务数据流。<br /><br />[index]: 指定获取数据的起始index；如果为-1则忽略所有pending的数据而读取最新数据；<br />[window]: 指定发送窗口的大小，即最大的未commit数据长度；当QS发出了window个dataframe数据但是客户端并没有commit时，发送会停止；<br />[indexOnly]: 返回的dataframe中只包含index和tags，而不返回具体数据，从而节约带宽；<br />[autoCommit]: 指定发出数据后，直接进行commit，从而避免Commit调用。当_auto_commit设置为true时，_window_指定的参数将被忽略；<br />[tags]: 自定义参数配置，可配置订阅服务的异常重试次数与重试间隔时间；设置为 null 时，则使用默认值，重试次数默认值为3，重试间隔时间默认值为5s                                                                                                                                                                                                                                                                                                                                  |
|               | commit(Long index) <br /><br />commit(Long[] index)                                             | 确认数据已被消费并在队列服务中删除该数据                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | end(boolean force)                                                                              | 终止队列服务                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

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

## QueueService 客户端程序示例
可通过 QueueService 客户端使用队列服务功能，具体demo示例如下：
```java
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.WebSocketWatcher;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class TestWatch {
  public static void main(String[] args) throws Exception {
    /** 创建队列服务客户端 */
    String queueEndpoint = "185925787991****.cn-hangzhou.pai-eas.aliyuncs.com";
    String inputQueueName = "test_group.test_qservice";
    String sinkQueueName = "test_group.test_qservice/sink";
    String queueToken = "test-token";
    QueueClient input_queue =
        new QueueClient(queueEndpoint, inputQueueName, queueToken, new HttpConfig());
    input_queue.clear();
    QueueClient sink_queue =
        new QueueClient(queueEndpoint, sinkQueueName, queueToken, new HttpConfig());
    sink_queue.clear();
    /** 往 input_queue 添加数据，推理服务会自动从 input_queue 中读取请求数据 */
    for (int i = 0; i < 10; ++i) {
      String data = Integer.toString(i);
      input_queue.put(data.getBytes(), null);
    }

    /** 自定义订阅服务的重试次数与重试间隔时间，tag 为 null 时，则为默认重试次数: 3, 默认重试间隔时间: 5s */
    Map<String, String> tag =
        new HashMap<String, String>() {
          {
            put("reconnect_count", "5");
            put("reconnect_interval", "10");
          }
        };
    /** 通过 watch 函数，订阅队列服务的数据，窗口大小为5, tag 为自定义参数 */
    WebSocketWatcher watcher = sink_queue.watch(0L, 5L, false, true, tag);

    /** tag 设置为 null 时, 订阅服务的重试次数和重试间隔时间为上述默认值 */
    //    WebSocketWatcher watcher = sink_queue.watch(0L, 5L, false, true, null);

    /** 推理服务处理输入数据后会将结果写入 sink_queue，通过 watch 函数订阅 sink_queue 查看数据结果 */
    for (int i = 0; i < 10; ++i) {
      /** getDataFrame 函数用于获取 DataFrame 类型的数据（见 get 函数的介绍），没有数据时会被阻塞 */
      byte[] data = watcher.getDataFrame().getData();
      System.out.println("[watch] data = " + new String(data));
    }
    /** 关闭已经打开的watcher对象，每个客户端实例只允许存在一个watcher对象，若watcher对象不关闭，再运行时会报错 */
    watcher.close();

    Thread.sleep(2000);
    JSONObject attrs = sink_queue.attributes();

    /** 关闭客户端 */
    input_queue.shutdown();
    sink_queue.shutdown();
  }
}
```
其中：
* 通过 QueueClient 创建队列服务客户端对象，如果创建了推理服务，需同时创建输入队列和输出队列对象；
* 使用 put() 函数向输入队列中发送数据；
* 使用 watch() 函数从输出队列中订阅数据；
* 现实场景中发送数据和订阅数据可以由不同的线程处理，本实例中为了演示方便在同一线程中完成，先put数据，后watch结果。