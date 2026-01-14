package com.dv.config.api.impl.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dv.config.api.entity.RouteDefinition;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@TableName("route")  // 使用固定表名来避免OGNL表达式解析问题
@Accessors(chain = true)
public class Route implements RouteDefinition {
    @TableId
    @TableField(value = "`id`")
    private String id;
    @TableField(value = "`uri`")
    private String uri;
    @TableField(value = "`predicates`", typeHandler = JacksonTypeHandler.class)
    private List<Predicate> predicates;
    @TableField(value = "`filters`", typeHandler = JacksonTypeHandler.class)
    private List<Filter> filters;
    @TableField(value = "`metadata`", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    @TableField(value = "`order`")
    private Integer order;
    @TableField(value = "`enabled`")
    private boolean enabled;
    @TableField(value = "`description`")
    private String description;

    @Override
    public int getOrder() {
        return this.order != null ? this.order : 0;
    }

    @Override
    public List<RouteDefinition.PredicateDefinition> getPredicates() {
        return (List<RouteDefinition.PredicateDefinition>) (List<?>) this.predicates;
    }

    @Override
    public List<RouteDefinition.FilterDefinition> getFilters() {
        return (List<RouteDefinition.FilterDefinition>) (List<?>) this.filters;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    @Data
    @Accessors(chain = true)
    public static class Predicate implements RouteDefinition.PredicateDefinition {
        private String name;
        private Map<String, String> args;
    }
    @Data
    @Accessors(chain = true)
    public static class Filter implements RouteDefinition.FilterDefinition {
        private String name;
        private Map<String, String> args;
    }
}
