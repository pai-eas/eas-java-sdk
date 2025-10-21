package com.aliyun.openservices.eas.predict.utils;
import org.xerial.snappy.Snappy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Utility class for Snappy compression and decompression
 */
public abstract class SnappyUtils {
    /**
     * Compress byte array using Snappy
     * @param data The input byte array
     * @return Compressed byte array
     */
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

    /**
     * Compress string using Snappy
     * @param data The input string
     * @return Compressed byte array
     */
    public static byte[] compress(String data) {
        return compress(data.getBytes());
    }

    /**
     * Compress string using Snappy with specified encoding
     * @param data The input string
     * @param encoding The character encoding
     * @return Compressed byte array
     * @throws UnsupportedEncodingException
     */
    public static byte[] compress(String data, String encoding) throws UnsupportedEncodingException {
        return compress(data.getBytes(encoding));
    }

    /**
     * Decompress Snappy compressed byte array to string
     * @param data The compressed byte array
     * @return Decompressed string
     */
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

    /**
     * Decompress Snappy compressed byte array to string with specified encoding
     * @param data The compressed byte array
     * @param encoding The character encoding
     * @return Decompressed string
     * @throws UnsupportedEncodingException
     */
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
    
    /**
     * Decompress Snappy compressed byte array to byte array
     * @param data The compressed byte array
     * @return Decompressed byte array
     */
    public static byte[] decompressToBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        try {
            return Snappy.uncompress(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}