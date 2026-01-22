package com.dv.config.api.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 路由数据传输对象 (仅用于 Netty 传输)
 */
@Data
public class RouteDTO {
    private String id;
    private String uri;
    private List<PredicateDefinition> predicates;
    private List<FilterDefinition> filters;
    private Map<String, Object> metadata;
    private Integer orderNum;

    @Data
    public static class PredicateDefinition {
        private String name;
        private Map<String, Object> args;
    }

    @Data
    public static class FilterDefinition {
        private String name;
        private Map<String, Object> args;
    }
}
