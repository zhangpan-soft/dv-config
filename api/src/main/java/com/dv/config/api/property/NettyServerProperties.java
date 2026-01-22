package com.dv.config.api.property;

import com.dv.config.api.crypto.CryptoProperties;
import lombok.Data;

@Data
public class NettyServerProperties {
    private int port;

    private int bossThreadCount;

    private int workerThreadCount;

    /**
     * 心跳超时时间（秒），默认65秒
     */
    private int idleTimeout = 65;

    /**
     * 加密配置
     */
    private CryptoProperties crypto = new CryptoProperties();
}