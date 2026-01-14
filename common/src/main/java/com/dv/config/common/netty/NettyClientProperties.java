package com.dv.config.common.netty;

import com.dv.config.common.crypto.CryptoProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NettyClientProperties {
    private String host;
    private int port;
    private List<String> subscribeNamespaces = new ArrayList<>();
    private List<String> refreshNamespaces = new ArrayList<>();

    /**
     * 最大重连次数（-1表示无限重连）
     */
    private int maxAttempts = 10;

    /**
     * 心跳间隔（毫秒）
     */
    private long initialDelay = 15000;
    
    /**
     * 重连初始延迟（毫秒）
     */
    private long reconnectInitialDelay = 2000;

    /**
     * 最大重连延迟（毫秒）
     */
    private long maxDelay = 15000;

    /**
     * 延迟倍数（指数退避）
     */
    private int multiplier = 2;
    
    /**
     * 是否启用路由功能（Gateway 服务需要设置为 true）
     */
    private boolean hasRoute = false;
    
    /**
     * 配置轮询间隔（毫秒，默认 60 秒）
     */
    private long configPollInterval = 60000;
    
    /**
     * 路由轮询间隔（毫秒，默认 60 秒）
     */
    private long routePollInterval = 60000;
    
    /**
     * 加密配置
     */
    private CryptoProperties crypto = new CryptoProperties();

    /**
     * 计算当前重连次数的延迟时间
     */
    public long calculateDelay(int attempt) {
        long delay = reconnectInitialDelay * (long) Math.pow(multiplier, attempt - 1);
        return Math.min(delay, maxDelay); // 不超过最大延迟
    }
}
