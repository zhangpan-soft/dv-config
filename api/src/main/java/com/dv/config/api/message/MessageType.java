package com.dv.config.api.message;

import lombok.Getter;

@Getter
public enum MessageType {

    /**
     * 1-心跳包
     */
    HEARTBEAT(1, "心跳包"),
    /**
     * 2-配置查询
     */
    QUERY_CONFIG(2, "配置查询"),
    /**
     * 4-配置更新通知
     */
    CONFIG_UPDATE_NOTIFY(3, "配置更新通知"),

    /**
     * 4-初始化
     */
    INIT(4, "初始化"),

    /**
     * 5-路由更新通知
     */
    ROUTE_UPDATE_NOTIFY(5, "路由更新通知"),
    
    /**
     * 6-路由查询
     */
    QUERY_ROUTE(6, "路由查询"),

    /**
     * 11-心跳响应
     */
    HEARTBEAT_RESPONSE(11, "心跳响应"),
    /**
     * 12-配置查询响应
     */
    QUERY_CONFIG_RESPONSE(12, "配置查询响应"),
    /**
     * 13-配置更新通知
     */
    CONFIG_UPDATE_NOTIFY_RESPONSE(13, "配置更新通知响应"),
    /**
     * 14-初始化响应
     */
    INIT_RESPONSE(14, "初始化响应"),
    
    /**
     * 15-路由查询响应
     */
    QUERY_ROUTE_RESPONSE(15, "路由查询响应"),
    ;
    private final Integer code;
    private final String desc;
    MessageType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MessageType fromCode(int code){
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getCode().equals(code)) {
                return messageType;
            }
        }
        return null;
    }
}
