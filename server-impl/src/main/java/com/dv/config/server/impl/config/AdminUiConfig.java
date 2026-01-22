package com.dv.config.server.impl.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "config.admin.ui", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = "com.dv.config.server.impl.controller")
public class AdminUiConfig {
}
