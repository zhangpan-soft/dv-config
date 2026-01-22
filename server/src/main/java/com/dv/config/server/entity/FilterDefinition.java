package com.dv.config.server.entity;

import java.util.Map;

public interface FilterDefinition {
    /**
     * 过滤器名称
     * @return 过滤器名称
     */
    String getName();

    /**
     * 过滤器参数
     * @return 过滤器参数
     */
    Map<String, Object> getArgs();
}
