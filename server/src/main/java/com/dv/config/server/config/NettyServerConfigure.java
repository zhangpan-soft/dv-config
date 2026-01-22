package com.dv.config.server.config;

import com.dv.config.api.crypto.CryptoProperties;
import com.dv.config.api.property.NettyServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
@ComponentScan("com.dv.config.server")
public class NettyServerConfigure {

    @Bean
    @ConfigurationProperties(prefix = "netty.server")
    public NettyServerProperties nettyServerProperties(CryptoProperties cryptoProperties) {
        NettyServerProperties nettyServerProperties = new NettyServerProperties();
        nettyServerProperties.setCrypto(cryptoProperties);
        return nettyServerProperties;
    }
}