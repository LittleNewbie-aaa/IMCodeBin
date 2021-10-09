package com.liheit.im.utils;

import android.util.Base64;

import java.nio.charset.Charset;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    public static final String DEFAULT_AES_INIT_PARAM = "123456ABCD!@#$%^";
    public static final String DEFAULT_AES_KEY = "123456ABCD!@#$%^";
    private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";//AES是加密方式 CBC是工作模式 PKCS5Padding是填充模式
    private static final String AES = "AES";//AES 加密


    // 增加  CryptoProvider  类

    public static byte[] encrypt(String content) throws Exception {
        // 创建AES秘钥
        SecretKeySpec key = new SecretKeySpec(DEFAULT_AES_KEY.getBytes(), AES);
        // 创建密码器
        Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
        AlgorithmParameterSpec algorithmParameterSpec = new IvParameterSpec(DEFAULT_AES_INIT_PARAM.getBytes());
        // 初始化加密器
        cipher.init(Cipher.ENCRYPT_MODE, key, algorithmParameterSpec);
        // 加密
        return cipher.doFinal(content.getBytes("UTF-8"));
    }

    public static String encryptToBase64String(String content) {
        // 创建AES秘钥
        SecretKeySpec key = new SecretKeySpec(DEFAULT_AES_KEY.getBytes(), AES);
        try {
            // 创建密码器
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            AlgorithmParameterSpec algorithmParameterSpec = new IvParameterSpec(DEFAULT_AES_INIT_PARAM.getBytes());
            // 初始化加密器
            cipher.init(Cipher.ENCRYPT_MODE, key, algorithmParameterSpec);
            // 加密
            return Base64.encodeToString(cipher.doFinal(content.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decrypt(byte[] content) throws Exception {
        // 创建AES秘钥
        SecretKeySpec key = new SecretKeySpec(DEFAULT_AES_KEY.getBytes(), AES);
        // 创建密码器
        Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
        // 初始化解密器
        AlgorithmParameterSpec algorithmParameterSpec = new IvParameterSpec(DEFAULT_AES_INIT_PARAM.getBytes());
        // 初始化加密器
        cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameterSpec);
        // 解密
        return cipher.doFinal(Base64.decode(content,Base64.NO_WRAP));
    }

    public static String decryptToString(String content) throws Exception {
        byte[] bytes = decrypt(content.getBytes("UTF-8"));
        return new String(bytes, Charset.forName("UTF-8"));
    }

}
