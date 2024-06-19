# 添加依赖包

编写Java客户端代码使用Maven管理项目，用户需在pom.xml文件中添加客户端所需的依赖包，名为eas-sdk，目前最新release版本的具体代码如下：

```
<dependency>
  <groupId>com.aliyun.openservices.eas</groupId>
  <artifactId>eas-sdk</artifactId>
  <version>2.0.17</version>
</dependency>
```

2.0.5 及以上版本增加了queue service客户端功能，支持多优先级异步队列服务，如果需要使用该功能，为避免依赖包版本冲突，用户还需自行添加如下两个依赖包，并修改这两个依赖包至合适版本：

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

| 类             | 主要接口                                                                                                                            | 描述                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|---------------|---------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PredictClient | PredictClient(HttpConfig httpConfig)                                                                                            | PredictClient类构造器                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|               | setToken(String token)                                                                                                          | 设置Http请求的token                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | setModelName(String modelName)                                                                                                  | 设置请求的在线预测服务的模型名字                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | setEndpoint(String endpoint)                                                                                                    | 设置请求服务的host和port，格式"host:port"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | setDirectEndpoint(String endpoint)                                                                                              | 设置通过高速直连通道访问服务的endpoint，如pai-eas-vpc.cn-shanghai.aliyuncs.com ,该调用方式适用于公共云上在用户vpc中通过高速直连                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | setRetryCount(int retryCount)                                                                                                   | 设置请求失败重试次数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|               | setRetryConditions(EnumSet<RetryCondition> retryConditions)                                                                     | 设置失败重试条件，默认请求错误都会重试，支持设置仅在以下错误时重试:<br />RetryCondition.CONNECTION_FAILED 请求连接失败, <br />RetryCondition.CONNECTION_TIMEOUT 请求连接超时, <br />RetryCondition.READ_TIMEOUT 等待请求返回超时, <br />RetryCondition.RESPONSE_5XX 返回状态码为5xx, <br />RetryCondition.RESPONSE_4XX 返回状态码为4xx                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|               | setTracing(HashMap<String, String> mapHeader)                                                                                   | 设置http Header是否需要返回，输入为Header字典，方法执行完后会自动写入字典                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|               | setContentType(String contentType)                                                                                              | 设置httpclient的content类型，默认为"application/octet-stream"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|               | setCompressor(Compressor compressor)                                                                                            | 设置请求数据的压缩方式，目前支持Compressor.Gzip和Compressor.Zlib                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | addExtraHeaders(Map<String, String> extraHeaders)                                                                               | 添加自定义的http Header                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|               | createChildClient(String token, String endpoint, String modelname)                                                              | 创建子Client对象，共用父Client对象的线程池，用于多线程预测                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|               | createChildClient()                                                                                                             | 创建子Client对象，共用父Client对象的线程池以及设置，用于多线程预测                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | predict(TFRequest runRequest)                                                                                                   | 向在线预测服务提交一个Tensorflow的请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | predict(String requestContent)                                                                                                  | 向在线预测服务提交一个字符串请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | predict(byte[] requestContent)                                                                                                  | 向在线预测服务提交一个byte数组请求                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| HttpConfig    | setIoThreadNum(int ioThreadNum)                                                                                                 | 设置http请求的io线程数，默认值为2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|               | setReadTimeout(int readTimeout)                                                                                                 | 设置Socket的setSoTimeout，默认值5000，表示5s                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|               | setConnectTimeout(int connectTimeout)                                                                                           | 设置连接超时时间，默认值5000，表示5s                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|               | setMaxConnectionCount(int maxConnectionCount)                                                                                   | 设置最大连接数，默认值1000                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | setMaxConnectionPerRoute(int maxConnectionPerRoute)                                                                             | 设置每个路由上最大的默认连接数，默认值1000                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | setKeepAlive(boolean keepAlive)                                                                                                 | 设置http服务的keep-alive                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|               | setRedirectsEnabled(boolean redirectsEnabled)                                                                                   | 设置http服务是否开启自动重定向，默认false                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|               | getErrorCode()                                                                                                                  | 返回最近一次调用的状态码                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | getErrorMessage()                                                                                                               | 返回最近一次调用的状态信息                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| TFRequest     | void setSignatureName(String value)                                                                                             | 如果请求的在线服务的模型为Tensorflow的SavedModel格式时，设置请求模型的signatureDef的name                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | void addFetch(String value)                                                                                                     | 请求Tensorflow的在线服务模型时，设置需要获得的输出Tensor的别名                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | void addFeed(String inputName, TFDataType dataType, long[]shape, ?[]content)                                                    | 请求Tensorflow的在线预测服务模型时，设置需要输入的Tensor，inputName表示输入Tensor的别名，dataType表示输入Tensor的DataType， shape表示输入Tensor的TensorShape，content表示输入Tensor的内容（一维数组展开表示）。如果输入Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，content中的元素类型?为float，当DataType为DT_COMPLEX64时，content中相邻两个float元素依次表示复数的实部和虚部；如果输入Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，content中的元素类型?为double，当DataType为DT_COMPLEX128时，content中相邻两个double元素依次表示复数的实部和虚部。如果输入Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，content中的元素类型?为int；如果输入Tensor的DataType为DT_INT64，content中的元素类型?为long；如果输入Tensor的DataType为DT_STRING，content中的元素类型?为String；如果输入Tensor的DataType为DT_BOOL，content中的元素类型?为boolean； |
| TFResponse    | getTensorShape(String outputname)                                                                                               | 获得别名为ouputname的输出Tensor的TensorShape                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|               | getFloatVals(String outputname)                                                                                                 | 如果输出Tensor的DataType为DT_FLOAT，DT_COMPLEX64，DT_BFLOAT16和DT_HALF，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | getDoubleVals(String outputname)                                                                                                | 如果输出Tensor的DataType为DT_DOUBLE和DT_COMPLEX128，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|               | getIntVals(String outputname)                                                                                                   | 如果输出Tensor的DataType为DT_INT32，DT_UINT8，DT_INT16，DT_INT8，DT_QINT8，DT_QUINT8，DT_QINT32，DT_QINT16，DT_QUINT16和DT_UINT16，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|               | getStringVals(String outputname)                                                                                                | 如果输出Tensor的DataType为DT_STRING，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | getInt64Vals(String outputname)                                                                                                 | 如果输出Tensor的DataType为DT_INT64，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | getBoolVals(String outputname)                                                                                                  | 如果输出Tensor的DataType为DT_BOOL，调用该函数获得名字为ouputname的输出Tensor的data                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| QueueClient   | QueueClient(String endpoint, String queueName, String token, HttpConfig httpConfig, QueueUser user)                             | 功能：QueueClient 类的构造函数。<br /><br />参数：<br />[endpoint]: 服务端的endpoint地址；<br />[queueName]:服务名字；<br />[token]:服务访问的token；<br />[httpConfig]:服务请求的配置；<br />[user]: 配置 UserId（默认为随机数）与 GroupName（默认为"eas"）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|               | JSONObject attributes()                                                                                                         | 功能：获取队列服务的详细信息。<br /><br />返回值：JSONObject类型的队列服务信息，主要包含如下字段：<br />meta.maxPayloadBytes 队列中允许的每个数据项的size上限；<br />meta.name 队列名；<br />stream.approxMaxLength 队列中能够存储的数据项的数量上限；<br />stream.firstEntry 队列中第一个数据项的index；<br />stream.lastEntry 队列中最后一个数据项的index；<br />stream.length 队列中当前存储的数据项的数量                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | long count(long priority, Map<String, String> tags)                                                                             | 功能：查询指定tag或优先级的数据个数。<br /><br />参数：<br />[priority]: 数据优先级。<br />[tags]: 自定义参数；<br /><br />返回值：查询的数据个数                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | JSONObject search(long index)                                                                                                   | 功能：查询数据的排队信息。<br /><br />参数：<br />[index]: 查询数据的index。<br /><br />返回值：JSONObject类型的数据排队信息，包含如下字段：<br /> IsPending：表示数据是否正在被处理，True表示正在被处理，False表示正在排队；<br /> WaitCount：表示前面还需排队等待的数据个数，仅IsPending为False时该值才有效，IsPending为True时该值为0；<br />                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|               | Pair<Long, String> put(byte[] data, long priority, Map<String, String> tags)                                                    | 功能：将数据写入队列服务。<br /><br />参数：<br />[data]: byte[] 数据 ；<br />[priority]: 表示数据优先级。默认值为0，表示非优先数据。将该参数配置为1时，表示高优先级数据；<br />[tags]: 自定义参数；<br /><br />返回值：Pair<Long, String> 类型的 <数据index, 请求Id>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|               | DataFrame[] get(long index, long length, long timeout, boolean autoDelete, Map<String, String> tags)                            | 功能：获取队列服务中的数据。<br /><br />参数：<br />[index]: 指定获取数据的起始index，如果为-1则读取最新的数据；<br />[length]: 获取的数据个数；<br />[timeout]: 超时时间，以秒为单位；<br />[autoDelete]: 获取数据后是否自动从队列中删除；<br />[tags]: 自定义参数，如指定 requestId <br /><br />返回值：DataFrame 数据类                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | void truncate(Long index)                                                                                                       | 功能：删除队列服务中所有index小于指定index的数据。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|               | String delete(Long index) <br /><br />String delete(Long[] index)                                                               | 功能：删除队列服务中指定index的数据；<br /><br />返回值：删除成功则返回"OK"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|               | WebSocketWatcher watch(long index, long window, boolean indexOnly, boolean autoCommit, Map<String, String> tags)                | 功能：订阅队列服务。<br /><br />参数：<br />[index]: 指定获取数据的起始index；如果为-1则忽略所有pending的数据而读取最新数据；<br />[window]: 指定发送窗口的大小，即最大的未commit数据长度；当QS发出了window个dataframe数据但是客户端并没有commit时，发送会停止；<br />[indexOnly]: 返回的dataframe中只包含index和tags，而不返回具体数据，从而节约带宽；<br />[autoCommit]: 指定发出数据后，直接进行commit，从而避免Commit调用。当_auto_commit设置为true时，_window_指定的参数将被忽略；<br />[tags]: 自定义订阅请求参数；<br /><br />返回值: WebSocketWatcher 类型，用于获取订阅数据；<br />使用方法可参考"QueueService 客户端程序示例"                                                                                                                                                                                                                                                                            |
|               | String commit(long index) <br /><br />String commit(long[] index)                                                               | 功能：确认数据已被消费并在队列服务中删除该数据；<br /><br />返回值：commit成功则返回"OK"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|               | String negative(long index, String code, String reason) <br /><br />String negative(long[] indexes, String code, String reason) | 功能：否定提交该数据；错误Code的设置详见文档：https://help.aliyun.com/zh/pai/user-guide/queue-service-subscription-push?spm=a2c4g.11186623.0.0.4ca721afPjhmQH#fcbcad2003pdm <br /><br />返回值：negative commit成功则返回"OK"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|               | void end(boolean force)                                                                                                         | 功能：关闭队列服务                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| DataFrame     | byte[] getData()                                                                                                                | 功能：获取数据值；<br /><br />返回值：byte[] 类型的数据值                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|               | long getIndex()                                                                                                                 | 功能：获取数据index；<br /><br />返回值：long 类型的数据index                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|               | Map<String, String> getTags()                                                                                                   | 功能：获取数据Tags; <br /><br />返回值：Map<String, String> 类型的数据Tags，可用于获取"requestId"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

# 程序示例

## 字符串输入输出程序示例

对于自定义Processor用户而言，通常采用字符串进行服务的输入输出调用(如pmml模型服务的调用)，具体的demo程序如下：

```java
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;

public class Test_String {
    public static void main(String[] args) throws Exception {
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
        } catch (Exception e) {
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
            content[i] = (float) 0.0;
        request.addFeed("images", TFDataType.DT_FLOAT, new long[]{1, 784}, content);
        request.addFetch("scores");
        return request;
    }

    public static void main(String[] args) throws Exception {
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
                    if (j != result.size() - 1)
                        System.out.print(", ");
                }
                System.out.print("]\n");
            } catch (Exception e) {
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

可通过 QueueClient 使用队列服务功能，具体demo示例如下：

```java
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.QueueUser;
import com.aliyun.openservices.eas.predict.queue_client.WatchConfig;
import com.aliyun.openservices.eas.predict.queue_client.WebSocketWatcher;

import java.util.HashMap;
import java.util.Map;

public class TestWatch {
    public static void main(String[] args) throws Exception {
        /** 创建队列服务客户端 */
        String queueEndpoint = "18*******.cn-hangzhou.pai-eas.aliyuncs.com";
        String inputQueueName = "test_qservice";
        String sinkQueueName = "test_qservice/sink";
        String queueToken = "test-token";

        /** 输入队列，往输入队列添加数据，推理服务会自动从输入队列中读取请求数据 */
        QueueClient input_queue =
            new QueueClient(queueEndpoint, inputQueueName, queueToken, new HttpConfig(), new QueueUser());
        /** 清除队列数据!!! 请谨慎使用 */
        input_queue.clear();
        /** 输出队列，推理服务处理输入数据后会将结果写入输出队列*/
        QueueClient sink_queue =
            new QueueClient(queueEndpoint, sinkQueueName, queueToken, new HttpConfig(), new QueueUser());
        sink_queue.clear();

        /** 往输入队列添加数据*/
        for (int i = 0; i < 10; ++i) {
            String data = Integer.toString(i);
            input_queue.put(data.getBytes(), null);
            /** 队列服务支持多优先级队列，可通过 put函数设置数据优先级，默认优先级为 0 */
            //  inputQueue.put(data.getBytes(), 0L, null);
        }

        /** 通过 watch 函数订阅输出队列的数据，窗口大小为5 */
        WebSocketWatcher watcher = sink_queue.watch(0L, 5L, false, true, null);
        /** WatchConfig参数可自定义重试次数、重试间隔（单位为秒）、是否无限重试；未配置WatchConfig 则默认重试次数:3，重试间隔:5 */
        //  WebSocketWatcher watcher = sink_queue.watch(0L, 5L, false, true, null, new WatchConfig(5, 10));
        //  WebSocketWatcher watcher = sink_queue.watch(0L, 5L, false, true, null, new WatchConfig(true, 10));

        /** 获取输出数据 */
        for (int i = 0; i < 10; ++i) {
            try {
                /** getDataFrame 函数用于获取 DataFrame 数据类，没有数据时会被阻塞 */
                byte[] data = watcher.getDataFrame().getData();
                System.out.println("[watch] data = " + new String(data));
            } catch (RuntimeException ex) {
                System.out.println("[watch] error = " + ex.getMessage());
            }
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


## 请求数据压缩
对于请求数据量较大的情况，EAS支持将数据压缩之后再发送至服务端，该功能需要在服务配置中指定相应的rpc.decompressor才能生效，目前支持的压缩方式有: zlib, gzip, snappy, zstd, lz4；
服务配置如下所示：
```json
"metadata": {
  "rpc": {
    "decompressor": "lz4"
  }
}
```

sdk代码示例如下：
```java
package com.aliyun.openservices.eas.predict;
import com.aliyun.openservices.eas.predict.http.Compressor;
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
public class Test_String {
    public static void main(String[] args) throws Exception {
        //启动并初始化客户端
        PredictClient client = new PredictClient(new HttpConfig());
        client.setEndpoint("eas-shanghai.alibaba-inc.com");
        client.setModelName("echo_compress");
        client.setToken("YzZjZjQwN2E4NGRkMDMxNDk5NzhhZDcwZDBjOTZjOGYwZDYxZGM2Mg==");
        client.setCompressor(Compressor.Zlib);  // 或者 Compressor.Gzip
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
```
该功能适用于任何请求形式，只需注意客户端和服务端所设置的压缩方式相同即可。
