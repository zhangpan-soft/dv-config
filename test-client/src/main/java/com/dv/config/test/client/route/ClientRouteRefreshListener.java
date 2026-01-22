package com.dv.config.test.client.route;

import com.dv.config.client.event.RouteRefreshEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientRouteRefreshListener implements ApplicationListener<RouteRefreshEvent> {
    @Resource
    private ApplicationContext applicationContext;
    @Override
    public void onApplicationEvent(RouteRefreshEvent event) {
        log.info("收到路由刷新事件>>>>>>>>>>>");
        applicationContext.publishEvent(new RefreshRoutesEvent(this));
    }
}
