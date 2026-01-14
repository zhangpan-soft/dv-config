package com.dv.config.server;

import com.dv.config.common.netty.MessageDecoder;
import com.dv.config.common.netty.MessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyServerHandler nettyServerHandler;

    // 构造器注入自定义Handler
    public NettyServerChannelInitializer(NettyServerHandler nettyServerHandler) {
        this.nettyServerHandler = nettyServerHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // 1. 空闲检测:65秒未读则触发心跳检测（略大于客户端轮询间隔）
        pipeline.addLast(new IdleStateHandler(65, 0, 0, TimeUnit.SECONDS));
        // 2. 序列化编解码器
        pipeline.addLast(new MessageEncoder());
        pipeline.addLast(new MessageDecoder());
        // 3. 自定义业务Handler
        pipeline.addLast(nettyServerHandler);
    }
}