package com.aliyun.openservices.eas.predict.utils;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for ZSTD compression and decompression
 */
public abstract class ZstdUtils {

    /**
     * Compress byte array using ZSTD
     * @param data The input byte array
     * @return Compressed byte array
     */
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        byte[] compressed = Zstd.compress(data);
        return compressed;
    }

    /**
     * Compress byte array using ZSTD and write to output stream
     * @param data The input byte array
     * @param os The output stream
     * @throws IOException
     */
    public static void compress(byte[] data, OutputStream os) throws IOException {
        if (data == null || data.length == 0 || os == null) {
            return;
        }
        try (ZstdOutputStream zos = new ZstdOutputStream(os)) {
            zos.write(data);
            zos.flush();
        }
    }

    /**
     * Decompress ZSTD compressed byte array
     * @param data The compressed byte array
     * @return Decompressed byte array
     */
    public static byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return data;
        }
        long originalSize = Zstd.decompressedSize(data);
        if (originalSize == -1) {
            throw new IllegalArgumentException("Could not determine decompressed size");
        }
        byte[] decompressed = Zstd.decompress(data, (int) originalSize);
        return decompressed;
    }

    /**
     * Decompress ZSTD compressed input stream
     * @param is The compressed input stream
     * @return Decompressed byte array
     * @throws IOException
     */
    public static byte[] decompress(InputStream is) throws IOException {
        if (is == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream o = new ByteArrayOutputStream();
             ZstdInputStream zis = new ZstdInputStream(is)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = zis.read(buf)) > 0) {
                o.write(buf, 0, len);
            }
            return o.toByteArray();
        }
    }
}