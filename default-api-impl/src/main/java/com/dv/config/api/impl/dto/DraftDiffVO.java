package com.dv.config.api.impl.dto;

import lombok.Data;

@Data
public class DraftDiffVO {
    private String key; // Config Key or Route ID
    private String namespace; // Only for Config
    private String oldValue;
    private String newValue;
    private String diffType; // ADD, UPDATE, DELETE
    
    // For Route, value is a summary or JSON representation
}
