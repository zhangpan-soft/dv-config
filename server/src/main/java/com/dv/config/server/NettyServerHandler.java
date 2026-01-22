package com.dv.config.server;

import com.dv.config.api.dto.ConfigDTO;
import com.dv.config.api.dto.RouteDTO;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.api.message.Message;
import com.dv.config.api.message.MessageType;
import com.dv.config.server.convertor.RouteConvertor;
import com.dv.config.server.event.NettyConfigRefreshEvent;
import com.dv.config.server.event.NettyRouteRefreshEvent;
import com.dv.config.server.handler.NettyConfigHandler;
import com.dv.config.server.handler.NettyRouteHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty服务端业务处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    /**
     * 维护客户端连接: key=clientId, value=Channel
     */
    private static final Map<String, Channel> CLIENTS = new ConcurrentHashMap<>();

    private static final Map<String, List<String>> CLIENT_NAMESPACES = new ConcurrentHashMap<>();

    @Resource
    private NettyConfigHandler nettyConfigHandler;

    @Resource
    private NettyRouteHandler nettyRouteHandler;

    @EventListener(NettyConfigRefreshEvent.class)
    public void onConfigRefreshEvent(NettyConfigRefreshEvent event) {
        pushConfigUpdate(event.getConfigDTO());
    }

    @EventListener (NettyRouteRefreshEvent.class)
     public void onRouteRefreshEvent(NettyRouteRefreshEvent event) {
        pushRouteUpdate(event.getRoutes());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        MessageType messageType = MessageType.fromCode(msg.getType());
        if (messageType == null) {
            log.warn("未知消息类型, remoteAddress: {}, message: {}", ctx.channel().remoteAddress(), msg);
            return;
        }
        switch (messageType) {
            case INIT -> {
                log.info("收到初始化消息, remoteAddress: {}, message: {}", ctx.channel().remoteAddress(), msg);
                CLIENTS.put(ctx.channel().remoteAddress().toString(), ctx.channel());
                CLIENT_NAMESPACES.put(ctx.channel().remoteAddress().toString(), msg.getSubscribeNamespaces());

                // 查询客户端订阅的所有namespace配置
                ConfigDTO configDTO = new ConfigDTO();
                configDTO.getConfigs().putAll(nettyConfigHandler.getConfigs(msg.getSubscribeNamespaces().toArray(new String[0])));

                // 发送INIT响应 (包含所有配置)
                Message res = new Message();
                res.setType(MessageType.INIT_RESPONSE.getCode());
                res.setData(JsonUtil.toJson(configDTO));
                ctx.writeAndFlush(res);
                log.info("发送INIT响应, remoteAddress: {}, namespaces: {}", ctx.channel().remoteAddress(), msg.getSubscribeNamespaces());
            }
            case HEARTBEAT -> {// 心跳
                log.info("收到客户端心跳, remoteAddress: {}", ctx.channel().remoteAddress().toString());
                // 回复心跳
                Message heartBeat = new Message();
                heartBeat.setType(MessageType.HEARTBEAT_RESPONSE.getCode());
                heartBeat.setData("1");
                ctx.writeAndFlush(heartBeat);
            }
            case QUERY_CONFIG -> { // 配置查询
                log.info("收到客户端配置查询请求, remoteAddress: {}, namespaces: {}", ctx.channel().remoteAddress().toString(), msg.getSubscribeNamespaces());
                // 从Redis查询配置

                ConfigDTO configDTO = new ConfigDTO();
                if (msg.getSubscribeNamespaces() != null) {
                    configDTO.getConfigs().putAll(nettyConfigHandler.getConfigs(msg.getSubscribeNamespaces().toArray(new String[0])));
                }

                // 构建响应
                Message response = new Message();
                response.setType(MessageType.QUERY_CONFIG_RESPONSE.getCode());
                response.setSubscribeNamespaces(msg.getSubscribeNamespaces());
                response.setData(JsonUtil.toJson(configDTO));
                ctx.writeAndFlush(response);
            }
            case CONFIG_UPDATE_NOTIFY_RESPONSE -> log.info("收到配置更新通知响应, message: {}", msg);
            case QUERY_ROUTE -> { // 路由查询
                log.info("收到客户端路由查询请求, remoteAddress: {}", ctx.channel().remoteAddress());

                // 转换为 DTO (用于 Netty 传输)
                List<RouteDTO> routeDTOs = nettyRouteHandler.getRoutes().stream().map(RouteConvertor.INSTANCE::toDTO).toList();

                // 构建响应
                Message response = new Message();
                response.setType(MessageType.QUERY_ROUTE_RESPONSE.getCode());
                response.setData(JsonUtil.toJson(routeDTOs));
                ctx.writeAndFlush(response);

                log.info("发送路由查询响应, remoteAddress: {}, 路由数: {}",
                        ctx.channel().remoteAddress(), routeDTOs.size());
            }
            default -> log.warn("未知消息类型, remoteAddress: {}, message: {}", ctx.channel().remoteAddress(), msg);
        }
    }

    /**
     * 客户端建立连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CLIENTS.putIfAbsent(ctx.channel().remoteAddress().toString(), ctx.channel());
        log.info("客户端建立连接, remoteAddress: {}, 当前在线数: {}", ctx.channel().remoteAddress(), CLIENTS.size());
        super.channelActive(ctx);
    }

    /**
     * 客户端连接断开
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 移除无效连接
        CLIENTS.remove(ctx.channel().remoteAddress().toString());
        CLIENT_NAMESPACES.remove(ctx.channel().remoteAddress().toString());
        log.info("客户端连接断开, remoteAddress: {}, 当前在线数: {}", ctx.channel().remoteAddress(), CLIENTS.size());
        super.channelInactive(ctx);
    }

    /**
     * 空闲事件 (客户端65秒未发送消息)
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            CLIENTS.remove(ctx.channel().remoteAddress().toString());
            CLIENT_NAMESPACES.remove(ctx.channel().remoteAddress().toString());
            log.warn("客户端心跳超时, 关闭连接, remoteAddress: {}, 当前在线数: {}", ctx.channel().remoteAddress(), CLIENTS.size());
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        CLIENTS.remove(ctx.channel().remoteAddress().toString());
        CLIENT_NAMESPACES.remove(ctx.channel().remoteAddress().toString());
        log.error("客户端连接异常, remoteAddress: {}, 当前在线数: {}", ctx.channel().remoteAddress(), CLIENTS.size(), cause);
        ctx.close();
    }

    /**
     * 推送配置更新通知
     */
    public void pushConfigUpdate(ConfigDTO configDTO) {
        Set<String> namespacesSet = configDTO.getConfigs().keySet();
        CLIENT_NAMESPACES.forEach((client, namespaces) -> {
            List<String> namespaceList = namespaces.stream().filter(namespacesSet::contains).toList();
            ConfigDTO temp = new ConfigDTO();
            for (String namespace : namespaceList) {
                temp.getConfigs().put(namespace, new TreeMap<>(configDTO.getConfigs().get(namespace)));
            }
            Message notifyMessage = new Message();
            notifyMessage.setType(MessageType.CONFIG_UPDATE_NOTIFY.getCode());
            notifyMessage.setData(JsonUtil.toJson(temp));
            Channel channel = CLIENTS.get(client);
            channel.writeAndFlush(notifyMessage);
        });
    }

    /**
     * 推送路由更新通知（推送给所有客户端）
     */
    public void pushRouteUpdate(List<RouteDTO> routeDTOs) {
        CLIENTS.forEach((client, channel) -> {
            if (channel != null && channel.isActive()) {
                Message message = new Message();
                message.setType(MessageType.ROUTE_UPDATE_NOTIFY.getCode());
                message.setData(JsonUtil.toJson(routeDTOs));
                channel.writeAndFlush(message);
                log.info("推送路由更新通知给客户端, remoteAddress: {}, message: {}", channel.remoteAddress(), message);
            }
        });
    }
}
