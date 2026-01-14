package com.dv.config.client.config;

import com.dv.config.common.crypto.CryptoProperties;
import com.dv.config.common.netty.NettyClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置客户端自动配置类
 * 注:NettyClient 和 DynamicPropertySource 已由 NettyLoadRunListener 在Bean初始化之前创建并注册
 */
@Slf4j
@Configuration
public class ConfigClientConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "netty.crypto")
    public CryptoProperties cryptoProperties() {
        return new CryptoProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "netty.client")
    public NettyClientProperties nettyClientProperties(CryptoProperties cryptoProperties) {
        NettyClientProperties nettyServerProperties = new NettyClientProperties();
        nettyServerProperties.setCrypto(cryptoProperties);
        return nettyServerProperties;
    }


}
