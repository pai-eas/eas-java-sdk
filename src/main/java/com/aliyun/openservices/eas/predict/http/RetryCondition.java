package com.aliyun.openservices.eas.predict.http;

public enum RetryCondition {
    CONNECTION_FAILED,
    CONNECTION_TIMEOUT,
    READ_TIMEOUT,
    RESPONSE_5XX,
    RESPONSE_4XX
}
