package com.dv.config.server.event;

import com.dv.config.api.event.ConfigPublishEvent;
import com.dv.config.api.event.RoutePublishEvent;
import com.dv.config.server.handler.NettyConfigHandler;
import com.dv.config.server.handler.NettyRouteHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听管理端发布事件，触发 Netty 推送
 */
@Slf4j
@Component
public class PublishEventListener {

    @Resource
    private NettyConfigHandler nettyConfigHandler;

    @Resource
    private NettyRouteHandler nettyRouteHandler;

    @EventListener
    public void onConfigPublish(ConfigPublishEvent event) {
        log.info("收到配置发布事件，触发 Netty 推送，命名空间: {}", event.getNamespaces());
        nettyConfigHandler.refresh(event.getNamespaces().toArray(new String[0]));
    }

    @EventListener
    public void onRoutePublish(RoutePublishEvent event) {
        log.info("收到路由发布事件，触发 Netty 推送");
        nettyRouteHandler.refresh();
    }
}
