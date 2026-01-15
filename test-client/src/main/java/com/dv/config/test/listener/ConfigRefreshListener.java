package com.dv.config.test.listener;

import com.dv.config.client.event.ConfigRefreshEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {

    @Resource
    private ContextRefresher contextRefresher;
    @Override
    public void onApplicationEvent(@Nonnull ConfigRefreshEvent event) {
        log.info("收到配置刷新事件，可以在这里执行自定义逻辑");
        // 注意：Environment 中的属性已经自动更新了，这里只是通知
        contextRefresher.refresh();
    }
}
