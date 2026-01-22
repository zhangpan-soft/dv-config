package com.dv.config.server.impl.entity;

import com.dv.config.server.entity.PredicateDefinition;
import lombok.Data;

import java.util.Map;

@Data
public class Predicate implements PredicateDefinition {
    private String name;
    private Map<String, Object> args;
}
