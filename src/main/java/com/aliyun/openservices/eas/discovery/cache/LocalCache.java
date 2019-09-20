package com.aliyun.openservices.eas.discovery.cache;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;
import com.aliyun.openservices.eas.discovery.core.Endpoint;
import com.aliyun.openservices.eas.discovery.core.Service;
import com.aliyun.openservices.eas.discovery.utils.CollectionUtils;
import com.aliyun.openservices.eas.discovery.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LocalCache {
    public static void write(Service dom) {
        write(dom, DiscoveryClient.getCacheDir());
    }

    public static void write(Service dom, String dir) {
        try {
            makeSureCacheDirExists(dir);
            File file = new File(dir, dom.getKey());
            if (!file.exists()) {
                if (!file.createNewFile() && !file.exists()) // add another !file.exists() to avoid conflicted creating-new-file from multi-instances
                {
                    throw new IllegalStateException("failed to create cache file");
                }
            }

            StringBuilder keyContentBuffer = new StringBuilder();

            String json = dom.getJsonFromServer();

            if (StringUtils.isEmpty(json)) {
                json = JSON.toJSONString(dom);
            }

            keyContentBuffer.append(json);

            //Use the concurrent API to ensure the consistency.
            ConcurrentDiskUtil.writeFileContent(file, keyContentBuffer.toString(), Charset.defaultCharset().toString());


        } catch (Throwable e) {
            DiscoveryClient.LOG.error("NA", "failed to write cache for dom:" + dom.getName(), e);
        }
    }

    public static String getLineSeperator() {
        return System.getProperty("line.separator");
    }

    public static Map<String, Service> read() {
        Map<String, Service> domMap = new HashMap<String, Service>();

        BufferedReader reader = null;
        try {
            File[] files = makeSureCacheDirExists().listFiles();
            if (files == null) {
                return domMap;
            }

            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }

                if (!(file.getName().endsWith(Service.SPLITER + "meta") || file.getName().endsWith(Service.SPLITER + "special-url"))) {
                    Service dom = new Service(file.getName());
                    List<Endpoint> ips = new ArrayList<Endpoint>();
                    dom.setEndpoints(ips);

                    Service newFormat = null;

                    try {
                        String dataString = ConcurrentDiskUtil.getFileContent(file, Charset.defaultCharset().toString());
                        reader = new BufferedReader(new StringReader(dataString));

                        String json;
                        while ((json = reader.readLine()) != null) {
                            try {
                                if (!json.startsWith("{")) {
                                    continue;
                                }

                                newFormat = JSON.parseObject(json, Service.class);

                                if (StringUtils.isEmpty(newFormat.getName())) {
                                    ips.add(JSON.parseObject(json, Endpoint.class));
                                }
                            } catch (Throwable e) {
                                DiscoveryClient.LOG.error("NA", "error while parsing cache file: " + json, e);
                            }
                        }
                    } catch (Exception e) {
                        DiscoveryClient.LOG.error("NA", "failed to read cache for dom: " + file.getName(), e);
                    } finally {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                    if (newFormat != null && !StringUtils.isEmpty(newFormat.getName()) && !CollectionUtils.isEmpty(newFormat.getEndpoints())) {
                        domMap.put(dom.getKey(), newFormat);
                    } else if (!CollectionUtils.isEmpty(dom.getEndpoints())) {
                        domMap.put(dom.getKey(), dom);
                    }
                }

            }
        } catch (Exception e) {
            DiscoveryClient.LOG.error("NA", "failed to read cache file", e);
        }


        return domMap;
    }


    private static File makeSureCacheDirExists() {
        return makeSureCacheDirExists(DiscoveryClient.getCacheDir());
    }

    private static File makeSureCacheDirExists(String dir) {
        File cacheDir = new File(dir);
        if ((!cacheDir.exists() || !cacheDir.isDirectory()) && !cacheDir.mkdirs()) {
            throw new IllegalStateException("failed to create cache dir: " + DiscoveryClient.getCacheDir());
        }

        return cacheDir;
    }
}
