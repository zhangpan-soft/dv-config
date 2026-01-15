package com.dv.config.server.handler;


import com.dv.config.api.entity.RouteDefinition;
import com.dv.config.api.gateway.RouteGateway;
import com.dv.config.server.convertor.RouteConvertor;
import com.dv.config.server.event.NettyRouteRefreshEvent;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class NettyRouteHandler {
    @Resource
    private RouteGateway routeGateway;
    @Resource
    private ApplicationContext applicationContext;

    public List<RouteDefinition> getRoutes() {
        return routeGateway.getRoutes().stream().sorted(Comparator.comparingInt(RouteDefinition::getOrder)).toList();
    }

    public void refresh() {
        applicationContext.publishEvent(new NettyRouteRefreshEvent(this, getRoutes().stream().map(RouteConvertor.INSTANCE::toDTO).toList()));
    }
}
