package com.dv.config.api.impl.security;

/**
 * 用户信息提供者接口
 * 使用者实现此接口以对接自己的认证系统
 */
public interface UserProvider {

    /**
     * 获取当前操作用户ID
     * @return 用户ID
     */
    String getUserId();

    /**
     * 获取当前操作用户名称
     * @return 用户名称
     */
    String getUserName();
}
