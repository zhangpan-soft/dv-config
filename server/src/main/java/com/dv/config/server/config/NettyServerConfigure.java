package com.dv.config.server.config;

import com.dv.config.common.crypto.CryptoProperties;
import com.dv.config.common.netty.NettyServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.dv.config.server")
public class NettyServerConfigure {

    @Bean
    @ConfigurationProperties(prefix = "netty.crypto")
    public CryptoProperties cryptoProperties() {
        return new CryptoProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "netty.server")
    public NettyServerProperties nettyServerProperties(CryptoProperties cryptoProperties) {
        NettyServerProperties nettyServerProperties = new NettyServerProperties();
        nettyServerProperties.setCrypto(cryptoProperties);
        return nettyServerProperties;
    }
}