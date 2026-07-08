package com.aliyun.openservices.eas.predict;

import com.aliyun.openservices.eas.predict.http.CallConfig;
import com.aliyun.openservices.eas.predict.http.HttpConfig;
import com.aliyun.openservices.eas.predict.http.PredictClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Integration test for per-call CallConfig against a real EAS endpoint.
 *
 * Usage:
 *   1. Fill in ENDPOINT, MODEL_NAME, TOKEN below.
 *   2. Run: mvn test-compile exec:java -Dexec.mainClass=com.aliyun.openservices.eas.predict.Test_CallConfig
 *      or just run main() from IDE.
 *
 * Expected: each predict call returns a response string; URLs in logs show
 * the correct path + query string per CallConfig.
 */
public class Test_CallConfig {

    // ====== Fill these in to run against your EAS service ======
    static final String ENDPOINT = "";
    static final String MODEL_NAME = "test_example";
    static final String TOKEN = "test-token";
    // ==========================================================

    static final String REQUEST_JSON = "1";

    public static void main(String[] args) throws Exception {
        PredictClient client = new PredictClient(new HttpConfig());
        client.setToken(TOKEN);
        client.setEndpoint(ENDPOINT);
        client.setModelName(MODEL_NAME);
        client.setRequestPath("/infer");

        try {
            // 1) No CallConfig — uses client default path "/v1", no query
            System.out.println("=== Test 1: no CallConfig (client default) ===");
            String r1 = client.predict(REQUEST_JSON);
            System.out.println("response: " + r1);

            // 2) CallConfig with query only — path inherits client default "/v1"
            System.out.println("\n=== Test 2: query only ===");
            Map<String, String> q2 = new HashMap<>();
            q2.put("uid", "1168XXXX");
            CallConfig cfg2 = new CallConfig().setQueryParams(q2);
            String r2 = client.predict(REQUEST_JSON, cfg2);
            System.out.println("response: " + r2);

            // 3) CallConfig with path only — query is empty
            System.out.println("\n=== Test 3: path only ===");
            CallConfig cfg3 = new CallConfig().setRequestPath("/sleep");
            String r3 = client.predict(REQUEST_JSON, cfg3);
            System.out.println("response: " + r3);

            // 4) CallConfig with both path and query
            System.out.println("\n=== Test 4: path + query ===");
            Map<String, String> q4 = new LinkedHashMap<>();
            q4.put("uid", "1168XXXX");
            q4.put("debug", "1");
            CallConfig cfg4 = new CallConfig()
                .setRequestPath("/sleep")
                .setQueryParams(q4);
            String r4 = client.predict(REQUEST_JSON, cfg4);
            System.out.println("response: " + r4);

            // 5) CallConfig with null query value (flag-style)
            System.out.println("\n=== Test 5: null query value ===");
            Map<String, String> q5 = new LinkedHashMap<>();
            q5.put("flag", null);
            q5.put("uid", "9999");
            CallConfig cfg5 = new CallConfig().setQueryParams(q5);
            String r5 = client.predict(REQUEST_JSON, cfg5);
            System.out.println("response: " + r5);

            // 6) Null CallConfig — same as no config
            System.out.println("\n=== Test 6: null CallConfig ===");
            String r6 = client.predict(REQUEST_JSON, (CallConfig) null);
            System.out.println("response: " + r6);

            // 7) Different config per call (same client, different query)
            System.out.println("\n=== Test 7: different config per call ===");
            for (int i = 0; i < 3; i++) {
                Map<String, String> q = new HashMap<>();
                q.put("uid", "user_" + i);
                CallConfig cfg = new CallConfig().setQueryParams(q);
                String r = client.predict(REQUEST_JSON, cfg);
                System.out.println("call " + i + " response: " + r);
            }

        } finally {
            client.shutdown();
        }
    }
}
