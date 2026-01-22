package com.dv.config.server.impl.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.server.entity.RouteDefinition;
import com.dv.config.server.gateway.RouteGateway;
import com.dv.config.server.impl.entity.Route;
import com.dv.config.server.impl.mapper.RouteMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RouteGatewayImpl implements RouteGateway {
    @Resource
    private RouteMapper routeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String ROUTE_KEY = "com.dv.config.server.impl.gateway.RouteGatewayImpl_route";

    @Override
    public List<RouteDefinition> getRoutes() {
        List<RouteDefinition> list = new ArrayList<>();
        if (stringRedisTemplate.hasKey(ROUTE_KEY)) {
            List<Route> routes = JsonUtil.fromJson(stringRedisTemplate.opsForValue().get(ROUTE_KEY), new TypeReference<>() {
            });
            list.addAll(routes);
        } else {
            List<Route> routes = routeMapper.selectList(Wrappers.lambdaQuery(Route.class).eq(Route::isEnabled, true));
            list.addAll(routes);
            stringRedisTemplate.opsForValue().set(ROUTE_KEY, JsonUtil.toJson(routes));
        }
        return list;
    }
}
