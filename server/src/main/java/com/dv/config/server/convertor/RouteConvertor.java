package com.dv.config.server.convertor;

import com.dv.config.api.dto.RouteDTO;
import com.dv.config.server.entity.RouteDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RouteConvertor {

    RouteConvertor INSTANCE = Mappers.getMapper(RouteConvertor.class);

    RouteDTO toDTO(RouteDefinition route);
}
