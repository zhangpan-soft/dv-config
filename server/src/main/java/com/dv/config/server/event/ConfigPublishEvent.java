package com.dv.config.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 配置发布事件
 * 当配置在管理端发布时触发
 */
@Getter
public class ConfigPublishEvent extends ApplicationEvent {

    private final Set<String> namespaces;

    public ConfigPublishEvent(Object source, Set<String> namespaces) {
        super(source);
        this.namespaces = namespaces;
    }

}
