package com.dv.config.server.entity;

import java.util.Map;

public interface PredicateDefinition {
    /**
     * 断言名称
     * @return 断言名称
     */
    String getName();

    /**
     * 断言参数
     * @return 断言参数
     */
    Map<String, Object> getArgs();
}
