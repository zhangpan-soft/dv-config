package com.dv.config.server.gateway;

import com.dv.config.server.entity.ConfigDefinition;

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
}
