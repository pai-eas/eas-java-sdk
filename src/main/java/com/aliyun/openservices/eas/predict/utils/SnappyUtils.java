package com.aliyun.openservices.eas.predict.utils;
import org.xerial.snappy.Snappy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class SnappyUtils {
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        try {
            return Snappy.compress(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] compress(String data) {
        return compress(data.getBytes());
    }

    public static byte[] compress(String data, String encoding) throws UnsupportedEncodingException {
        return compress(data.getBytes(encoding));
    }

    public static String decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return new String();
        }
        try {
            byte[] uncompressed = Snappy.uncompress(data);
            return new String(uncompressed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decompress(byte[] data, String encoding) throws UnsupportedEncodingException {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            byte[] uncompressed = Snappy.uncompress(data);
            return new String(uncompressed, encoding);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}