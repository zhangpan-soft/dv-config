package com.dv.config.client.netty;

import com.dv.config.client.config.DynamicPropertySource;
import com.dv.config.common.netty.NettyClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
public class NettyLoadRunListener implements SpringApplicationRunListener {
    private NettyClient nettyClient;

    public NettyLoadRunListener(SpringApplication application, String[] args){
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        if (log.isDebugEnabled()){
            log.debug("starting");
        }
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        log.info("NettyLoadRunListener: environmentPrepared - 开始加载远程配置");
        
        try {

            // 1. 从环境中绑定Netty客户端配置
            NettyClientProperties nettyClientProperties = Binder.get(environment)
                    .bind("netty.client", NettyClientProperties.class)
                    .orElse(new NettyClientProperties());
            log.info("Netty客户端配置加载完成");

            // 3. 创建 DynamicPropertySource

            DynamicPropertySource dynamicPropertySource = new DynamicPropertySource("dynamicConfig");
            // 4. 设置加密配置到 DynamicPropertySource
            dynamicPropertySource.setCryptoConfig(
                nettyClientProperties.getCrypto().getMasterKey(),
                nettyClientProperties.getCrypto().getIterations(),
                nettyClientProperties.getCrypto().isEnabled()
            );
            log.info("加密功能已启用: {}", nettyClientProperties.getCrypto().isEnabled());
            
            // 5. 注册到 Environment
            environment.getPropertySources().addFirst(dynamicPropertySource);
            log.info("DynamicPropertySource 已注册到 Environment（优先级最高）");

            // 6. 初始化 Netty 客户端并加载远程配置
            log.info("初始化 Netty 配置客户端，服务器: {}:{}", nettyClientProperties.getHost(), nettyClientProperties.getPort());

            if (nettyClientProperties.isHasRoute()){
                DynamicPropertySource routePropertySource = new DynamicPropertySource("routePropertySource");
                routePropertySource.setCryptoConfig(null,0, false);
                environment.getPropertySources().addFirst(routePropertySource);
                nettyClient = new NettyClient(nettyClientProperties, routePropertySource, new RouteLoadHandler(routePropertySource));
                log.info("RoutePropertySource 已注册到 Environment（优先级最高）");
            } else {
                nettyClient = new NettyClient(nettyClientProperties, dynamicPropertySource);
            }

            
        } catch (Exception e) {
            log.error("加载远程配置失败，应用启动中止", e);
            // 启动时连接不到config-server，直接启动失败
            throw new RuntimeException("远程配置加载失败，应用启动中止:无法连接到配置服务", e);
        }
    }
    
    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        nettyClient.setApplicationContext(context);
        context.getBeanFactory().registerSingleton("nettyClient", nettyClient);
    }
    
    @Override
    public void started(ConfigurableApplicationContext context, java.time.Duration timeTaken) {
        log.info("NettyLoadRunListener: started - 应用启动完成，耗时: {}", timeTaken);
        
        // 标记启动阶段完成，从现在开始连接断开不会影响服务运行
        if (nettyClient != null) {
            nettyClient.markStartupComplete();
        }
    }
    
    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        log.error("NettyLoadRunListener: failed - 应用启动失败", exception);
        // 清理资源
        if (nettyClient != null) {
            try {
                nettyClient.destroy();
            } catch (Exception e) {
                log.error("关闭 NettyClient 失败", e);
            }
        }
    }
}