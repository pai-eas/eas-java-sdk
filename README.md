# 添加依赖包

编写Java客户端代码使用Maven管理项目，用户需在pom.xml文件中添加客户端所需的依赖包，名为eas-sdk，目前最新release版本的具体代码如下：

```
<dependency>
  <groupId>com.aliyun.openservices.eas</groupId>
  <artifactId>eas-sdk</artifactId>
  <version>3.0.0</version>
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

# 3.0 升级说明（HttpClient 5）

HTTP 底层由 Apache HttpClient 4.x（已 EOL）升级为 Apache HttpClient 5.3.1，核心公开 API 保持兼容。

# Java SDK调用接口说明

按服务类型分为两部分：同步服务调用（PredictClient，含 HttpConfig 连接配置、CallConfig 单次调用配置与 TFRequest/TFResponse 请求响应类）与异步服务调用（QueueClient 队列服务，含队列辅助类）。描述中的"示例"链接可直接跳转到《功能与接口示例》中的对应章节。

## 同步服务调用（PredictClient）

| 类 | 主要接口 | 描述 |
|----|----------|------|
| PredictClient | PredictClient()<br />PredictClient(HttpConfig httpConfig) | 构造器（SDK 自建并管理 HTTP client）[示例 1](#1-创建客户端与同步调用) |
|              | PredictClient(HttpConfig httpConfig, CloseableHttpAsyncClient httpClient) | 注入外部构建的 HttpClient 5 客户端（SDK 不启动、不关闭；连接类配置以外部 client 为准）[示例 16](#16-使用外部-httpclient-5) |
|              | setHttpClient(CloseableHttpAsyncClient httpClient) | 运行时替换为外部 HttpClient 5 客户端（契约同上）[示例 16](#16-使用外部-httpclient-5) |
|              | setToken(String token) | 设置 Http 请求的 token [示例 1](#1-创建客户端与同步调用) |
|              | setEndpoint(String endpoint) | 设置请求服务的 endpoint，格式 "host:port" 或网关域名 [示例 1](#1-创建客户端与同步调用) |
|              | setDirectEndpoint(String endpoint) | 设置高速直连通道 endpoint（如 pai-eas-vpc.cn-shanghai.aliyuncs.com），适用于公共云上在用户 VPC 中通过高速直连访问服务 [示例 1](#1-创建客户端与同步调用) |
|              | setServiceName(String serviceName)<br />getServiceName() | 设置/获取请求的在线预测服务名，即请求路径 "/api/predict/{服务名}" 中的服务名 [示例 1](#1-创建客户端与同步调用) |
|              | setRequestPath(String requestPath) | 设置请求路径，追加在 "/api/predict/{服务名}" 之后，适用于服务暴露自定义 path 的场景；也可在 CallConfig 中按次覆盖 [示例 1](#1-创建客户端与同步调用)、[示例 2](#2-单次调用覆盖路径查询参数与请求头callconfig) |
|              | setUrl(String url) | 直接设置完整请求 URL（适用于已经拼好完整路径的场景） |
|              | setRetryCount(int retryCount) | 设置请求失败重试次数，默认 3 [示例 3](#3-错误处理与失败重试) |
|              | setRetryConditions(EnumSet&lt;RetryCondition&gt; retryConditions) | 设置失败重试条件，默认所有错误都重试，支持设置为仅在以下错误时重试：CONNECTION_FAILED（连接失败）、CONNECTION_TIMEOUT（连接超时）、READ_TIMEOUT（读超时）、RESPONSE_5XX（5xx）、RESPONSE_4XX（4xx）[示例 3](#3-错误处理与失败重试) |
|              | setRequestTimeout(int requestTimeout) | 设置 SDK 层单次调用等待上限（毫秒），0 表示不限制；超时抛出 TimeoutException。建议在构造客户端前通过 HttpConfig 设置，未显式设置 readTimeout 时 SDK 会自动推导读超时 [示例 4](#4-超时控制) |
|              | setContentType(String contentType) | 设置请求的 Content-Type，默认 "application/octet-stream" |
|              | setIsCompressed(boolean isCompressed) | 是否压缩请求数据（遗留接口，建议改用 setCompressor）[示例 17](#17-请求数据压缩) |
|              | setCompressor(Compressor compressor)<br />setDecompressor(Compressor decompressor) | 设置请求数据压缩/响应解压方式，支持 Gzip、Zlib、Snappy、LZ4、LZ4Frame、Zstd；解压还支持 Auto（按 Content-Encoding 自动识别）[示例 17](#17-请求数据压缩) |
|              | addExtraHeaders(Map&lt;String, String&gt; extraHeaders) | 添加自定义 Http Header，对客户端所有请求生效 |
|              | createChildClient(String token, String endpoint, String serviceName)<br />createChildClient() | 创建子 Client 对象，共用父 Client 的线程池与设置，用于多线程预测 [示例 14](#14-子客户端多线程共享连接池) |
|              | predict(...) | **同步调用**：向在线预测服务提交请求并阻塞等待结果。支持 TFRequest、BladeRequest、CaffeRequest、JsonRequest、TorchRequest、EasyRecRequest、TorchRecRequest、String、byte[] 入参，且每种均有带 CallConfig 的重载 [示例 1](#1-创建客户端与同步调用)、[示例 2](#2-单次调用覆盖路径查询参数与请求头callconfig) |
|              | predictAsync(...) | **异步回调调用**（同步服务的非阻塞版本）：立即返回 CompletableFuture，不阻塞调用线程。支持 String、byte[]、TFRequest、BladeRequest、CaffeRequest、JsonRequest、TorchRequest 入参 [示例 5](#5-异步回调predictasync) |
|              | getErrorCode()<br />getErrorMessage() | 返回最近一次调用的状态码/状态信息（成功为 200，网络等异常为 -1）[示例 3](#3-错误处理与失败重试) |
|              | shutdown() | 关闭客户端（SDK 自建的 HTTP client 会被关闭；外部注入的不会被关闭）[示例 15](#15-关闭客户端) |
| CallConfig   | setRequestPath(String requestPath) | 单次调用级配置：覆盖请求路径（优先于 client 级 setRequestPath）[示例 2](#2-单次调用覆盖路径查询参数与请求头callconfig) |
|              | setQueryParams(Map&lt;String, String&gt; queryParams) | 单次调用级配置：追加查询参数 [示例 2](#2-单次调用覆盖路径查询参数与请求头callconfig) |
|              | setHeaders(Map&lt;String, String&gt; headers) | 单次调用级配置：自定义请求 Header，最后应用、覆盖 client 级 addExtraHeaders [示例 2](#2-单次调用覆盖路径查询参数与请求头callconfig) |
| HttpConfig   | setIoThreadNum(int ioThreadNum) | 设置 http 请求的 io 线程数，默认 10 |
|              | setConnectTimeout(int connectTimeout) | 设置连接超时时间（毫秒），默认 5000 [示例 1](#1-创建客户端与同步调用)、[示例 4](#4-超时控制) |
|              | setReadTimeout(int readTimeout) | 设置 Socket 读超时（毫秒），默认 5000。未显式设置且 requestTimeout &gt; 0 时，SDK 自动取 max(readTimeout, requestTimeout + connectTimeout)，保证读超时不早于请求总超时；显式设置后以该值为准，若不大于 requestTimeout 会打印 WARN。注意：QueueClient.get 长轮询时必须大于轮询超时（见[示例 12](#12-场景二请求与-get-循环)）[示例 1](#1-创建客户端与同步调用)、[示例 4](#4-超时控制) |
|              | setRequestTimeout(int requestTimeout) | 设置 SDK 层单次调用等待上限（毫秒），默认 0（不限制）；未显式设置 readTimeout 时，SDK 自动推导读超时 [示例 4](#4-超时控制) |
|              | setMaxConnectionCount(int maxConnectionCount)<br />setMaxConnectionPerRoute(int maxConnectionPerRoute) | 设置最大连接数/每个路由上最大的连接数，默认均为 1000 |
|              | setKeepAlive(boolean keepAlive)<br />setRedirectsEnabled(boolean redirectsEnabled) | 设置是否开启 http keep-alive / 自动重定向（重定向默认 false） |
|              | setIdleConnectionTimeout(int idleConnectionTimeout)<br />setConnTimeToLive(long connTimeToLive)<br />setConnectionCleanupInterval(int connectionCleanupInterval) | 连接空闲多久（毫秒）可被清理 / 连接最长存活时间（毫秒） / 后台清理任务执行周期（毫秒） |
|              | setHttpTracingEnabled(boolean httpTracingEnabled) | 开启 HTTP Tracing，默认 false。开启后每次同步 predict 调用（含每次重试）自动携带 Client-Timestamp 请求头，并在服务端返回 tracing 头时输出一条服务端分段耗时日志，**必须在构造客户端之前设置**；predictAsync 不参与 tracing [示例 18](#18-http-tracing-耗时诊断) |
| TFRequest    | void setSignatureName(String value) | 模型为 Tensorflow SavedModel 格式时，设置 signatureDef 的 name |
|              | void addFetch(String value) | 设置需要获得的输出 Tensor 的别名 |
|              | void addFeed(String inputName, TFDataType dataType, long[] shape, ?[] content) | 设置输入 Tensor：inputName 为输入别名，dataType 为类型，shape 为形状，content 为内容（一维数组展开）。float 对应 DT_FLOAT/DT_COMPLEX64/DT_BFLOAT16/DT_HALF（复数相邻两元素依次为实部/虚部）；double 对应 DT_DOUBLE/DT_COMPLEX128；int 对应 DT_INT32/DT_UINT8/DT_INT16/DT_INT8/DT_QINT8/DT_QUINT8/DT_QINT32/DT_QINT16/DT_QUINT16/DT_UINT16；long 对应 DT_INT64；String 对应 DT_STRING；boolean 对应 DT_BOOL。完整程序见[示例 7](#7-完整程序tensorflow输入输出) |
| TFResponse   | getTensorShape(String outputname) | 获得输出 Tensor 的 TensorShape |
|              | getFloatVals(String outputname) | 输出为 DT_FLOAT/DT_COMPLEX64/DT_BFLOAT16/DT_HALF 时获取数据 |
|              | getDoubleVals(String outputname) | 输出为 DT_DOUBLE/DT_COMPLEX128 时获取数据 |
|              | getIntVals(String outputname) | 输出为 DT_INT32 等整型时获取数据 |
|              | getInt64Vals(String outputname) | 输出为 DT_INT64 时获取数据 |
|              | getStringVals(String outputname) | 输出为 DT_STRING 时获取数据 |
|              | getBoolVals(String outputname) | 输出为 DT_BOOL 时获取数据 |

> 注：PredictClient 与 QueueClient 共同继承公共基类 AbstractEasClient，setRetryCount / setRequestPath / addExtraHeaders / setHttpClient / shutdown 为两者共享的通用能力（各自的链式调用仍返回自身类型）。

## 异步服务调用（QueueClient 队列服务）

> 异步服务的创建方式、队列语义与使用限制参见[官方文档：队列服务与异步推理](https://help.aliyun.com/zh/pai/queue-service-and-asynchronous-inference?spm=a2c4g.11186623.0.0.180d59a5Cnp1oO)。

| 类 | 主要接口 | 描述 |
|----|----------|------|
| QueueClient | QueueClient(String endpoint, String queueName, String token, HttpConfig httpConfig, QueueUser user)<br />QueueClient(..., CloseableHttpAsyncClient httpClient) | 构造器。[endpoint]: 服务端 endpoint；[queueName]: 队列名（异步服务的输出队列为 "服务名/sink"）；[token]: 访问 token；[httpConfig]: 请求配置；[user]: 配置 UserId（默认随机数）与 GroupName（默认 "eas"）。带 httpClient 的重载注入外部 HttpClient 5 客户端（SDK 不启动、不关闭）[示例 8](#8-创建队列客户端含自定义-path) |
|              | QueueClient(String url, String token, HttpConfig httpConfig, QueueUser user)<br />QueueClient(..., CloseableHttpAsyncClient httpClient) | 直接传入完整 URL（如 "https://host:port/api/predict/队列名"）；省略 scheme 时默认使用 HTTPS [示例 8](#8-创建队列客户端含自定义-path) |
|              | setRequestPath(String requestPath) | 设置队列服务的自定义请求路径，追加在 "/api/predict/{队列名}" 之后，对客户端全部接口生效；put 时可在 CallConfig 中按次覆盖 [示例 8](#8-创建队列客户端含自定义-path) |
|              | JSONObject attributes() | 获取队列服务的详细信息，主要字段：meta.maxPayloadBytes（单条数据 size 上限）、meta.name（队列名）、stream.approxMaxLength（数据条数上限）、stream.firstEntry / stream.lastEntry（首/末数据 index）、stream.length（当前数据条数）[示例 9](#9-队列信息查询) |
|              | long count(Map&lt;String, String&gt; tags)<br />long count(long priority, Map&lt;String, String&gt; tags) | 查询未消费数据的个数，可按自定义 tag 或优先级过滤 [示例 9](#9-队列信息查询) |
|              | JSONObject search(long index) | 查询数据的排队信息：IsPending（是否正被处理）、WaitCount（前方还有多少数据在排队，仅 IsPending 为 False 时有效）[示例 9](#9-队列信息查询) |
|              | Pair&lt;Long, String&gt; put(byte[] data, long priority, Map&lt;String, String&gt; tags)<br />put(String data, long priority, Map&lt;String, String&gt; tags)<br />put(byte[] data, Map&lt;String, String&gt; tags)<br />put(String data, Map&lt;String, String&gt; tags) | 将数据写入队列。[priority]: 优先级，0 为普通，1 为高优先级（高优先级优先被消费）；[tags]: 自定义查询参数。返回 &lt;输入队列数据 index, 服务端生成的 requestId&gt;。requestId 由服务端通过响应头 `X-Eas-Queueservice-Request-Id` 返回，SDK 解析后放在 `Pair.getRight()` 中 [示例 10](#10-写入请求put) |
|              | put(byte[] data, long priority, Map&lt;String, String&gt; tags, CallConfig callConfig)<br />put(String data, long priority, Map&lt;String, String&gt; tags, CallConfig callConfig) | put 的 CallConfig 重载：请求级路径、追加查询参数与自定义 Header（复用 CallConfig 的三个配置项）[示例 10](#10-写入请求put) |
|              | DataFrame[] get(long index, long length, int timeout, boolean autoDelete, Map&lt;String, String&gt; tags) | 获取队列数据。[index]: 起始 index；[length]: 个数；[timeout]: 长轮询超时（秒），无数据时服务端会挂起等待，**HttpConfig 读超时必须大于该值**；[autoDelete]: 取后是否自动删除；[tags]: 过滤参数，如 requestId（使用 put 返回的值）[示例 12](#12-场景二请求与-get-循环)、[示例 13](#13-数据管理) |
|              | DataFrame[] getByIndex(long index) | 按 index 获取单条数据（取后自动删除）[示例 13](#13-数据管理) |
|              | DataFrame[] getByRequestId(String requestId) | 按 put 返回的 requestId 获取结果（输出队列数据自动携带该 tag，取后自动删除）[示例 13](#13-数据管理) |
|              | String delete(long index)<br />String delete(long[] indexes) | 删除指定 index 的数据，成功返回 "OK" [示例 13](#13-数据管理) |
|              | void truncate(long index) | 删除所有 index **小于**指定值的数据（clear() 即基于此实现）[示例 13](#13-数据管理) |
|              | WebSocketWatcher watch(long index, long window, boolean indexOnly, boolean autoCommit, Map&lt;String, String&gt; tags)<br />watch(..., WatchConfig watchConfig) | 订阅队列。[index]: 起始 index，-1 表示跳过 pending 数据从最新开始；[window]: 发送窗口大小，即最大未 commit 数据长度；[indexOnly]: 只返回 index 和 tags 不返回数据（省带宽）；[autoCommit]: 发送即提交，无需再调用 commit（此时 window 被忽略）；[tags]: 自定义订阅参数。WatchConfig 可配置断线重连次数/间隔或无限重连（重连间隔默认 5 秒）。每个客户端实例同一时刻只允许一个 watcher [示例 11](#11-场景一请求与订阅watch) |
|              | String commit(long index)<br />String commit(long[] indexes) | 确认数据已消费并从队列删除，成功返回 "OK"。**只能对通过 watch 订阅到的数据调用** [示例 11](#11-场景一请求与订阅watch) |
|              | String negative(long index, String code, String reason)<br />String negative(long[] indexes, String code, String reason) | 否定提交（消费失败）。数据**不会**被删除，而是按错误码语义等待重投/重新处理，错误码详见[官方文档](https://help.aliyun.com/zh/pai/user-guide/queue-service-subscription-push)；同样只能对 watch 订阅到的数据调用 [示例 11](#11-场景一请求与订阅watch) |
|              | void clear() | 清空队列中所有数据（基于 truncate 实现）[示例 13](#13-数据管理) |
|              | void end(boolean force) | 关闭队列服务（破坏性操作，谨慎使用） |
| QueueUser    | QueueUser()<br />QueueUser(String uid, String gid) | 队列消费者身份：uid（默认随机数）、gid 消费组名（默认 "eas"）[示例 8](#8-创建队列客户端含自定义-path) |
| WatchConfig  | WatchConfig(int reConCnt, int reConInterval)<br />WatchConfig(boolean infinityReConnect, int reConInterval) | watch 断线重连配置：重试次数、重试间隔（秒，默认 5）、是否无限重连 [示例 11](#11-场景一请求与订阅watch) |
| WebSocketWatcher | DataFrame getDataFrame() | 阻塞获取下一条订阅数据 [示例 11](#11-场景一请求与订阅watch) |
|                  | boolean isOpen() | 订阅连接是否打开 |
|                  | void close() | 关闭订阅（再次 watch 前必须先关闭）[示例 11](#11-场景一请求与订阅watch) |
| DataFrame    | byte[] getData() | 获取数据值 |
|              | long getIndex() | 获取数据 index |
|              | Map&lt;String, String&gt; getTags() | 获取数据 Tags，可用于读取服务端返回的 "requestId" |
|              | Exception getError()<br />String getMessage() | 获取传输过程中的异常/消息 |

# 功能与接口示例

示例按服务类型分为三大类：**同步服务调用**（PredictClient：predict 阻塞调用与 predictAsync 异步回调，服务端处理完成后返回结果）、**异步服务调用**（服务端异步服务，通过 QueueClient 队列服务写入请求并订阅/拉取结果）、**通用配置**（两类调用共用的客户端配置）。

> 示例中的 `/* name= */` 是参数名注释，用于标注实参含义，不影响 Java 编译。

## 同步服务调用

### 1. 创建客户端与同步调用

**演示接口与功能：** `PredictClient(HttpConfig)` 构造、`setEndpoint` / `setServiceName` / `setToken` / `setRequestPath` / `setRetryCount` 客户端配置、`predict(String)` 同步调用，以及 `shutdown()` 释放连接。

```java
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;

HttpConfig config = new HttpConfig();
config.setConnectTimeout(5000);   // 连接超时（毫秒）
config.setReadTimeout(30000);     // 读超时（毫秒）

PredictClient client = new PredictClient(/* httpConfig= */ config);
client.setEndpoint("your-endpoint");
client.setServiceName("scorecard_pmml_example");
client.setToken("<your-token>");
client.setRequestPath("/v1");      // 服务自定义 path，追加在 "/api/predict/{服务名}" 之后；无自定义 path 时省略
client.setRetryCount(3);           // 失败重试次数，默认 3

try {
    // 字符串请求
    String result = client.predict("[{\"money_credit\": 3000000}]");
    System.out.println(result);

    // 其他入参类型：predict(byte[]) / predict(TFRequest) / predict(BladeRequest) /
    // predict(TorchRequest) / predict(JsonRequest) / predict(EasyRecRequest) 等重载
} finally {
    client.shutdown();
}
```

> client 对象需要共享复用，千万不可每个请求都创建一个 client 对象。如果要使用网络直连功能，需使用 setDirectEndpoint 方法（如 pai-eas-vpc.cn-shanghai.aliyuncs.com）；直连需在 EAS 控制台开通，普通网关访问请使用以用户 uid 开头的 endpoint。

### 2. 单次调用覆盖路径、查询参数与请求头（CallConfig）

**演示接口与功能：** `CallConfig.setRequestPath` / `setQueryParams` / `setHeaders` 单次调用级覆盖，以及带 `CallConfig` 的 `predict(String, CallConfig)`。

```java
import com.aliyun.openservices.eas.predict.http.CallConfig;
import java.util.Collections;

CallConfig callConfig = new CallConfig()
        .setRequestPath("/v2")                                      // 覆盖请求路径（优先于 client 级 setRequestPath）
        .setQueryParams(Collections.singletonMap("trace_tag", "demo"))  // 追加查询参数
        .setHeaders(Collections.singletonMap("X-Custom-Header", "value")); // 请求级 Header，覆盖 client 级 addExtraHeaders

String result = client.predict("hello", /* callConfig= */ callConfig);
```

### 3. 错误处理与失败重试

非 2xx 响应会抛出 `HttpException`，`getCode()` 为状态码，`getMessage()` 为服务端返回内容；也可以通过 `getErrorCode()` / `getErrorMessage()` 获取最近一次调用的状态信息。

**演示接口与功能：** `HttpException` 及 `getCode()` / `getMessage()` 错误处理、`getErrorCode()` / `getErrorMessage()` 最近一次调用状态，以及 `setRetryCount()` / `setRetryConditions()` 配置失败重试。

```java
import com.aliyun.openservices.eas.predict.http.HttpException;

try {
    client.predict("bad-request");
} catch (HttpException e) {
    System.out.println(e.getCode());      // 400 / 401 / 500 ...
    System.out.println(e.getMessage());   // 服务端返回的错误信息
}
```

请求失败时默认会重试，可以通过 `setRetryCount` 和 `setRetryConditions` 调整：

```java
import com.aliyun.openservices.eas.predict.http.RetryCondition;
import java.util.EnumSet;

client.setRetryCount(2);   // 重试次数，默认 3
client.setRetryConditions(EnumSet.of(
        RetryCondition.CONNECTION_FAILED,    // 连接失败
        RetryCondition.CONNECTION_TIMEOUT,   // 连接超时
        RetryCondition.READ_TIMEOUT,         // 读超时
        RetryCondition.RESPONSE_5XX,         // 5xx
        RetryCondition.RESPONSE_4XX));       // 4xx
```

不调用 `setRetryConditions` 时，所有错误都会重试。

### 4. 超时控制

**演示接口与功能：** `HttpConfig.setConnectTimeout` / `setRequestTimeout` / `setReadTimeout` 的配置方式与相互影响，以及请求超过 `requestTimeout` 时抛出 `TimeoutException` 的行为。

```java
HttpConfig config = new HttpConfig();
config.setConnectTimeout(2000);     // 连接超时
config.setRequestTimeout(10000);    // SDK 层单次调用等待上限（毫秒），0 表示不限制
// 未显式设置 readTimeout：SDK 自动推导为 max(5000, 10000 + 2000) = 12000ms

PredictClient client = new PredictClient(/* httpConfig= */ config);
// 服务端处理 6 秒：在 10 秒总超时内正常返回
// 服务端处理 15 秒：10 秒时抛出 java.util.concurrent.TimeoutException，而不是在默认 5 秒读超时失败
```

如果确实需要单独控制读空闲超时，请在构造客户端前显式设置 `readTimeout`，并保证它大于 `requestTimeout`：

```java
HttpConfig config = new HttpConfig();
config.setConnectTimeout(2000);
config.setRequestTimeout(10000);
config.setReadTimeout(15000);       // 显式设置后以该值为准

PredictClient client = new PredictClient(/* httpConfig= */ config);
```

> `requestTimeout` 是**单次尝试**的等待上限，不包含重试总时长。若显式设置的 `readTimeout` 小于或等于 `requestTimeout`，SDK 会打印 WARN，提示读超时可能先触发。QueueClient 使用长轮询 `get` 时，`readTimeout` 还必须大于轮询超时。

### 5. 异步回调（predictAsync）

`predictAsync` 是同步服务调用的一种**异步回调方式**：底层请求与服务端处理逻辑与 predict 完全一致，只是调用立即返回 `CompletableFuture`，不阻塞调用线程，通过回调或 `future.get()` 获取最终预测结果。适合调用方线程不能被阻塞（如 Web 服务、批量并发预测）的场景。

它与后文的**异步服务调用**不同：`predictAsync` 只是客户端侧的非阻塞调用，服务端仍会同步处理，最终结果直接包含在 HTTP 响应中；而异步服务通过输入队列和输出队列解耦，HTTP 请求只返回队列 index，处理结果需要从输出队列订阅或拉取。

**演示接口与功能：** `predictAsync(String)` 返回 `CompletableFuture`，演示单请求异步等待，以及多请求并发提交后统一收集结果。

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

PredictClient client = new PredictClient(/* httpConfig= */ new HttpConfig());
client.setEndpoint("your-endpoint");
client.setServiceName("scorecard_pmml_example");
client.setToken("<your-token>");

try {
    // 立即返回 future，不阻塞调用线程
    CompletableFuture<String> future = client.predictAsync("hello");
    // ... 此处可执行其他逻辑 ...
    String result = future.get(30, TimeUnit.SECONDS);
    System.out.println(result);

    // 并发提交多个请求，统一收集结果
    List<CompletableFuture<String>> futures = new ArrayList<CompletableFuture<String>>();
    for (int i = 0; i < 4; i++) {
        futures.add(client.predictAsync("hello"));
    }
    for (CompletableFuture<String> f : futures) {
        System.out.println(f.get(30, TimeUnit.SECONDS));
    }

    // 其他重载：
    // predictAsync(byte[]) / predictAsync(TFRequest) / predictAsync(BladeRequest)
    // predictAsync(CaffeRequest) / predictAsync(JsonRequest) / predictAsync(TorchRequest)
} finally {
    client.shutdown();
}
```

### 6. 完整程序：字符串输入输出

对于自定义 Processor 用户而言，通常采用字符串进行服务的输入输出调用（如 pmml 模型服务的调用）：

**演示接口与功能：** 完整程序演示 `predict(String)` 的字符串请求/响应流程，包括客户端初始化与 `shutdown()`。

```java
import com.aliyun.openservices.eas.predict.http.PredictClient;
import com.aliyun.openservices.eas.predict.http.HttpConfig;

public class Test_String {
    public static void main(String[] args) throws Exception {
        // 启动并初始化客户端, client对象需要共享，千万不可每个请求都创建一个client对象。
        PredictClient client = new PredictClient(/* httpConfig= */ new HttpConfig());
        client.setToken("<your-token>");
        // 如果要使用网络直连功能，需使用setDirectEndpoint方法
        // 如 client.setDirectEndpoint("pai-eas-vpc.cn-shanghai.aliyuncs.com");
        // 网络直连需打通在EAS控制台开通，提供用于访问EAS服务的源vswitch，打通后可绕过网关以软负载的方式直接访问服务的实例，以实现更好的稳定性和性能
        // 注：普通网关访问时请使用以用户uid为开头的endpoint，在eas控制台服务的调用信息中可查到。直连访问时请使用如上的pai-eas-vpc.{region_id}.aliyuncs.com的域名进行访问。
        client.setEndpoint("your-endpoint");
        client.setServiceName("scorecard_pmml_example");

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
    }
}
```

### 7. 完整程序：Tensorflow输入输出

TF 用户可以使用 TFRequest 与 TFResponse 作为数据的输入输出格式：

**演示接口与功能：** `TFRequest.setSignatureName` / `addFeed` / `addFetch` 构造 TensorFlow 请求，`predict(TFRequest)` 同步调用，以及 `TFResponse.getFloatVals` 读取输出。

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
        PredictClient client = new PredictClient(/* httpConfig= */ new HttpConfig());
        // 直连与网关的 endpoint 选择说明同上
        client.setEndpoint("your-endpoint");
        client.setServiceName("mnist_saved_model_example");
        client.setToken("<your-token>");
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

## 异步服务调用

> 异步服务的创建方式、队列语义与使用限制参见[官方文档：队列服务与异步推理](https://help.aliyun.com/zh/pai/queue-service-and-asynchronous-inference?spm=a2c4g.11186623.0.0.180d59a5Cnp1oO)。

服务端异步服务通过队列解耦请求与结果：客户端把请求写入输入队列（队列名即服务名），服务实例自动消费并处理，处理结果写入输出队列（"服务名/sink"）。客户端使用 `QueueClient` 写入请求，并通过订阅（watch）或拉取（get）获取结果。

> 下文统一以占位服务 `your_async_service` 为例，其输出队列为 `your_async_service/sink`；endpoint、token 与请求体格式按实际服务替换。

### 8. 创建队列客户端（含自定义 path）

**演示接口与功能：** `QueueClient(...)` 构造输入队列与输出队列客户端、`QueueUser` 配置消费者身份、`setRequestPath` 设置队列自定义 path，并说明完整 URL 与外部 HttpClient 构造方式。

```java
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.QueueUser;

// get 为长轮询，读超时需大于轮询超时（见示例 12）
HttpConfig config = new HttpConfig();
config.setReadTimeout(60000);

// 队列消费者身份：uid 默认随机数，gid 消费组名默认 "eas"
QueueUser user = new QueueUser(/* uid= */ "user-1", /* gid= */ "group-a");

// 输入队列（队列名即服务名）与输出队列（服务名/sink）
QueueClient inputQueue = new QueueClient(
        /* endpoint= */ "your-endpoint",
        /* queueName= */ "your_async_service",
        /* token= */ "<your-token>",
        /* httpConfig= */ config,
        /* user= */ new QueueUser());
QueueClient sinkQueue = new QueueClient(
        /* endpoint= */ "your-endpoint",
        /* queueName= */ "your_async_service/sink",
        /* token= */ "<your-token>",
        /* httpConfig= */ config,
        /* user= */ new QueueUser());

// 服务暴露了自定义 path 时：追加在 "/api/predict/{队列名}" 之后，对客户端的全部接口生效
inputQueue.setRequestPath("/custom");

// 也可直接传入完整 url：
// new QueueClient(
//         /* url= */ "http://host:port/api/predict/your_async_service",
//         /* token= */ token,
//         /* httpConfig= */ config,
//         /* user= */ user);

// 需要复用外部 HttpClient 5 时，使用带 httpClient 参数的构造器（SDK 不启动、不关闭）
```

### 9. 队列信息查询

**演示接口与功能：** `attributes()` 获取队列长度与首末 index、`count(...)` 按 tag/优先级统计未消费数据、`search(index)` 查询指定位点的排队状态。

```java
import com.alibaba.fastjson.JSONObject;

// 队列详细信息：meta.state / meta.maxPayloadBytes、stream.length、
// stream.firstEntry / stream.lastEntry 等
JSONObject attrs = sinkQueue.attributes();
long length = Long.parseLong(attrs.getString("stream.length"));
long first = Long.parseLong(attrs.getString("stream.firstEntry"));
long last = Long.parseLong(attrs.getString("stream.lastEntry"));

// 未消费数据个数：可按 tag 或优先级过滤
long all = sinkQueue.count(/* tags= */ null);
// requestId 来自 put 返回值的 getRight()
String requestId = "<put 返回的 requestId>";
long byRequestId = sinkQueue.count(
        /* tags= */ java.util.Collections.singletonMap("requestId", requestId));
long highPriority = inputQueue.count(
        /* priority= */ 1L,
        /* tags= */ null);

// 查询某条数据的排队信息：IsPending（是否正被处理）、WaitCount（前方排队数）
JSONObject info = inputQueue.search(/* index= */ index);
```

### 10. 写入请求（put）

**演示接口与功能：** `put(String, long, tags)` 指定优先级、`put(String, tags)` 使用默认优先级并携带自定义 tags、字符串入参重载，以及 `put(String, long, tags, CallConfig)` 的请求级路径、查询参数与 Header 覆盖。返回值为 `<输入队列 index, 服务端生成的 requestId>`。

```java
import com.aliyun.openservices.eas.predict.http.CallConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

// 1) 指定优先级：0 为普通，1 为高优先级（优先被消费）
// put 返回 <输入队列 index, 服务端生成的 requestId>
Pair<Long, String> r1 = inputQueue.put(
        "request-1",
        /* priority= */ 1L,
        /* tags= */ null);
System.out.println("index = " + r1.getLeft() + ", requestId = " + r1.getRight());

// 2) 默认普通优先级，可携带自定义 tags
Map<String, String> tags = new HashMap<String, String>();
tags.put("source", "demo");   // 自定义查询参数
Pair<Long, String> r2 = inputQueue.put(
        "request-2",
        /* tags= */ tags);
System.out.println("index = " + r2.getLeft() + ", requestId = " + r2.getRight());

// 3) 字符串重载
Pair<Long, String> r3 = inputQueue.put(
        "request-3",
        /* tags= */ tags);
System.out.println("index = " + r3.getLeft() + ", requestId = " + r3.getRight());

// 4) 单次调用覆盖路径、追加查询参数与自定义 Header（优先于 client 级 setRequestPath）
CallConfig callConfig = new CallConfig()
        .setRequestPath("/submit")
        .setQueryParams(Collections.singletonMap("source", "demo"))
        .setHeaders(Collections.singletonMap("Authorization", "<your-token>"));
Pair<Long, String> r4 = inputQueue.put(
        "request-4",
        /* priority= */ 0L,
        /* tags= */ null,
        /* callConfig= */ callConfig);
System.out.println("index = " + r4.getLeft() + ", requestId = " + r4.getRight());
```

### 11. 场景一：请求与订阅（watch）

写入请求后订阅输出队列，结果到达即可取出，确认消费后删除数据。实际使用时通常由两个线程协作：请求线程持续向输入队列写入请求，订阅线程用 `watch` 在 `while` 循环中持续消费输出队列，处理成功后 `commit`，处理失败则 `negative`。适合低延迟、持续消费结果的场景。

**演示接口与功能：** `clear()`、`put(...)`、`watch(...)`、`WebSocketWatcher.getDataFrame()` / `close()`、`commit()`、`negative()`、`shutdown()`，演示订阅式消费以及成功确认、失败否定提交。

**代码流程：**

1. 创建输入队列与输出队列客户端，并清空队列。
2. 请求线程循环调用 `put` 写入请求，打印输入队列 index 和服务端返回的 requestId。
3. 订阅线程调用 `watch` 订阅输出队列，在 `while` 循环中通过 `getDataFrame()` 阻塞获取结果；处理成功调用 `commit`，失败调用 `negative`，结束时关闭 watcher。
4. 先启动订阅线程，再启动请求线程；两个线程结束后关闭客户端。

```java
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.DataFrame;
import com.aliyun.openservices.eas.predict.queue_client.QueueUser;
import com.aliyun.openservices.eas.predict.queue_client.WebSocketWatcher;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TestQueueWatch {
    public static void main(String[] args) throws Exception {
        final String endpoint = "your-endpoint";
        final String token = "<your-token>";
        final int total = 10;

        final QueueClient inputQueue = new QueueClient(
                /* endpoint= */ endpoint,
                /* queueName= */ "your_async_service",
                /* token= */ token,
                /* httpConfig= */ new HttpConfig(),
                /* user= */ new QueueUser());
        final QueueClient sinkQueue = new QueueClient(
                /* endpoint= */ endpoint,
                /* queueName= */ "your_async_service/sink",
                /* token= */ token,
                /* httpConfig= */ new HttpConfig(),
                /* user= */ new QueueUser());

        // 演示前清空队列；生产环境请根据业务决定是否清理，避免误删未处理数据
        inputQueue.clear();
        sinkQueue.clear();

        final AtomicInteger received = new AtomicInteger();

        // 线程 1：专门发送请求
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < total; i++) {
                        // 自定义 tags 可选
                        Map<String, String> tags = Collections.singletonMap("source", "demo");
                        Pair<Long, String> submitted = inputQueue.put(
                                "request-" + i,
                                /* tags= */ tags);
                        // 输入队列 index, 服务端返回的 requestId
                        System.out.println("index = " + submitted.getLeft()
                                + ", requestId = " + submitted.getRight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "request-producer");

        // 线程 2：专门订阅输出队列
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                WebSocketWatcher watcher = null;
                try {
                    watcher = sinkQueue.watch(
                            /* index= */ 0L,
                            /* window= */ 5L,
                            /* indexOnly= */ false,
                            /* autoCommit= */ false,
                            /* tags= */ null);
                    while (received.get() < total) {
                        DataFrame frame = watcher.getDataFrame();
                        // 输出队列的 DataFrame 自带服务端生成的 requestId tag
                        String requestId = frame.getTags().get("requestId");
                        try {
                            // 处理结果：这里的业务逻辑可按实际替换
                            String result = new String(frame.getData(), "UTF-8");
                            System.out.println("index = " + frame.getIndex()
                                    + ", requestId = " + requestId + ", result = " + result);
                            // 确认消费成功：服务端删除该条输出数据，不再重复投递
                            sinkQueue.commit(frame.getIndex());
                            received.incrementAndGet();
                        } catch (Exception e) {
                            // 处理失败：否定提交，等待服务端重新投递
                            try {
                                sinkQueue.negative(
                                        /* index= */ frame.getIndex(),
                                        /* code= */ "500",
                                        /* reason= */ e.getMessage());
                            } catch (Exception negativeError) {
                                negativeError.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (watcher != null) {
                        watcher.close();
                    }
                }
            }
        }, "result-subscriber");

        consumer.start();   // 先启动订阅，再发送请求
        producer.start();
        producer.join();
        consumer.join();

        inputQueue.shutdown();
        sinkQueue.shutdown();
    }
}
```

### 12. 场景二：请求与 get 循环

批量写入请求并记录每个请求的 requestId，再按 requestId 逐个 `get` 长轮询结果。`get` 在无数据时由服务端挂起等待至 timeout（秒）或返回空数组，因此 **HttpConfig 的读超时必须大于轮询 timeout**。该方式不需要并发线程，适合先提交一批任务、再统一收取结果的场景。

**演示接口与功能：** `put(...)` 写入请求并获取服务端返回的 requestId、`get(index, length, timeout, autoDelete, tags)` 按 requestId tag 长轮询、`delete(index)` 删除已处理结果，以及 `DataFrame.getIndex()` / `getTags()` / `getData()`，演示通过 requestId 关联请求与结果。

**代码流程：**

1. 创建输入队列与输出队列客户端；输出队列的 `readTimeout` 设置为 60 秒，确保大于 30 秒轮询超时；清空队列。
2. 循环调用 `put` 写入全部请求，将返回的 requestId 保存到列表中，同时打印输入队列 index 和 requestId。
3. 遍历 requestId 列表，逐个调用 `get`，通过 `tags` 指定 requestId 并长轮询结果；拿到结果后打印输出队列 index、requestId 和结果，并调用 `delete` 删除数据。
4. 全部结果处理完成后关闭两个客户端。

```java
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.QueueClient;
import com.aliyun.openservices.eas.predict.queue_client.DataFrame;
import com.aliyun.openservices.eas.predict.queue_client.QueueUser;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestQueueGetLoop {
    public static void main(String[] args) throws Exception {
        final String endpoint = "your-endpoint";
        final String token = "<your-token>";
        final int total = 100;

        QueueClient inputQueue = new QueueClient(
                /* endpoint= */ endpoint,
                /* queueName= */ "your_async_service",
                /* token= */ token,
                /* httpConfig= */ new HttpConfig(),
                /* user= */ new QueueUser());
        // get 长轮询会在服务端挂起至 timeout，读超时必须大于轮询超时
        HttpConfig sinkConfig = new HttpConfig();
        sinkConfig.setReadTimeout(60000);
        QueueClient sinkQueue = new QueueClient(
                /* endpoint= */ endpoint,
                /* queueName= */ "your_async_service/sink",
                /* token= */ token,
                /* httpConfig= */ sinkConfig,
                /* user= */ new QueueUser());

        inputQueue.clear();
        sinkQueue.clear();

        try {
            // 1. 先批量写入请求，并记录每个请求的 requestId
            List<String> requestIds = new ArrayList<String>();
            for (int i = 0; i < total; i++) {
                Pair<Long, String> submitted = inputQueue.put(
                        "request-" + i,
                        /* tags= */ Collections.singletonMap("source", "demo"));
                System.out.println("index = " + submitted.getLeft()
                        + ", requestId = " + submitted.getRight());
                requestIds.add(submitted.getRight());
            }

            // 2. 再按 requestId 逐个长轮询结果
            for (String requestId : requestIds) {
                DataFrame[] frames = sinkQueue.get(
                        /* index= */ 0L,
                        /* length= */ 1L,
                        /* timeout= */ 30,
                        /* autoDelete= */ false,
                        /* tags= */ Collections.singletonMap("requestId", requestId));
                if (frames.length == 0) {
                    // 30 秒内未拿到结果，可按业务需要重试或记录失败
                    System.out.println("requestId = " + requestId + " not ready");
                    continue;
                }

                // 这里使用的是输出队列 index，与输入队列 index 无关
                DataFrame frame = frames[0];
                String result = new String(frame.getData(), "UTF-8");
                System.out.println("index = " + frame.getIndex()
                        + ", requestId = " + requestId + ", result = " + result);
                sinkQueue.delete(frame.getIndex());
            }
        } finally {
            inputQueue.shutdown();
            sinkQueue.shutdown();
        }
    }
}
```

### 13. 数据管理

> **输入队列与输出队列的 index 相互独立。** `inputQueue.put()` 返回的是输入队列 index，`sinkQueue.get()` / `getByIndex()` 使用的是输出队列 index，不能用输入队列 index 去查询输出队列。put 返回值的 `getRight()` 是服务端通过响应头 `X-Eas-Queueservice-Request-Id` 返回的 requestId，只有它能唯一对应同一条请求与结果；该 requestId 会出现在输出队列 DataFrame 的 tags 中，可通过 `sinkQueue.getByRequestId(requestId)` 或 `DataFrame.getTags().get("requestId")` 关联。

**演示接口与功能：** `put(...)` 返回的输入队列 index 与 requestId、`getByIndex()` / `getByRequestId()` 按不同维度查询、`delete(long)` / `delete(long[])` / `truncate()` / `clear()` 删除数据、`negative()` 否定提交，以及 `end()` 关闭队列。重点演示输入/输出队列 index 相互独立、只能通过 requestId 关联请求与结果。

```java
import org.apache.commons.lang3.tuple.Pair;

// put 返回 <输入队列 index, 服务端生成的 requestId>
Pair<Long, String> submitted = inputQueue.put(
        "request-body",
        /* tags= */ null);
long inputIndex = submitted.getLeft();     // 输入队列 index
String requestId = submitted.getRight();   // 服务端返回的 requestId

// 输出数据使用输出队列 index；跨队列关联使用 requestId
DataFrame[] byIndex = sinkQueue.getByIndex(/* index= */ outputIndex);
DataFrame[] byRequestId = sinkQueue.getByRequestId(/* requestId= */ requestId);

// 删除指定 index 的数据 / 删除所有 index 小于指定值的数据（clear 基于 truncate 实现）
sinkQueue.delete(/* index= */ index);
sinkQueue.delete(/* indexes= */ new long[]{index1, index2});
sinkQueue.truncate(/* index= */ lastIndex + 1);
sinkQueue.clear();

// 否定提交（消费失败）：数据不会被删除，按错误码语义等待重投/重新处理；
// 只能对 watch 订阅到的数据调用
sinkQueue.negative(
        /* index= */ index,
        /* code= */ "500",
        /* reason= */ "process failed");
sinkQueue.negative(
        /* indexes= */ new long[]{index1, index2},
        /* code= */ "500",
        /* reason= */ "process failed");

// 关闭队列服务（破坏性操作，谨慎使用）
// sinkQueue.end(/* force= */ false);
```

**QueueClient 语义说明：**

* 输入队列与输出队列的 `index` 相互独立，只有 put 返回的 `requestId` 能对应同一条请求与结果；`requestId` 由服务端生成并通过 put 响应头返回，会出现在输出队列 DataFrame 的 tags 中，可通过 `getByRequestId` / `DataFrame.getTags()` 获取。高优先级请求的完成顺序可能与写入顺序不同，建议按 `requestId` 关联而非依赖顺序。
* `commit` 确认消费并删除数据；`negative` 否定提交（消费失败），数据**不会**被删除，而是按错误码语义等待重投/重新处理（错误码见[官方文档](https://help.aliyun.com/zh/pai/user-guide/queue-service-subscription-push)）。两者都只能对通过 watch 订阅到的数据调用。
* watch 关闭后才能再次 watch（每个客户端实例同一时刻只允许一个 watcher）；`autoCommit=true` 时数据发送即提交，`window` 被忽略。
* `get` 是长轮询，`timeout` 单位为秒，HttpConfig 读超时必须大于该值，否则会抛读超时异常。
* `truncate` / `clear` 删除数据后 `stream.firstEntry` 不会推进，计算起始 index 时请以 `attributes()` / `get` 返回的实际 index 为准。
* 客户端级 `setRequestPath` 与单次调用 `CallConfig`（路径/查询参数/自定义 Header）均生效：自定义 path 可命中同一队列，`CallConfig` 的 Header 可覆盖签名头（如 `Authorization`）。

## 通用配置

### 14. 子客户端（多线程共享连接池）

**演示接口与功能：** `createChildClient()` 创建共享父客户端 HTTP 连接池与配置的子客户端，`predict(String)` 复用连接发起预测，生命周期由父客户端统一管理。

```java
PredictClient child = client.createChildClient();
String result = child.predict("hello");
// 子客户端与父客户端共享同一个 HTTP client，由父客户端统一 shutdown()
```

### 15. 关闭客户端

**演示接口与功能：** `shutdown()` 关闭客户端资源：SDK 自建的 HttpClient 会被关闭，外部注入的 HttpClient 不由 SDK 关闭。

```java
client.shutdown();
```

* SDK 自建的 HttpClient 会在此关闭
* 外部注入的 HttpClient **不会**被关闭（生命周期由调用方管理）

### 16. 使用外部 HttpClient 5

3.0 新增，注入外部构建并已启动的 HttpClient 5 客户端（TLS、代理、连接池、拦截器、可观测性等均在外部 client 上配置）：

**演示接口与功能：** `PredictClient(HttpConfig, CloseableHttpAsyncClient)` 构造注入与 `setHttpClient(...)` 运行时注入、`predict(String)` 调用，以及 SDK 不启动、不关闭外部 client 的生命周期约定。

```java
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;

// 外部自由定制：TLS、代理、连接池、拦截器、可观测性等
CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
        // .setConnectionManager(自定义连接池)
        // .addRequestInterceptorFirst(自定义埋点)
        .build();
httpClient.start();   // 由调用方启动

HttpConfig sdkConfig = new HttpConfig();
sdkConfig.setConnectTimeout(5000);   // 注入模式下连接类参数不生效（以 httpClient 为准）
sdkConfig.setReadTimeout(30000);

PredictClient client = new PredictClient(
        /* httpConfig= */ sdkConfig,
        /* httpClient= */ httpClient);
// 或 client.setHttpClient(httpClient);
client.setEndpoint("your-endpoint");
client.setServiceName("scorecard_pmml_example");
client.setToken("<your-token>");

try {
    System.out.println(client.predict("hello"));
} finally {
    client.shutdown();     // 不会关闭 httpClient
    httpClient.close();    // 由调用方关闭
}
```

### 17. 请求数据压缩

请求数据量大时，EAS 支持压缩后再发送，需在服务配置中指定对应的 `rpc.decompressor`（支持 zlib、gzip、snappy、zstd、lz4）：

**演示接口与功能：** `setCompressor(...)` 设置请求压缩方式、`setDecompressor(...)` 设置响应解压方式（可选 `Auto`），并通过 `predict(String)` 发起压缩请求。

```json
"metadata": {
  "rpc": {
    "decompressor": "lz4"
  }
}
```

```java
import com.aliyun.openservices.eas.predict.http.Compressor;

PredictClient client = new PredictClient(/* httpConfig= */ new HttpConfig());
client.setEndpoint("your-endpoint");
client.setServiceName("your_model");
client.setToken("<your-token>");
client.setCompressor(Compressor.Zlib);     // 也可选 Gzip / Snappy / LZ4 / LZ4Frame / Zstd
// client.setDecompressor(Compressor.Auto);  // 需要解压响应时设置；Auto 按 Content-Encoding 自动识别

String response = client.predict("[{\"money_credit\": 3000000}]");
client.shutdown();
```

客户端与服务端所设置的压缩方式需一致；该功能适用于任何请求形式。

### 18. HTTP Tracing 耗时诊断

**演示接口与功能：** `HttpConfig.setHttpTracingEnabled(true)` 开启 HTTP Tracing（须在构造客户端前设置），同步 `predict(String)` 调用自动携带 `Client-Timestamp` 请求头，并根据服务端返回的 tracing 头输出服务端分段耗时日志。

```java
HttpConfig config = new HttpConfig();
config.setHttpTracingEnabled(/* httpTracingEnabled= */ true);        // 默认关闭，必须在构造客户端之前设置

PredictClient client = new PredictClient(/* httpConfig= */ config);
client.setEndpoint("your-endpoint");
client.setServiceName("scorecard_pmml_example");
client.setToken("<your-token>");

client.predict("hello");
client.shutdown();
```

开启后 SDK 会在同步 predict 请求中自动携带 `Client-Timestamp` 请求头激活 EAS 服务端 tracing；服务端在响应头中返回各阶段时间戳后，SDK 向 logger `com.aliyun.openservices.eas.predict.http.tracing`（INFO）输出一条服务端分段耗时日志。每次重试各输出一条；响应中没有服务端 tracing 头时不输出。`predictAsync` 不参与 tracing。字段含义见[附录 A](#附录-ahttp-tracing-字段说明)。

HTTP Tracing 只统计服务端分段。客户端侧的 DNS、建连、TLS、发送、TTFB、下载、连接池、签名、压缩等耗时不在 SDK 内统计；如有需要，可通过外部 HTTP client 的拦截器或 Agent 自行采集（见[通过外部 HTTP client 接入 ARMS / OpenTelemetry](#通过外部-http-client-接入-arms--opentelemetry)）。

# 附录 A：HTTP Tracing 字段说明

开启后每次**同步 predict 调用**（含每次重试）输出一条单行 JSON 日志；日志仅在服务端返回 tracing 头时生成。`predictAsync` 不参与 tracing：

```json
{"timestamp":1730527200123,"event":"eas_http_tracing","url.full":"http://xxx/api/predict/m","attempt":1,"http.response.status_code":200,"srv_recv_ms":1.2,"srv_queue_ms":0.1,"srv_process_ms":24.8,"srv_total_ms":26.1,"srv_queue_length":0}
```

日志采用 OpenTelemetry 语义约定命名的标准维度（`url.full`、`http.response.status_code`）以及 EAS 服务端分段字段（`srv_*`），可直接接入 ARMS / SLS 做聚合分析。服务端未返回的分段不会输出；例如只返回 Server/Response 时间戳时，日志中只有 `srv_total_ms`。

### 服务端分段（server 时钟，EAS 服务端返回 tracing 头时才有）

| 字段 | 含义 |
|---|---|
| srv_recv_ms | 服务端接收请求体的耗时（Enqueue−Server 时间戳），反映上行带宽 |
| srv_queue_ms | 服务端排队等待（Dequeue−Enqueue 时间戳），反映服务端负载，均值反映负载、方差反映稳定性 |
| srv_process_ms | 模型计算耗时（Response−Dequeue 时间戳），均值反映计算速度、方差反映稳定性 |
| srv_total_ms | 服务端全程（Response−Server 时间戳） |
| srv_queue_length | 入队时的服务端队列长度 |

### 定位思路

| 观测到 | 结论 |
|---|---|
| srv_recv_ms 高 | 请求体传输慢（上行带宽不足或请求体过大） |
| srv_queue_ms 高（结合 srv_queue_length） | 服务端过载排队，考虑扩容或降低并发 |
| srv_process_ms 高 | 模型计算慢，结合服务端资源/批处理配置排查 |
| srv_total_ms 明显高于 srv_process_ms | 耗时主要在排队或请求接收，而非模型计算 |

均值/方差/直方图等聚合统计建议采集该 logger 的 JSON 日志（如接入 SLS / ARMS 日志分析）完成。

### 通过外部 HTTP client 接入 ARMS / OpenTelemetry

3.0 起 SDK 内置的 HTTP client 已升级为 Apache HttpClient 5，并且支持传入外部构建的 client。可观测性（ARMS/OpenTelemetry/自定义埋点）统一在外部 client 或 Java Agent 侧配置，SDK 不再内置任何追踪桥接：

```java
// 1) 外部构建并定制 client（TLS/代理/连接池/拦截器/OTel 埋点均可自由配置）
CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
        .setConnectionManager(/* 自定义连接池 */ ...)
        .addRequestInterceptorFirst(/* 自定义埋点/打标 */ ...)
        .build();
httpClient.start();   // 由调用方负责启动

// 2) 注入；SDK 只使用，不启动、不关闭
PredictClient client = new PredictClient(
        /* httpConfig= */ new HttpConfig(),
        /* httpClient= */ httpClient);
// 或 client.setHttpClient(httpClient);
```

* 外部 client 的生命周期归调用方：SDK 的 `shutdown()` 不会关闭它
* 注入模式下 `HttpConfig` 的连接类参数（超时/连接池/IO 线程/keepalive 等）不再生效，统一由外部 client 配置（SDK 会打印 WARN 提示）
* 零代码接入 ARMS：挂载 ARMS/OTel Java Agent（自动对 Apache HttpClient 5 埋点并注入 W3C traceparent）
* 也可在外部 client 上通过拦截器或包装 `CloseableHttpAsyncClient` 自行上报
* SDK 的 HTTP Tracing 日志只包含服务端分段，注入外部 client 后行为不变；客户端网络/连接池/压缩等分段请在外部 client 的拦截器或 Agent 侧自行采集

> 注意：HttpClient 5 使用 SLF4J 作为日志门面，SDK 会传递依赖 `slf4j-api`。若应用未引入 SLF4J 绑定，启动时会出现 `SLF4J: Defaulting to no-operation (NOP) logger` 提示（不影响功能）；如需查看 HttpClient 内部日志，请在应用中添加合适的 SLF4J 实现。
