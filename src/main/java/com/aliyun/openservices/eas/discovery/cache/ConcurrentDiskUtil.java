package com.aliyun.openservices.eas.discovery.cache;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class ConcurrentDiskUtil {

    static final public com.taobao.middleware.logger.Logger log = DiscoveryClient.LOG;
    static final int RETRY_COUNT = 10;
    static final int SLEEP_BASETIME = 10;

    /**
     * get file content
     *
     * @param path        file path
     * @param charsetName charsetName
     * @return content
     * @throws IOException IOException
     */
    public static String getFileContent(String path, String charsetName)
            throws IOException {
        File file = new File(path);
        return getFileContent(file, charsetName);
    }

    /**
     * get file content
     *
     * @param file        file
     * @param charsetName charsetName
     * @return content
     * @throws IOException IOException
     */
    public static String getFileContent(File file, String charsetName)
            throws IOException {

        try (RandomAccessFile fis = new RandomAccessFile(file, "r");
             FileChannel fcin = fis.getChannel()) {

            int count = 0;
            do {
                FileLock lock = null;
                try {
                    lock = fcin.tryLock(0L, Long.MAX_VALUE, true);
                    if (lock == null) {
                        throw new IOException("file lock is held by another process");
                    }
                    long size = fcin.size();
                    if (size > Integer.MAX_VALUE) {
                        throw new IOException("cache file is too large: " + size);
                    }
                    ByteBuffer byteBuffer = ByteBuffer.allocate((int) size);
                    while (byteBuffer.hasRemaining()) {
                        int read = fcin.read(byteBuffer);
                        if (read < 0) {
                            break;
                        }
                    }
                    byteBuffer.flip();
                    return byteBufferToString(byteBuffer, charsetName);

                } catch (OverlappingFileLockException e) {
                    sleep(SLEEP_BASETIME * count);
                    log.warn("read " + file.getName() + " conflict;retry time: " + count, e);
                } catch (IOException e) {
                    sleep(SLEEP_BASETIME * count);
                    log.warn("read " + file.getName() + " conflict;retry time: " + count, e);
                } finally {
                    releaseQuietly(lock, file);
                }
            } while (++count < RETRY_COUNT);

            log.error("NA", "read " + file.getName() + " fail;retried time: " + count);
            throw new IOException("read " + file.getAbsolutePath()
                    + " conflict");
        }
    }

    /**
     * write file content
     *
     * @param path        file path
     * @param content     content
     * @param charsetName charsetName
     * @return whether write ok
     * @throws IOException IOException
     */
    public static Boolean writeFileContent(String path, String content,
                                           String charsetName) throws IOException {
        File file = new File(path);
        return writeFileContent(file, content, charsetName);
    }

    /**
     * write file content
     *
     * @param file        file
     * @param content     content
     * @param charsetName charsetName
     * @return whether write ok
     * @throws IOException IOException
     */
    public static Boolean writeFileContent(File file, String content,
                                           String charsetName) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("failed to create parent directory for " + file.getAbsolutePath());
        }
        if (!file.exists() && !file.createNewFile() && !file.exists()) {
            throw new IOException(String.format(
                    "unable to find target file:%s and failed to create this file again", file.getName()));
        }

        byte[] bytes = content.getBytes(charsetName);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel()) {

            int i = 0;
            do {
                FileLock lock = null;
                try {
                    lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("file lock is held by another process");
                    }
                    channel.truncate(0L);
                    channel.position(0L);
                    ByteBuffer sendBuffer = ByteBuffer.wrap(bytes);
                    while (sendBuffer.hasRemaining()) {
                        channel.write(sendBuffer);
                    }
                    // Truncate by the number of encoded bytes, not by the
                    // number of characters in the String.
                    channel.truncate(bytes.length);
                    channel.force(false);
                    return true;

                } catch (OverlappingFileLockException e) {
                    sleep(SLEEP_BASETIME * i);
                    log.warn("write " + file.getName() + " conflict;retry time: " + i, e);
                } catch (IOException e) {
                    sleep(SLEEP_BASETIME * i);
                    log.warn("write " + file.getName() + " conflict;retry time: " + i, e);
                } finally {
                    releaseQuietly(lock, file);
                }
            } while (++i < RETRY_COUNT);

            log.error("NA", "write " + file.getName() + " fail;retryed time: " + i);
            throw new IOException("write " + file.getAbsolutePath()
                    + " conflict");

        }
    }

    private static void releaseQuietly(FileLock lock, File file) {
        if (lock == null) {
            return;
        }
        try {
            lock.release();
        } catch (IOException e) {
            log.warn("failed to release lock for " + file.getName(), e);
        }
    }

    /**
     * transfer ByteBuffer to String
     *
     * @param buffer      buffer
     * @param charsetName charsetName
     * @return String
     * @throws IOException IOException
     */
    public static String byteBufferToString(ByteBuffer buffer,
                                            String charsetName) throws IOException {
        Charset charset;
        CharsetDecoder decoder;
        CharBuffer charBuffer;
        charset = Charset.forName(charsetName);
        decoder = charset.newDecoder();
        charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
        return charBuffer.toString();
    }

    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("sleep wrong", e);
        }
    }
}
