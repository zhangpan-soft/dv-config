package com.dv.config.test.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@ConfigurationProperties(prefix = "test.config")
@Data
@RefreshScope
public class TestConfigProperties {
    private String a;
    private int b;
    @NestedConfigurationProperty
    private TestNestedProperties nested;

    @Data
    public static class TestNestedProperties {
        private String c;
        private long d;
    }
}
