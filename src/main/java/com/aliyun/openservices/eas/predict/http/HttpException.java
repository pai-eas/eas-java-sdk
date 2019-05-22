package com.aliyun.openservices.eas.predict.http;

import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * Created by yaozheng.wyz on 2019/5/21.
 */
public class HttpException extends IOException {
    private int code;
    private String message;

    public HttpException() {
        super();
    }

    public HttpException(int code, String message) {
        super("Status Code: " + code + " Predict Failed:" + message);
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    public int getCode() { return code; }
}
