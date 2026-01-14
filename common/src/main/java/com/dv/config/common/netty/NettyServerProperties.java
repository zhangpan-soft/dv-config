package com.dv.config.common.netty;

import com.dv.config.common.crypto.CryptoProperties;
import lombok.Data;

@Data
public class NettyServerProperties {
    private int port;

    private int bossThreadCount;

    private int workerThreadCount;
    
    /**
     * 加密配置
     */
    private CryptoProperties crypto = new CryptoProperties();
}
