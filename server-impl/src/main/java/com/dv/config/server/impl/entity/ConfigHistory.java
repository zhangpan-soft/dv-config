package com.dv.config.server.impl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("config_history")
public class ConfigHistory {
    @TableId(type = IdType.AUTO)
    private Long historyId;
    
    @TableField(value = "`config_id`")
    private Long configId;
    
    @TableField(value = "`namespace`")
    private String namespace;
    @TableField(value = "`key`")
    private String key;
    @TableField(value = "`value`")
    private String value;
    @TableField(value = "`description`")
    private String description;
    @TableField(value = "`enabled`")
    private boolean enabled;
    @TableField(value = "`encrypted`")
    private boolean encrypted;
    
    @TableField(value = "`version`")
    private String version; // 版本号，例如 V20231027101010
    
    @TableField(value = "`operation_type`")
    private String operationType; // ADD, UPDATE, DELETE
    
    @TableField(value = "`create_time`")
    private LocalDateTime createTime;
    @TableField(value = "`create_by`")
    private String createBy;
}
