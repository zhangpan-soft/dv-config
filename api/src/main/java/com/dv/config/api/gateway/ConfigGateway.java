package com.dv.config.api.gateway;

import com.dv.config.api.entity.ConfigDefinition;

import java.util.List;

/**
 * 配置网关
 */
public interface ConfigGateway {
    /**
     * 获取配置
     * @param namespaces 命名空间
     * @return 配置
     */
    List<ConfigDefinition> getConfigs(String... namespaces);

    /**
     * 刷新缓存
     * @param namespaces 命名空间
     * @return 是否刷新成功
     */
    boolean refresh(String... namespaces);

    /**
     * 保存配置
     * @param configs 配置
     */
    void saves(List<ConfigDefinition> configs);
}
