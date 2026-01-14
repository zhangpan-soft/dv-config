package com.dv.config.client.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件
 */
@Getter
public class ConfigRefreshEvent extends ApplicationEvent {
    
    public ConfigRefreshEvent(Object source) {
        super(source);
    }
}
