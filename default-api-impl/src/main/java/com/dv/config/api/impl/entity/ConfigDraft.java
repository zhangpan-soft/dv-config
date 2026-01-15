package com.dv.config.api.impl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("config_draft")
public class ConfigDraft {
    @TableId(type = IdType.AUTO)
    private Long id; // 草稿自身的ID
    
    @TableField(value = "`config_id`")
    private Long configId; // 关联的正式配置ID
    
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
    
    @TableField(value = "`operation_type`")
    private String operationType; // ADD, UPDATE, DELETE
    
    @TableField(value = "`create_time`")
    private LocalDateTime createTime;
    @TableField(value = "`update_time`")
    private LocalDateTime updateTime;
    @TableField(value = "`create_by`")
    private String createBy;
    @TableField(value = "`update_by`")
    private String updateBy;
}
