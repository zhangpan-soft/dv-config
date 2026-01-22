package com.dv.config.api.message;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Netty通信的统一消息体
 */
@Data
public class Message implements Serializable {
    /**
     * 消息类型:1-心跳包 2-配置查询 3-配置查询响应 4-配置更新通知
     */
    private Integer type;
    private String data;
    /**
     * 订阅的命名空间列表（用于初始化消息和配置查询消息）
     */
    private List<String> subscribeNamespaces;
}