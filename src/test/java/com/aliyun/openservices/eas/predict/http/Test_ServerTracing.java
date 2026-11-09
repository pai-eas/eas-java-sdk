package com.aliyun.openservices.eas.predict.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the timestamp precision normalization of the EAS server-side
 * tracing headers (milliseconds, microseconds and nanoseconds are seen in the
 * wild across deployments).
 */
public class Test_ServerTracing {

    private static final long NOW_MS = 1793792431000L;

    @Test
    public void testNormalizeTimestampUnits() {
        assertEquals(NOW_MS, ServerTracing.normalizeToMillis(NOW_MS, NOW_MS));
        assertEquals(NOW_MS + 1500L, ServerTracing.normalizeToMillis(NOW_MS + 1500L, NOW_MS));
        assertEquals(NOW_MS, ServerTracing.normalizeToMillis(NOW_MS * 1000L, NOW_MS));
        assertEquals(1793792431342L, ServerTracing.normalizeToMillis(1793792431342482L, NOW_MS));
        assertEquals(NOW_MS, ServerTracing.normalizeToMillis(NOW_MS * 1_000_000L, NOW_MS));
        assertEquals(-1L, ServerTracing.normalizeToMillis(0L, NOW_MS));
        assertEquals(-1L, ServerTracing.normalizeToMillis(-1L, NOW_MS));
    }
}
