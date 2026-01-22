package com.dv.config.client.listener;

import com.dv.config.client.event.ConfigRefreshEvent;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnClass(name = "org.springframework.cloud.context.refresh.ContextRefresher")
public class ClientConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {
    @Resource
    private ContextRefresher contextRefresher;
    @Override
    public void onApplicationEvent(@NonNull ConfigRefreshEvent event) {
        if (log.isDebugEnabled()){
            log.debug("Client config refresh event");
        }
        contextRefresher.refresh();
    }
}
