package com.dv.config.client.config;

import com.dv.config.common.crypto.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 可动态更新的PropertySource:基于ConcurrentHashMap实现
 */
@Slf4j
public class DynamicPropertySource extends MapPropertySource {

    // 用ConcurrentHashMap保证线程安全

    // 加密配置
    private String cryptoMasterKey;
    private int cryptoIterations = 65536;
    private boolean cryptoEnabled = true;

    public DynamicPropertySource(String name) {
        super(name, new ConcurrentHashMap<>());
    }

    /**
     * 设置加密配置
     */
    public void setCryptoConfig(String masterKey, int iterations, boolean enabled) {
        this.cryptoMasterKey = masterKey;
        this.cryptoIterations = iterations;
        this.cryptoEnabled = enabled;
    }

    /**
     * 添加/更新k-v
     * 如果值是加密的，自动解密
     */
    public void setProperty(String key, Object value) {
        Assert.notNull(key, "Key must not be null");
        
        // 如果是字符串且启用了加密，检查是否需要解密
        if (value instanceof String strValue && cryptoEnabled && StringUtils.hasText(cryptoMasterKey)) {
            if (CryptoUtil.isEncrypted(strValue)) {
                try {
                    // 解密
                    String decrypted = CryptoUtil.decrypt(strValue, cryptoMasterKey, cryptoIterations);
                    getSource().put(key, decrypted);
                    log.debug("配置 [{}] 已自动解密", key);
                    return;
                } catch (Exception e) {
                    log.error("配置 [{}] 解密失败，将使用原始值", key, e);
                }
            }
        }
        
        // 不需要解密或解密失败，直接保存
        getSource().put(key, value);
    }

}