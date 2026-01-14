package com.dv.config.api.impl.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.entity.ConfigDefinition;
import com.dv.config.api.gateway.ConfigGateway;
import com.dv.config.api.impl.entity.Config;
import com.dv.config.api.impl.mapper.ConfigMapper;
import com.dv.config.common.JsonUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigGatewayImpl implements ConfigGateway {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ConfigMapper configMapper;

    private static final String CONFIG_KEY_PATTERN="com.dv.config.api.impl.gateway.ConfigGatewayImpl_config_%s";

    @Override
    public List<ConfigDefinition> getConfigs(String... namespaces) {
        if (namespaces==null || namespaces.length==0){
            return List.of();
        }
        List<ConfigDefinition> configs = new ArrayList<>();
        for (String namespace : namespaces) {
            String configKey = getConfigKey(namespace);
            if (stringRedisTemplate.hasKey(configKey)){
                Map<String, String> entries = stringRedisTemplate.<String, String>opsForHash().entries(configKey);
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    String value = entry.getValue();
                    Config config = JsonUtil.fromJson(value, Config.class);
                    configs.add(config);
                }
            } else {
                List<Config> configList = configMapper.selectList(Wrappers.lambdaQuery(Config.class).in(Config::getNamespace, Arrays.stream(namespaces).toList()));
                configs.addAll(configList);
                stringRedisTemplate.opsForHash().putAll(configKey, configList.stream().collect(
                                java.util.stream.Collectors.toMap(Config::getKey, JsonUtil::toJson)));
            }

        }
        return configs;
    }

    @Override
    public boolean refresh(String... namespaces) {
        if (namespaces==null || namespaces.length==0){
            return false;
        }
        stringRedisTemplate.delete(Arrays.stream(namespaces).map(this::getConfigKey).toList());
        getConfigs(namespaces);
        return true;
    }

    @Override
    public void saves(List<ConfigDefinition> configs) {
        List<Config> configList = configMapper.selectList(Wrappers
                .lambdaQuery(Config.class)
                .in(Config::getNamespace, configs.stream().map(ConfigDefinition::getNamespace).toList())
                .in(Config::getKey, configs.stream().map(ConfigDefinition::getKey).toList())
        );
        Map<String, Config> collect = configList.stream().collect(Collectors.toMap(e-> e.getNamespace()+"$" + e.getKey(), e->e));
        List<Config> updateList = new ArrayList<>();
        for (ConfigDefinition config : configs) {
            Config updateConfig  = collect.get(config.getNamespace()+"$" + config.getKey());
            if (updateConfig == null){
                updateConfig = new Config();
            }
            updateConfig.setEnabled(config.isEnabled());
            updateConfig.setEncrypted(config.isEncrypted());
            updateConfig.setValue(config.getValue());
            updateConfig.setDescription(config.getDescription());
            updateConfig.setNamespace(config.getNamespace());
            updateConfig.setKey(config.getKey());
            updateList.add(updateConfig);
        }
        configMapper.insertOrUpdate(updateList, 1000);
        refresh(configs.stream().map(ConfigDefinition::getNamespace).distinct().toArray(String[]::new));
    }

    private String getConfigKey(String namespace){
        return String.format(CONFIG_KEY_PATTERN, namespace);
    }
}
