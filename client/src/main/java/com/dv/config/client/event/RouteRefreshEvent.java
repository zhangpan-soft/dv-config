package com.dv.config.client.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 路由刷新事件
 */
@Getter
public class RouteRefreshEvent extends ApplicationEvent {

    public RouteRefreshEvent(Object source) {
        super(source);
    }
}
