package com.aliyun.openservices.eas.predict;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertTrue;

/**
 * Captures the SDK's HTTP tracing JSON log lines (logger
 * "com.aliyun.openservices.eas.predict.http.tracing") so tests can assert on
 * tracing output. Each captured line is parsed into a {@link JSONObject};
 * server-side phases absent from the response tracing headers are omitted.
 */
public class TracingLogCollector {

    public static final String LOGGER_NAME = "com.aliyun.openservices.eas.predict.http.tracing";

    private final List<JSONObject> events = new CopyOnWriteArrayList<JSONObject>();

    private final AppenderSkeleton appender = new AppenderSkeleton() {
        @Override
        protected void append(LoggingEvent event) {
            String message = event.getRenderedMessage();
            if (message != null && message.startsWith("{")) {
                events.add(JSONObject.parseObject(message));
            }
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
        }
    };

    public void install() {
        Logger logger = Logger.getLogger(LOGGER_NAME);
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
    }

    public void uninstall() {
        Logger.getLogger(LOGGER_NAME).removeAppender(appender);
    }

    public void clear() {
        events.clear();
    }

    public List<JSONObject> events() {
        return events;
    }

    /** waits up to 5s for the first tracing line and returns it */
    public JSONObject awaitEvent() throws InterruptedException {
        awaitEvents(1);
        return events.get(0);
    }

    /** waits up to 5s for at least n tracing lines */
    public void awaitEvents(int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (events.size() < n && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue("expected " + n + " tracing log line(s), got " + events.size(),
                events.size() >= n);
    }
}
