package com.dv.config.api.impl.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "dv.config.admin-ui", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.dv.config.api.impl.controller")
public class AdminUiConfig {
}
