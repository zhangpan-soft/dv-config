package com.dv.config.api.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 哈希工具类
 * 提供常用的哈希算法（SHA-1、SHA-256、MD5）
 */
public class HashUtil {

    /**
     * 计算字符串的 SHA-1 值
     * @param input 输入字符串
     * @return SHA-1 哈希值（40位十六进制字符串）
     */
    public static String sha1(String input) {
        if (input == null) {
            return null;
        }
        return hash(input, "SHA-1");
    }

    /**
     * 计算字节数组的 SHA-1 值
     * @param bytes 输入字节数组
     * @return SHA-1 哈希值（40位十六进制字符串）
     */
    public static String sha1(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return hash(bytes, "SHA-1");
    }

    /**
     * 计算字符串的 SHA-256 值
     * @param input 输入字符串
     * @return SHA-256 哈希值（64位十六进制字符串）
     */
    public static String sha256(String input) {
        if (input == null) {
            return null;
        }
        return hash(input, "SHA-256");
    }

    /**
     * 计算字符串的 MD5 值
     * @param input 输入字符串
     * @return MD5 哈希值（32位十六进制字符串）
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        return hash(input, "MD5");
    }

    /**
     * 通用哈希方法（字符串）
     * @param input 输入字符串
     * @param algorithm 算法名称（SHA-1、SHA-256、MD5等）
     * @return 哈希值（十六进制字符串）
     */
    private static String hash(String input, String algorithm) {
        return hash(input.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    /**
     * 通用哈希方法（字节数组）
     * @param bytes 输入字节数组
     * @param algorithm 算法名称（SHA-1、SHA-256、MD5等）
     * @return 哈希值（十六进制字符串）
     */
    private static String hash(byte[] bytes, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(bytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " algorithm not found", e);
        }
    }
}
