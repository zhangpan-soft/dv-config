package com.dv.config.api.impl.dto;

import com.dv.config.api.impl.entity.Route;
import com.dv.config.common.JsonUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
public class RouteVO extends Route {
    
    public String getPredicatesJson() {
        return getPredicates() != null ? JsonUtil.toJson(getPredicates()) : "";
    }
    
    public String getFiltersJson() {
        return getFilters() != null ? JsonUtil.toJson(getFilters()) : "";
    }
    
    public String getMetadataJson() {
        return getMetadata() != null ? JsonUtil.toJson(getMetadata()) : "";
    }
    
    public static RouteVO from(Route route) {
        RouteVO vo = new RouteVO();
        BeanUtils.copyProperties(route, vo);
        return vo;
    }
}
