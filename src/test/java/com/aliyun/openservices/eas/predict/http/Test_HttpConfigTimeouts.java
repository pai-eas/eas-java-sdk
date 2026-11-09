package com.aliyun.openservices.eas.predict.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the relationship between the transport read timeout and the
 * SDK-level request timeout: an explicitly configured read timeout wins,
 * otherwise the SDK derives one that lets the request timeout fire first.
 */
public class Test_HttpConfigTimeouts {

    @Test
    public void testRequestTimeoutDerivesReadTimeout() {
        HttpConfig config = new HttpConfig();
        assertFalse(config.isReadTimeoutExplicitlySet());
        assertEquals(5000, config.getEffectiveReadTimeout());

        config.setRequestTimeout(30000);
        config.setConnectTimeout(2000);

        assertEquals(5000, config.getReadTimeout());
        assertEquals(32000, config.getEffectiveReadTimeout());
    }

    @Test
    public void testExplicitReadTimeoutWins() {
        HttpConfig config = new HttpConfig();
        config.setReadTimeout(60000);
        config.setRequestTimeout(30000);
        assertTrue(config.isReadTimeoutExplicitlySet());
        assertEquals(60000, config.getEffectiveReadTimeout());

        // an explicitly smaller value is respected as-is
        config.setReadTimeout(2000);
        assertEquals(2000, config.getEffectiveReadTimeout());

        // the constructor form also counts as explicit
        HttpConfig constructed = new HttpConfig(10, 5000, 5000, 1000, 1000, 30000);
        assertTrue(constructed.isReadTimeoutExplicitlySet());
        assertEquals(5000, constructed.getEffectiveReadTimeout());
    }
}
