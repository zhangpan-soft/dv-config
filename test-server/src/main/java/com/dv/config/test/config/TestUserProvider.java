package com.dv.config.test.config;

import com.dv.config.api.impl.security.UserProvider;
import org.springframework.stereotype.Component;

@Component
public class TestUserProvider implements UserProvider {

    @Override
    public String getUserId() {
        return "test-admin";
    }

    @Override
    public String getUserName() {
        return "Test Administrator";
    }
}
