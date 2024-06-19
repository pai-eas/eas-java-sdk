package com.aliyun.openservices.eas.discovery.cache;

import com.aliyun.openservices.eas.discovery.core.DiscoveryClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
                try (FileLock ignored = fcin.tryLock(0L, Long.MAX_VALUE, true)) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate((int) fcin.size());
                    fcin.read(byteBuffer);
                    byteBuffer.flip();
                    return byteBufferToString(byteBuffer, charsetName);

                } catch (IOException e) {
                    sleep(SLEEP_BASETIME * count);
                    log.warn("read " + file.getName() + " conflict;retry time: " + count);
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
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException(String.format("unable to find target file:%s and failed to create this file again", file.getName()));
            }
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel()) {

            int i = 0;
            do {
                try (FileLock ignored = channel.tryLock()) {
                    ByteBuffer sendBuffer = ByteBuffer.wrap(content
                            .getBytes(charsetName));
                    while (sendBuffer.hasRemaining()) {
                        channel.write(sendBuffer);
                    }
                    channel.truncate(content.length());
                    return true;

                } catch (IOException e) {
                    sleep(SLEEP_BASETIME * i);
                    log.warn("write " + file.getName() + " conflict;retry time: " + i);
                }
            } while (++i < RETRY_COUNT);

            log.error("NA", "write " + file.getName() + " fail;retryed time: " + i);
            throw new IOException("write " + file.getAbsolutePath()
                    + " conflict");

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
            log.warn("sleep wrong", e);
        }
    }

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 10000; i++) {
                writeFileContent("D:/test.txt", "test\r\ntest1", "GBK");
                String abc = getFileContent("D:/test.txt", "GBK");
                if (!"test\r\ntest1".equals(abc)) {
                    System.out.println(abc);
                    System.out.println("diff");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
