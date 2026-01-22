package com.dv.config.server;

import com.dv.config.api.message.MessageDecoder;
import com.dv.config.api.message.MessageEncoder;
import com.dv.config.api.property.NettyServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyServer {

    @Resource
    private NettyServerProperties nettyServerProperties;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final NettyServerHandler nettyServerHandler;

    public NettyServer(NettyServerHandler nettyServerHandler) {
        this.nettyServerHandler = nettyServerHandler;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(nettyServerProperties.getBossThreadCount());
        workerGroup = new NioEventLoopGroup(nettyServerProperties.getWorkerThreadCount());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 空闲状态检测 (使用配置的超时时间)
                                .addLast(new IdleStateHandler(nettyServerProperties.getIdleTimeout(), 0, 0, TimeUnit.SECONDS))
                                // 编解码器
                                .addLast(new MessageDecoder())
                                .addLast(new MessageEncoder())
                                // 业务处理器
                                .addLast(nettyServerHandler);
                    }
                });

        // 绑定端口
        serverChannel = bootstrap.bind(nettyServerProperties.getPort()).sync().channel();
        log.info("Netty服务端启动成功, 端口: {}", nettyServerProperties.getPort());
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        log.info("Netty服务端已关闭");
    }
}