package com.aliyun.openservices.eas.predict.utils;

import net.jpountz.lz4.*;
import net.jpountz.lz4.LZ4FrameOutputStream.BLOCKSIZE;
import net.jpountz.lz4.LZ4FrameOutputStream.FLG;
import net.jpountz.xxhash.XXHashFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility class for LZ4 compression and decompression supporting both block and frame formats.
 */
public class LZ4Utils {

    private static final LZ4Factory factory = LZ4Factory.fastestInstance();
    private static final XXHashFactory xxHashFactory = XXHashFactory.fastestInstance();

    // LZ4 Frame Magic Number (for identification)
    private static final int LZ4F_MAGIC_NUMBER = 0x184D2204;

    /**
     * Compress byte array using LZ4 block format.
     * @param data The input byte array
     * @return Compressed byte array in block format
     */
    public static byte[] compressBlock(byte[] data) {
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

    /**
     * Decompress LZ4 block compressed byte array.
     * @param compressedData The compressed byte array in block format
     * @param originalLength The original length of data
     * @return Decompressed byte array
     */
    public static byte[] decompressBlock(byte[] compressedData, int originalLength) {
        if (compressedData == null || compressedData.length == 0) {
            return null;
        }
        final LZ4FastDecompressor decompressor = factory.fastDecompressor();
        byte[] restored = new byte[originalLength];
        decompressor.decompress(compressedData, 0, restored, 0, originalLength);
        return restored;
    }

    /**
     * Compress byte array using LZ4 frame format.
     * @param data The input byte array
     * @return Compressed byte array in frame format
     * @throws IOException if an I/O error occurs
     */
    public static byte[] compressFrame(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return data;
        }
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             LZ4FrameOutputStream lz4Out = new LZ4FrameOutputStream(
                 byteOut,
                 BLOCKSIZE.SIZE_64KB,
                 -1, // default compression level
                 FLG.Bits.BLOCK_CHECKSUM,
                 FLG.Bits.CONTENT_CHECKSUM,
                 FLG.Bits.BLOCK_INDEPENDENCE,
                 FLG.Bits.BLOCK_INDEPENDENCE)) {
            lz4Out.write(data);
            lz4Out.close();
            return byteOut.toByteArray();
        }
    }

    /**
     * Decompress LZ4 frame compressed byte array.
     * @param compressedData The compressed byte array in frame format
     * @return Decompressed byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] decompressFrame(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return null;
        }
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(compressedData);
             LZ4FrameInputStream lz4In = new LZ4FrameInputStream(byteIn);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = lz4In.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * Compress byte array using LZ4 block format.
     * @param data The input byte array
     * @return Compressed byte array in block format
     */
    public static byte[] compress(byte[] data) {
         return compressBlock(data);
    }

    /**
     * Automatically detect format (block or frame) and decompress.
     * @param compressedData The compressed byte array
     * @param originalLength Required for block format, ignored for frame format
     * @return Decompressed byte array
     * @throws IOException if an I/O error occurs or format is invalid
     */
    public static byte[] decompress(byte[] compressedData, Integer originalLength) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return null;
        }
        // Check for LZ4 frame magic number (first 4 bytes)
        if (isFrameFormat(compressedData)) {
            return decompressFrame(compressedData);
        } else {
            if (originalLength == null) {
                throw new IllegalArgumentException("Original length must be provided for block format decompression");
            }
            return decompressBlock(compressedData, originalLength);
        }
    }

    /**
     * Check if the compressed data is in LZ4 frame format.
     * @param data Compressed data
     * @return true if frame format, false if block format
     */
    private static boolean isFrameFormat(byte[] data) {
        if (data.length < 4) {
            return false; // Too short to be a frame
        }
        // Read first 4 bytes as little-endian integer
        int magic = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24);
        return magic == LZ4F_MAGIC_NUMBER;
    }
}
