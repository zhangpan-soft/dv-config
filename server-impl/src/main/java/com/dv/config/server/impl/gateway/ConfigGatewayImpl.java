package com.dv.config.server.impl.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.server.entity.ConfigDefinition;
import com.dv.config.server.gateway.ConfigGateway;
import com.dv.config.server.impl.entity.Config;
import com.dv.config.server.impl.mapper.ConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfigGatewayImpl implements ConfigGateway {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ConfigMapper configMapper;

    private static final String CONFIG_KEY_PATTERN="com.dv.config.server.impl.gateway.ConfigGatewayImpl_config_%s";

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
                List<Config> configList = configMapper.selectList(Wrappers.lambdaQuery(Config.class).in(Config::getNamespace, Arrays.stream(namespaces).toList()).eq(Config::isEnabled, true));
                configs.addAll(configList);
                stringRedisTemplate.opsForHash().putAll(configKey, configList.stream().collect(
                                java.util.stream.Collectors.toMap(Config::getKey, JsonUtil::toJson)));
            }

        }
        return configs;
    }

    private String getConfigKey(String namespace){
        return String.format(CONFIG_KEY_PATTERN, namespace);
    }
}
