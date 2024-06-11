package com.aliyun.openservices.eas.predict.utils;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class LZ4Utils {

    private static final LZ4Factory factory = LZ4Factory.fastestInstance();

    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        final LZ4Compressor compressor = factory.fastCompressor();
        final int maxCompressedLength = compressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);
        byte[] trimmed = new byte[compressedLength];
        System.arraycopy(compressed, 0, trimmed, 0, compressedLength);
        return trimmed;
    }

    public static byte[] decompress(byte[] compressedData, int originalLength) {
        if (compressedData == null || compressedData.length == 0) {
            return null;
        }
        final LZ4FastDecompressor decompressor = factory.fastDecompressor();
        byte[] restored = new byte[originalLength];
        decompressor.decompress(compressedData, 0, restored, 0, originalLength);
        return restored;
    }
}