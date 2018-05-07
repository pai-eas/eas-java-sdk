package com.aliyun.openservices.eas.predict.http;

import com.aliyun.openservices.eas.predict.auth.HmacSha1Signature;
import com.aliyun.openservices.eas.predict.request.CaffeRequest;
import com.aliyun.openservices.eas.predict.request.JsonRequest;
import com.aliyun.openservices.eas.predict.request.TFRequest;
import com.aliyun.openservices.eas.predict.response.CaffeResponse;
import com.aliyun.openservices.eas.predict.response.JsonResponse;
import com.aliyun.openservices.eas.predict.response.TFResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.xerial.snappy.Snappy;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by xiping.zk on 2018/05/07.
 */
public class PredictClient {
	private static Log log = LogFactory.getLog(PredictClient.class);
	private CloseableHttpAsyncClient httpclient = null;
	private String token = null;
	private String modelName = null;
	private String endpoint = null;
	private boolean isCompressed = false;
	ObjectMapper defaultObjectMapper = new ObjectMapper();

	public PredictClient() {
	}

	public PredictClient(HttpConfig httpConfig) {
		try {
			ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
			PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
					ioReactor);
			cm.setMaxTotal(httpConfig.getMaxConnectionCount());
			cm.setDefaultMaxPerRoute(httpConfig.getMaxConnectionPerRoute());

			IOReactorConfig config = IOReactorConfig.custom()
					.setTcpNoDelay(true)
					.setSoTimeout(httpConfig.getReadTimeout())
					.setSoReuseAddress(true)
					.setConnectTimeout(httpConfig.getConnectTimeout())
					.setIoThreadCount(httpConfig.getIoThreadNum())
					.setSoKeepAlive(httpConfig.isKeepAlive()).build();
			final RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(httpConfig.getConnectTimeout())
					.setSocketTimeout(httpConfig.getReadTimeout()).build();

			httpclient = HttpAsyncClients.custom().setConnectionManager(cm)
					.setDefaultIOReactorConfig(config)
					.setDefaultRequestConfig(requestConfig).build();
			httpclient.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setHttp(CloseableHttpAsyncClient httpclient) {
		this.httpclient = httpclient;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setIsCompressed(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}

	public void printConfig() {
		System.out.println("token:" + token);
		System.out.println("endpoint:" + endpoint);
		System.out.println("modelname:" + modelName);
	}

	private String buildUri() {
		return "http://" + endpoint + "/api/predict/" + modelName;
	}

	private void generateSignature(HttpPost request, byte[] requestContent) {
		HmacSha1Signature signature = new HmacSha1Signature();
		String md5Content = signature.getMD5(requestContent);
		request.addHeader(HttpHeaders.CONTENT_MD5, md5Content);
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String currentTime = dateFormat.format(now) + " GMT";
		request.addHeader(HttpHeaders.DATE, currentTime);
		request.addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
		String auth = "POST" + "\n" + md5Content + "\n"
				+ "application/octet-stream" + "\n" + currentTime + "\n"
				+ "/api/predict/" + modelName;
		request.addHeader(HttpHeaders.AUTHORIZATION,
				"EAS " + signature.computeSignature(token, auth));
	}
	
	private byte[] getContent(HttpPost request) throws IOException, InterruptedException, ExecutionException {
		byte[] content = null;
		int status = 0;
		HttpResponse response = null;
		Future<HttpResponse> future = httpclient.execute(request, null);
		response = future.get();
		
		if (future.isDone()) {
			try {
				status = response.getStatusLine().getStatusCode();
				if (status == 200) {
					content = IOUtils.toByteArray(response.getEntity()
							.getContent());
					if (isCompressed)
						content = Snappy.uncompress(content);
				} else {
					throw new IOException("Status Code: "
							+ status
							+ " Predict Failed: "
							+ IOUtils.toString(response.getEntity()
									.getContent(), "UTF-8"));
				}
			} catch (IllegalStateException e) {
				log.error("Illegal State", e);
			}
		} else if (future.isCancelled()) {
			log.error("request cancelled!", new Exception("Request cancelled"));
		} else {
			throw new IOException("request failed!");
		}
		return content;
	}

	public PredictClient createChlidClient(String token, String endpoint,
			String modelname) {
		PredictClient client = new PredictClient();
		client.setHttp(this.httpclient);
		client.setToken(token);
		client.setEndpoint(endpoint);
		client.setModelName(modelname);
		return client;
	}

	public TFResponse predict(TFRequest runRequest) {
		TFResponse runResponse = new TFResponse();
		byte[] result = predict(runRequest.getRequest().toByteArray());
		if (result != null) {
			runResponse.setContentValues(result);
		}
		return runResponse;
	}

	public CaffeResponse predict(CaffeRequest runRequest) {
		CaffeResponse runResponse = new CaffeResponse();
		byte[] result = predict(runRequest.getRequest().toByteArray());
		if (result != null) {
			runResponse.setContentValues(result);
		}
		return runResponse;
	}

	public JsonResponse predict(JsonRequest requestContent)
			throws JsonGenerationException, JsonMappingException, IOException {
		byte[] result = predict(defaultObjectMapper
				.writeValueAsBytes(requestContent));

		return defaultObjectMapper.readValue(result, 0, result.length,
				JsonResponse.class);
	}

	public String predict(String requestContent) {
		byte[] result = predict(requestContent.getBytes());
		return new String(result);
	}

	public byte[] predict(byte[] requestContent) {
		HttpPost request = new HttpPost(buildUri());
		if (isCompressed) {
			try {
				requestContent = Snappy.compress(requestContent);
			} catch (IOException e) {
				log.error("Compress Error", e);
			}
		}
		request.setEntity(new NByteArrayEntity(requestContent));
		if (token != null)
			generateSignature(request, requestContent);		
		
		byte[] content = null;
		try {
			content = getContent(request);
		} catch (Exception ex) {
			for (int i = 0; i < 3; i++) {
				try {
					content = getContent(request);
				} catch (Exception e) {
					if (i == 2)
						log.error("Exception Error", e);
					continue;
				}
				break;
			}
		}
		return content;
	}

	public void shutdown() {
		try {
			httpclient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
