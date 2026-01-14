package com.dv.config.server.event;

import com.dv.config.common.dto.ConfigDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class NettyConfigRefreshEvent extends ApplicationEvent {
    @Getter
    private final ConfigDTO configDTO;
    public NettyConfigRefreshEvent(Object source,ConfigDTO configDTO) {
        super(source);
        this.configDTO = configDTO;
    }
}
