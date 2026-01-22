package com.dv.config.server.impl.dto;

import lombok.Data;

@Data
public class RouteDraftDTO {
    private String id;
    private String uri;
    private Integer orderNum;
    private boolean enabled;
    private String description;
    private String operationType;
    
    // 接收 JSON 字符串
    private String predicatesJson;
    private String filtersJson;
    private String metadataJson;
}
