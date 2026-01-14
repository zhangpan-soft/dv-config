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
}
