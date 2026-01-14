package com.dv.config.api.impl.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.entity.RouteDefinition;
import com.dv.config.api.gateway.RouteGateway;
import com.dv.config.api.impl.entity.Route;
import com.dv.config.api.impl.mapper.RouteMapper;
import com.dv.config.common.JsonUtil;
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

    private static final String ROUTE_KEY = "com.dv.config.api.impl.gateway.RouteGatewayImpl_route";

    @Override
    public List<RouteDefinition> getRoutes() {
        List<RouteDefinition> list = new ArrayList<>();
        if (stringRedisTemplate.hasKey(ROUTE_KEY)) {
            List<Route> routes = JsonUtil.fromJson(stringRedisTemplate.opsForValue().get(ROUTE_KEY), new TypeReference<>() {
            });
            list.addAll(routes);
        } else {
            List<Route> routes = routeMapper.selectList(Wrappers.lambdaQuery(Route.class));
            list.addAll(routes);
            stringRedisTemplate.opsForValue().set(ROUTE_KEY, JsonUtil.toJson(routes));
        }
        return list;
    }

    @Override
    public boolean refresh() {
        stringRedisTemplate.delete(ROUTE_KEY);
        getRoutes();
        return true;
    }

    @Override
    public void saves(List<RouteDefinition> routes) {
        routeMapper.insertOrUpdate(routes
                .stream()
                .map(route -> new Route().setId(route.getId())
                        .setUri(route.getUri())
                        .setPredicates(route.getPredicates().stream().map(predicate -> new Route.Predicate().setName(predicate.getName()).setArgs(predicate.getArgs())).toList())
                        .setFilters(route.getFilters().stream().map(filter -> new Route.Filter().setName(filter.getName()).setArgs(filter.getArgs())).toList())
                        .setMetadata(route.getMetadata())
                        .setOrder(route.getOrder())
                        .setEnabled(route.isEnabled())
                        .setDescription(route.getDescription())
                )
                .toList()
        );
        refresh();
    }
}
