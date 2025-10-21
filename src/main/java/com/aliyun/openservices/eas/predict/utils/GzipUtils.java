package com.aliyun.openservices.eas.predict.utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for GZIP compression and decompression
 */
public class GzipUtils {
    /**
     * Compress byte array using GZIP
     * @param str The input byte array
     * @return Compressed byte array
     */
    public static byte[] compress(byte[] str) {
        if (str == null || str.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
    
    /**
     * Compress string using GZIP
     * @param str The input string
     * @return Compressed byte array
     */
    public static byte[] compress(String str) {
        return compress(str.getBytes());
    }
    
    /**
     * Compress string using GZIP with specified encoding
     * @param str The input string
     * @param encoding The character encoding
     * @return Compressed byte array
     * @throws UnsupportedEncodingException
     */
    public static byte[] compress(String str, String encoding) throws UnsupportedEncodingException {
        return compress(str.getBytes(encoding));
    }
    
    /**
     * Decompress GZIP compressed byte array to string
     * @param b The compressed byte array
     * @return Decompressed string
     */
    public static String decompress(byte[] b) {
        if (b == null || b.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        try {
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }
    
    /**
     * Decompress GZIP compressed byte array to string with specified encoding
     * @param b The compressed byte array
     * @param encoding The character encoding
     * @return Decompressed string
     */
    public static String decompress(byte[] b, String encoding) {
        if (b == null || b.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        try {
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toString(encoding);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Decompress GZIP compressed byte array to byte array
     * @param b The compressed byte array
     * @return Decompressed byte array
     */
    public static byte[] decompressToBytes(byte[] b) {
        if (b == null || b.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        try {
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}