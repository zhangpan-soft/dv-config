package com.dv.config.test.listener;

import com.dv.config.client.event.ConfigRefreshEvent;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClientConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {

    @Override
    public void onApplicationEvent(@Nonnull ConfigRefreshEvent event) {
        log.info("收到配置刷新事件，可以在这里执行自定义逻辑");
        // 注意：Environment 中的属性已经自动更新了，这里只是通知
        // todo 可以使用springcloud逻辑
    }
}
