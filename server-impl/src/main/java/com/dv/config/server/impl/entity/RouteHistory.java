package com.dv.config.server.impl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "route_history", autoResultMap = true)
public class RouteHistory {
    @TableId(type = IdType.AUTO)
    private Long historyId;
    
    @TableField(value = "`route_id`")
    private String routeId;
    
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
    
    @TableField(value = "`version`")
    private String version;
    
    @TableField(value = "`operation_type`")
    private String operationType;
    
    @TableField(value = "`create_time`")
    private LocalDateTime createTime;
    @TableField(value = "`create_by`")
    private String createBy;
}
