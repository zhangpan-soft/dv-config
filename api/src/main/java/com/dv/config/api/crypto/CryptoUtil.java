package com.dv.config.api.crypto;

import com.dv.config.api.json.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 加密解密工具类
 */
@Slf4j
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;

    /**
     * 加密配置值
     *
     * @param plaintext  明文
     * @param password   密码
     * @param iterations PBKDF2 迭代次数
     * @return 加密后的 JSON 字符串
     */
    public static String encrypt(String plaintext, String password, int iterations) {
        if (!StringUtils.hasText(plaintext) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("明文和密码不能为空");
        }

        try {
            // 1. 生成随机 IV 和 Salt
            byte[] iv = generateRandomBytes(IV_LENGTH);
            byte[] salt = generateRandomBytes(SALT_LENGTH);

            // 2. 派生密钥
            SecretKey secretKey = deriveKey(password, salt, iterations);

            // 3. 加密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 4. 构造加密值 JSON
            Map<String, String> encryptedData = new HashMap<>();
            encryptedData.put("algorithm", ALGORITHM);
            encryptedData.put("iv", Base64.getEncoder().encodeToString(iv));
            encryptedData.put("salt", Base64.getEncoder().encodeToString(salt));
            encryptedData.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));

            return JsonUtil.toJson(encryptedData);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密配置值
     *
     * @param encryptedJson 加密的 JSON 字符串
     * @param password      密码
     * @param iterations    PBKDF2 迭代次数
     * @return 明文
     */
    public static String decrypt(String encryptedJson, String password, int iterations) {
        if (!StringUtils.hasText(encryptedJson) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("加密数据和密码不能为空");
        }

        try {
            // 1. 解析加密值 JSON
            Map<String, String> encryptedData = JsonUtil.fromJson(
                encryptedJson,
                new TypeReference<>() {}
            );

            byte[] iv = Base64.getDecoder().decode(encryptedData.get("iv"));
            byte[] salt = Base64.getDecoder().decode(encryptedData.get("salt"));
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData.get("ciphertext"));

            // 2. 派生密钥
            SecretKey secretKey = deriveKey(password, salt, iterations);

            // 3. 解密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否为加密值
     *
     * @param value 配置值
     * @return 是否为加密值
     */
    public static boolean isEncrypted(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            Map<String, String> data = JsonUtil.fromJson(value, new TypeReference<>() {});
            return data.containsKey("algorithm") &&
                   data.containsKey("iv") &&
                   data.containsKey("salt") &&
                   data.containsKey("ciphertext");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 派生密钥（PBKDF2）
     */
    private static SecretKey deriveKey(String password, byte[] salt, int iterations) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            256  // AES-256
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * 生成随机字节
     */
    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
