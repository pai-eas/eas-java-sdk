package com.aliyun.openservices.eas.discovery.net;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.discovery.utils.IOUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class HttpClient {
    public static final int TIME_OUT_MILLIS = Integer.parseInt(System.getProperty("com.aliyun.eas.socket.timeout", "3000"));
    public static final int CON_TIME_OUT_MILLIS = Integer.parseInt(System.getProperty("com.aliyun.eas.connect.timeout", "1000"));
    private static final boolean enableHttps = Boolean.parseBoolean(System.getProperty("tls.enable", "false"));

    static {
        // limit max redirection
        System.setProperty("http.maxRedirects", "5");
    }

    public static String getPrefix() {
        if (enableHttps) {
            return "https://";
        }

        return "http://";

    }

    public static String getServerPort() {
        if (enableHttps) {
            return DiscoveryServerProxy.TLS_SERVER_PORT;
        }

        return DiscoveryServerProxy.SERVER_PORT;
    }

    public static HttpResult httpGet(String url, List<String> headers, Map<String, String> paramValues, String encoding) {
        return httpGet(url, headers, paramValues, encoding, TIME_OUT_MILLIS, CON_TIME_OUT_MILLIS);
    }

    public static HttpResult httpGet(String url, List<String> headers, Map<String, String> paramValues, String encoding, int timeout, int connTimeout) {
        HttpURLConnection conn = null;
        try {
            String encodedContent = encodingParams(paramValues, encoding);
            url += (null == encodedContent) ? "" : ("?" + encodedContent);

            conn = (HttpURLConnection) new URL(url).openConnection();

            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            setHeaders(conn, headers, encoding);
            conn.connect();
            DiscoveryClient.LOG.debug("Request from server: " + url);
            HttpResult result = getResult(conn);

            if (result.code != HttpsURLConnection.HTTP_OK) {
                DiscoveryClient.LOG.warn("failed to request " + url + ", caused by " + result.content);
            }
            return result;
        } catch (Exception e) {
            try {
                if (conn != null) {
                    DiscoveryClient.LOG.warn("failed to request " + conn.getURL() + " from "
                        + InetAddress.getByName(conn.getURL().getHost()).getHostAddress());
                }
            } catch (Exception e1) {
                DiscoveryClient.LOG.error("NA", "failed to request ", e1);
                //ignore
            }

            DiscoveryClient.LOG.error("NA", "failed to request ", e);

            return new HttpResult(500, e.toString(), Collections.<String, String>emptyMap());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpResult getResult(HttpURLConnection conn) throws IOException {
        int respCode = conn.getResponseCode();

        InputStream inputStream;
        if (HttpURLConnection.HTTP_OK == respCode
            || HttpURLConnection.HTTP_NOT_MODIFIED == respCode) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        Map<String, String> respHeaders = new HashMap<String, String>(conn.getHeaderFields().size());
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() != null) {
                respHeaders.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
            }
        }

        if ("gzip".equals(respHeaders.get("content-encoding"))) {
            inputStream = new GZIPInputStream(inputStream);
        }

        return new HttpResult(respCode, IOUtils.toString(inputStream, getCharset(conn)), respHeaders);
    }

    private static String getCharset(HttpURLConnection conn) {
        String contentType = conn.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            return "GBK";
        }

        String[] values = contentType.split(";");
        if (values.length == 0) {
            return "GBK";
        }

        String charset = "GBK";
        for (String value : values) {
            value = value.trim();

            if (value.toLowerCase().startsWith("charset=")) {
                charset = value.substring("charset=".length());
            }
        }

        return charset;
    }

    private static void setHeaders(HttpURLConnection conn, List<String> headers, String encoding) {
        if (null != headers) {
            for (Iterator<String> iter = headers.iterator(); iter.hasNext(); ) {
                conn.addRequestProperty(iter.next(), iter.next());
            }
        }

        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="
            + encoding);
        conn.addRequestProperty("Accept-Charset", encoding);
    }

    private static String encodingParams(Map<String, String> params, String encoding)
        throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == params || params.isEmpty()) {
            return null;
        }

        params.put("encoding", encoding);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }

            sb.append(entry.getKey()).append("=");
            sb.append(URLEncoder.encode(entry.getValue(), encoding));
            sb.append("&");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("s", "Wms+rkGG8jlaBBbpl8FIDxxNQGA=");
        System.out.println(encodingParams(params, "utf-8"));
    }

    public static class HttpResult {
        final public int code;
        final public String content;
        final private Map<String, String> respHeaders;

        public HttpResult(int code, String content, Map<String, String> respHeaders) {
            this.code = code;
            this.content = content;
            this.respHeaders = respHeaders;
        }

        public String getHeader(String name) {
            return respHeaders.get(name.toLowerCase());
        }
    }
}
