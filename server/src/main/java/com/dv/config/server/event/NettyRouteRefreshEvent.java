package com.dv.config.server.event;

import com.dv.config.api.dto.RouteDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class NettyRouteRefreshEvent extends ApplicationEvent {
    @Getter
    private final List<RouteDTO> routes;
    public NettyRouteRefreshEvent(Object source,List<RouteDTO> routes) {
        super(source);
        this.routes = routes;
    }
}
