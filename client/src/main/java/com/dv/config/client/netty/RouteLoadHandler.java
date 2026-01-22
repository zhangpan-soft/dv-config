package com.dv.config.client.netty;

import com.dv.config.api.dto.RouteDTO;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.api.message.Message;
import com.dv.config.client.config.DynamicPropertySource;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 连接建立时加载路由配置的监听器
 * 在Netty客户端连接建立后立即查询路由配置并添加到Environment中
 */
@Slf4j
public class RouteLoadHandler {

    private final DynamicPropertySource routePropertySource;
    
    public RouteLoadHandler(DynamicPropertySource routePropertySource) {
        this.routePropertySource = routePropertySource;
    }

    /**
     * 处理路由查询响应
     */
    public void handleRouteResponse(Message message, CompletableFuture<Void> routeLoadFuture) {
        try {
            // 将路由配置添加到Environment中
            addRouteToEnvironment(message);

            if (routeLoadFuture != null){
                // 标记路由加载完成
                routeLoadFuture.complete(null);
            }

            
            log.info("路由配置已添加到Environment中");
        } catch (Exception e) {
            log.error("处理路由响应失败", e);
            if (routeLoadFuture != null){
                routeLoadFuture.completeExceptionally(e);
            }
        }
    }
    
    /**
     * 将路由配置添加到Environment中
     */
    private void addRouteToEnvironment(Message message) {
        try {
            // 解析路由数据
            String routeData = message.getData();
            if (routeData == null || routeData.isEmpty()) {
                log.warn("路由数据为空");
                return;
            }
            
            // 解析RouteDTO列表
            List<RouteDTO> routes = JsonUtil.fromJson(routeData, new TypeReference<>() {
            });
            if (routes == null || routes.isEmpty()) {
                log.warn("解析路由数据为空");
                return;
            }
            
            // 创建路由属性映射
            
            // 转换路由配置为属性格式
            for (int i = 0; i < routes.size(); i++) {
                RouteDTO route = routes.get(i);
                
                // 基本路由属性
                routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].id", route.getId());
                routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].uri", route.getUri());
                if (route.getOrderNum() != null) {
                    routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].order", route.getOrderNum());
                }
                
                // 处理断言
                if (route.getPredicates() != null) {
                    for (int j = 0; j < route.getPredicates().size(); j++) {
                        RouteDTO.PredicateDefinition predicate = route.getPredicates().get(j);
                        routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].predicates[" + j + "].name", predicate.getName());
                        
                        // 处理断言参数
                        if (predicate.getArgs() != null) {
                            for (Map.Entry<String, Object> arg : predicate.getArgs().entrySet()) {
                                routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].predicates[" + j + "].args." + arg.getKey(), arg.getValue());
                            }
                        }
                    }
                }
                
                // 处理过滤器
                if (route.getFilters() != null) {
                    for (int j = 0; j < route.getFilters().size(); j++) {
                        RouteDTO.FilterDefinition filter = route.getFilters().get(j);
                        routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].filters[" + j + "].name", filter.getName());
                        
                        // 处理过滤器参数
                        if (filter.getArgs() != null) {
                            for (Map.Entry<String, Object> arg : filter.getArgs().entrySet()) {
                                routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].filters[" + j + "].args." + arg.getKey(), arg.getValue());
                            }
                        }
                    }
                }
                
                // 处理元数据
                if (route.getMetadata() != null) {
                    for (Map.Entry<String, Object> metadata : route.getMetadata().entrySet()) {
                        routePropertySource.setProperty("spring.cloud.gateway.routes[" + i + "].metadata." + metadata.getKey(), metadata.getValue());
                    }
                }
            }
            
            log.info("路由配置已添加到Environment，路由数量: {}", routes.size());
        } catch (Exception e) {
            log.error("添加路由配置到Environment失败", e);
            throw new RuntimeException("Failed to add route config to environment", e);
        }
    }

}