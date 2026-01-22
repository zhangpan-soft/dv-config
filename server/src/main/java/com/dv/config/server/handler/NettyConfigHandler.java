package com.dv.config.server.handler;

import com.dv.config.api.dto.ConfigDTO;
import com.dv.config.server.entity.ConfigDefinition;
import com.dv.config.server.event.NettyConfigRefreshEvent;
import com.dv.config.server.gateway.ConfigGateway;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class NettyConfigHandler {
    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private ConfigGateway configGateway;

    /**
     * 获取配置
     * @param namespaces 命名空间
     * @return 配置
     */
    public Map<String, TreeMap<String, String>> getConfigs(String... namespaces) {
        List<ConfigDefinition> configs = configGateway.getConfigs(namespaces);
        Map<String, TreeMap<String, String>> result = new HashMap<>();
        for (ConfigDefinition config : configs) {
            result.computeIfAbsent(config.getNamespace(), k -> new TreeMap<>());
            result.get(config.getNamespace()).put(config.getKey(), config.getValue());
        }
        return result;
    }

    /**
     * 刷新缓存
     */
    public void refresh(String... namespaces) {
        if (namespaces==null || namespaces.length==0){
            return;
        }
        ConfigDTO configDTO = new ConfigDTO();
        Map<String, TreeMap<String, String>> configs = getConfigs(namespaces);
        configDTO.getConfigs().putAll(configs);
        applicationContext.publishEvent(new NettyConfigRefreshEvent(this, configDTO));
    }
}
