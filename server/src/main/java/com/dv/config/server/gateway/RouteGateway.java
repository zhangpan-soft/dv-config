package com.dv.config.server.gateway;

import com.dv.config.server.entity.RouteDefinition;

import java.util.List;

/**
 * 路由网关
 */
public interface RouteGateway {
    /**
     * 获取路由
     * @return 路由
     */
    List<RouteDefinition> getRoutes();
}
