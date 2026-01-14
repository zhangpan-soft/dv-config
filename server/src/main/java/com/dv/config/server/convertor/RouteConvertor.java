package com.dv.config.server.convertor;

import com.dv.config.api.entity.RouteDefinition;
import com.dv.config.common.dto.RouteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RouteConvertor {

    RouteConvertor INSTANCE = Mappers.getMapper(RouteConvertor.class);

    RouteDTO toDTO(RouteDefinition route);
}
