package com.dv.config.api.impl.convertor;

import com.dv.config.api.impl.dto.RouteVO;
import com.dv.config.api.impl.entity.Route;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RouteConvertor {

    RouteConvertor INSTANCE = Mappers.getMapper(RouteConvertor.class);

    RouteVO toVo(Route route);
}
