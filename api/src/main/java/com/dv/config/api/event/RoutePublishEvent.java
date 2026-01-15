package com.dv.config.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * 路由发布事件
 * 当路由在管理端发布时触发
 */
public class RoutePublishEvent extends ApplicationEvent {

    public RoutePublishEvent(Object source) {
        super(source);
    }
}
