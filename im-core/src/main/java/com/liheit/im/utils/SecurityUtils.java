package com.liheit.im.utils;

import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtils {
    /**
     * md5加密
     *
     * @param str
     * @return
     */

    private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    public final static String get32MD5Str(String str) {
        try {
            if (str == null || str.trim().length() == 0) {
                return "";
            }
            byte[] bytes = str.getBytes();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            bytes = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(HEX_DIGITS[(bytes[i] & 0xf0) >> 4] + ""
                        + HEX_DIGITS[bytes[i] & 0xf]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 加密传入的数据是byte类型的，并非使用decode方法将原始数据转二进制，String类型的数据 使用 str.getBytes()即可
     * 在这里使用的是encode方式，返回的是byte类型加密数据，可使用new String转为String类型
     *
     * @param str
     * @return
     */
    public final static String getBase64Encode(String str) {
        String strBase64 = new String(Base64.encode(str.getBytes(), Base64.DEFAULT));
        Log.e("Base64", "encode >>>" + strBase64);
        return strBase64;
    }

    /**
     * 这里 encodeToString 则直接将返回String类型的加密数据
     *
     * @param str
     * @return
     */
    public final static String getBase64EncodeToString(String str) {
        String enToStr = Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
        Log.e("Base64", "encodeToString >>> " + enToStr);
        return enToStr;
    }

    /**
     * 对base64加密后的数据进行解密
     *
     * @param strBase64
     * @return
     */
    public final static String getBase64Decode(String strBase64) {

        String str = new String(Base64.decode(strBase64.getBytes(), Base64.DEFAULT));
        Log.e("Base64", "decode >>>" + str);
        return str;
    }
}
