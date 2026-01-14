package com.dv.config.api.entity;

/**
 * 配置定义
 */
public interface ConfigDefinition {
    /**
     * 命名空间
     * @return 命名空间
     */
    String getNamespace();

    /**
     * 键
     * @return 键
     */
    String getKey();

    /**
     * 值
     * @return 值
     */
    String getValue();

    /**
     * 是否启用
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 是否加密
     * @return 是否加密
     */
    boolean isEncrypted();

    /**
     * 描述
     * @return 描述
     */
    String getDescription();
}
