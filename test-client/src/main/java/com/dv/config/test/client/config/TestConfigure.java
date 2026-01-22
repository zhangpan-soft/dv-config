package com.dv.config.test.client.config;

import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TestConfigProperties.class)
public class TestConfigure {
    @Getter
    private static TestConfigProperties properties;

    public TestConfigure(TestConfigProperties properties) {
        TestConfigure.properties = properties;
    }
}
