package com.dv.config.server.impl.entity;

import com.dv.config.server.entity.FilterDefinition;
import lombok.Data;

import java.util.Map;

@Data
public class Filter implements FilterDefinition {
    private String name;
    private Map<String, Object> args;
}
