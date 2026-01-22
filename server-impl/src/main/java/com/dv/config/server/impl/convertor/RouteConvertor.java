package com.dv.config.server.impl.convertor;

import com.dv.config.server.impl.dto.RouteVO;
import com.dv.config.server.impl.entity.Route;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RouteConvertor {

    RouteConvertor INSTANCE = Mappers.getMapper(RouteConvertor.class);

    RouteVO toVo(Route route);
}
