package com.dv.config.common.crypto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密配置属性
 */
@Data
public class CryptoProperties {
    
    /**
     * 是否启用加密功能
     */
    private boolean enabled = true;
    
    /**
     * 主密钥（从环境变量读取）
     */
    private String masterKey;
    
    /**
     * 当前密钥版本
     */
    private String currentVersion = "v1";
    
    /**
     * 多版本密钥（支持密钥轮换）
     * key: 版本号（如 v1, v2）
     * value: 密钥
     */
    private Map<String, String> keys = new HashMap<>();
    
    /**
     * 加密算法
     */
    private String algorithm = "AES-256-GCM";
    
    /**
     * PBKDF2 迭代次数
     */
    private int iterations = 65536;
    
    /**
     * 获取当前版本的密钥
     */
    public String getCurrentKey() {
        // 优先从 keys 中获取当前版本的密钥
        if (keys.containsKey(currentVersion)) {
            return keys.get(currentVersion);
        }
        // 兼容单密钥配置
        return masterKey;
    }
    
    /**
     * 获取指定版本的密钥
     */
    public String getKey(String version) {
        if (keys.containsKey(version)) {
            return keys.get(version);
        }
        // 如果版本是当前版本，返回 masterKey
        if (currentVersion.equals(version)) {
            return masterKey;
        }
        throw new IllegalArgumentException("密钥版本不存在: " + version);
    }
}
