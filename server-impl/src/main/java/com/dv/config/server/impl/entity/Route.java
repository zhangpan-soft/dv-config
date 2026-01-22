package com.dv.config.server.impl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dv.config.server.entity.FilterDefinition;
import com.dv.config.server.entity.PredicateDefinition;
import com.dv.config.server.entity.RouteDefinition;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "route", autoResultMap = true)
@Accessors(chain = true)
public class Route implements RouteDefinition {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    
    @TableField(value = "`uri`")
    private String uri;
    
    @TableField(value = "`predicates`", typeHandler = JacksonTypeHandler.class)
    private List<Predicate> predicates;
    
    @TableField(value = "`filters`", typeHandler = JacksonTypeHandler.class)
    private List<Filter> filters;

    @TableField(value = "`metadata`", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    
    @TableField(value = "`order_num`")
    private Integer orderNum;
    
    @TableField(value = "`enabled`")
    private boolean enabled;
    @TableField(value = "`description`")
    private String description;
    
    @TableField(value = "`create_time`")
    private LocalDateTime createTime;
    @TableField(value = "`update_time`")
    private LocalDateTime updateTime;
    @TableField(value = "`create_by`")
    private String createBy;
    @TableField(value = "`update_by`")
    private String updateBy;

    @Override
    public int getOrder() {
        return this.orderNum != null ? this.orderNum : 0;
    }

    @Override
    public List<PredicateDefinition> getPredicates() {
        return (List<PredicateDefinition>) (List<?>) this.predicates;
    }

    @Override
    public List<FilterDefinition> getFilters() {
        return (List<FilterDefinition>) (List<?>) this.filters;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }
}
