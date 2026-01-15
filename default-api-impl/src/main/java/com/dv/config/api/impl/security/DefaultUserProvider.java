package com.dv.config.api.impl.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认的用户信息提供者
 * 如果用户没有提供自己的实现，则使用此默认实现
 */
@Component
@ConditionalOnMissingBean(UserProvider.class)
public class DefaultUserProvider implements UserProvider {

    @Override
    public String getUserId() {
        return "admin";
    }

    @Override
    public String getUserName() {
        return "Administrator";
    }
}
