package com.dv.config.test.listener;

import com.dv.config.client.event.RouteRefreshEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RouteRefreshListener implements ApplicationListener<RouteRefreshEvent> {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(@Nonnull RouteRefreshEvent event) {
        log.info("收到路由刷新事件，触发 Gateway 路由刷新");
        applicationContext.publishEvent(new RefreshRoutesEvent(this));
    }
}
