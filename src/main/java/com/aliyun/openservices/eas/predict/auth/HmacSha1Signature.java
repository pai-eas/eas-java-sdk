package com.aliyun.openservices.eas.predict.auth;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by yaozheng.wyz on 2017/10/31.
 */
public class HmacSha1Signature {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public String computeSignature(String key, String data) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            return new String(Base64.encodeBase64(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8))), StandardCharsets.US_ASCII);
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
            // BigInteger.toString(16) drops leading zero nibbles; the digest
            // must always be a fixed 32-character hex string.
            return String.format("%032x", new BigInteger(1, md.digest()));
        } catch (Exception e) {
            throw new RuntimeException("MD5 encoder error");
        }
    }
}
