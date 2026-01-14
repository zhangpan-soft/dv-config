package com.dv.config.api.gateway;

import com.dv.config.api.entity.RouteDefinition;

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
