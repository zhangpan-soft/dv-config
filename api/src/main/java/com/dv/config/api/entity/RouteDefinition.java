package com.dv.config.api.entity;

import java.util.List;
import java.util.Map;

/**
 * 路由定义
 */
public interface RouteDefinition {

    /**
     * 路由ID
     * @return 路由ID
     */
    String getId();

    /**
     * 路由地址
     * @return 路由地址
     */
    String getUri();

    /**
     * 路由顺序
     * @return 路由顺序
     */
    int getOrder();

    /**
     * 路由元数据
     * @return 路由元数据
     */
    Map<String, Object> getMetadata();

    /**
     * 路由断言
     * @return 路由断言
     */
    List<PredicateDefinition> getPredicates();

    /**
     * 路由过滤器
     * @return 路由过滤器
     */
    List<FilterDefinition> getFilters();

    /**
     * 是否启用
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 路由断言定义
     */
    interface PredicateDefinition {

        /**
         * 断言名称
         * @return 断言名称
         */
        String getName();

        /**
         * 断言参数
         * @return 断言参数
         */
        Map<String, String> getArgs();
    }

    /**
     * 路由过滤器定义
     */
    interface FilterDefinition {

        /**
         * 过滤器名称
         * @return 过滤器名称
         */
        String getName();

        /**
         * 过滤器参数
         * @return 过滤器参数
         */
        Map<String, String> getArgs();
    }
}
