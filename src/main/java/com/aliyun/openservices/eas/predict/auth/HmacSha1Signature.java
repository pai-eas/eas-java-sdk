package com.aliyun.openservices.eas.predict.auth;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by yaozheng.wyz on 2017/10/31.
 */
public class HmacSha1Signature {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String DEFAULT_ENCODING = "UTF-8";

    public String computeSignature(String key, String data) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(DEFAULT_ENCODING), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            return new String(Base64.encodeBase64(mac.doFinal(data.getBytes(DEFAULT_ENCODING))));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("not supported encode: " + DEFAULT_ENCODING);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("not supported algorithm: " + HMAC_SHA1_ALGORITHM);
        } catch (InvalidKeyException ex) {
            throw new RuntimeException("invalid key");
        }
    }

    public String getMD5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(content);
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            throw new RuntimeException("MD5 encoder error");
        }
    }
}
