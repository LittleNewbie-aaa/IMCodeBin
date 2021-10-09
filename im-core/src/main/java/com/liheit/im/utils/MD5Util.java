package com.liheit.im.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
    private static final String TAG = MD5Util.class.getSimpleName();
    private static final int STREAM_BUFFER_LENGTH = 1024;

    public static MessageDigest getDigest(final String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    public static byte[] md5(String txt) {
        return md5(txt.getBytes());
    }

    public static byte[] md5(byte[] bytes) {
        try {
            MessageDigest digest = getDigest("MD5");
            digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] md5(InputStream is) throws NoSuchAlgorithmException, IOException {
        return updateDigest(getDigest("MD5"), is).digest();
    }

    public static MessageDigest updateDigest(final MessageDigest digest, final InputStream data) throws IOException {
        final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

        while (read > -1) {
            digest.update(buffer, 0, read);
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
        }
        data.close();

        return digest;
    }

    public static String md5Hex(InputStream is) throws IOException, NoSuchAlgorithmException {
        return toHexString(md5(is));
    }

    /**
     * 十六进制下数字到字符的映射数组
     */
    public static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    public static String toHexString(final byte[] bs) {
        final int len;
        if (bs != null && (len = bs.length) != 0) {
            final char[] cs = new char[len << 1];
            final char[] myDigits = DIGITS;
            byte b;
            for (int i = 0, j = 0; i < len; i++) {
                cs[j++] = myDigits[((b = bs[i]) >>> 4) & 0xF];
                cs[j++] = myDigits[b & 0xF];
            }
            return String.valueOf(cs);
        }
        return null;
    }
}
