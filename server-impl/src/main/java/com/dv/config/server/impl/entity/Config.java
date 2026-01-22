package com.dv.config.server.impl.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dv.config.server.entity.ConfigDefinition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("config")
public class Config implements ConfigDefinition {
    @TableId
    @TableField(value = "`id`")
    private Long id;
    @TableField(value = "`namespace`")
    private String namespace;
    @TableField(value = "`key`")
    private String key;
    @TableField(value = "`value`")
    private String value;
    @TableField(value = "`description`")
    private String description;
    @TableField(value = "`create_time`")
    private LocalDateTime createTime;
    @TableField(value = "`update_time`")
    private LocalDateTime updateTime;
    @TableField(value = "`enabled`")
    private boolean enabled;
    @TableField(value = "`encrypted`")
    private boolean encrypted;
    
    @TableField(value = "`create_by`")
    private String createBy;
    @TableField(value = "`update_by`")
    private String updateBy;

    @Override
    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
