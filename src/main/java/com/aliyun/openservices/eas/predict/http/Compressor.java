package com.aliyun.openservices.eas.predict.http;

/**
 * Compression algorithms supported by PredictClient
 */
public enum Compressor {
    Gzip,
    Zlib,
    Snappy,
    LZ4,
    LZ4Frame,
    Zstd,
    Auto  // Automatically detect Content-Encoding and decompress
}