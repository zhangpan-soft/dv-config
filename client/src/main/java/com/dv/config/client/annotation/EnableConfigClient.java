package com.dv.config.client.annotation;

import com.dv.config.client.config.ConfigClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用配置客户端
 * 自动配置 Netty 客户端和配置刷新监听器
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ConfigClientConfiguration.class)
public @interface EnableConfigClient {
}
