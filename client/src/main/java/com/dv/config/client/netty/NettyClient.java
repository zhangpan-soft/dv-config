package com.dv.config.client.netty;

import com.dv.config.api.crypto.HashUtil;
import com.dv.config.api.dto.ConfigDTO;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.api.message.Message;
import com.dv.config.api.message.MessageDecoder;
import com.dv.config.api.message.MessageEncoder;
import com.dv.config.api.message.MessageType;
import com.dv.config.api.property.NettyClientProperties;
import com.dv.config.client.config.DynamicPropertySource;
import com.dv.config.client.event.ConfigRefreshEvent;
import com.dv.config.client.event.RouteRefreshEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

import java.security.SecureRandom;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty客户端（实现可靠重连）
 */
@Slf4j
public class NettyClient implements DisposableBean {

    // ========== 重连状态 ==========
    /**
     * 是否正在重连（防止并发重连）
     */
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    /**
     * 当前重连次数
     */
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * 是否处于启动阶段（启动阶段连接失败需要抛异常,运行阶段连接断开只需后台重连）
     */
    private final AtomicBoolean isStartupPhase = new AtomicBoolean(true);
    private final NettyClientHandler nettyClientHandler;
    private final NettyClientProperties nettyClientProperties;
    private final DynamicPropertySource dynamicPropertySource;

    private CompletableFuture<Void> initFuture;
    private final CompletableFuture<Void> initRouteFuture = new CompletableFuture<>();

    // ========== Netty核心对象 ==========
    private EventLoopGroup group;
    /**
     * -- GETTER --
     *  获取当前通道（用于业务发送消息）
     */
    @Getter
    private Channel channel;

    private final Map<String, String> namespaceConfigSha1 = new ConcurrentHashMap<>();
    private volatile String routeSha1;

    // ========== 轮询守护线程 ==========
    private Thread configPollThread;
    private Thread routePollThread;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private final SecureRandom random = new SecureRandom();

    private final RouteLoadHandler routeLoadHandler;

    @Setter
    private ApplicationContext applicationContext;

    public NettyClient(NettyClientProperties nettyClientProperties,
                       DynamicPropertySource dynamicPropertySource) {
        this(nettyClientProperties, dynamicPropertySource, null);
    }

    public NettyClient(NettyClientProperties nettyClientProperties,
                       DynamicPropertySource dynamicPropertySource,
                       RouteLoadHandler routeLoadHandler) {
        this.nettyClientHandler = new NettyClientHandler();
        this.nettyClientProperties = nettyClientProperties;
        this.dynamicPropertySource = dynamicPropertySource;
        this.routeLoadHandler = routeLoadHandler;
        doConnect();
    }

    /**
     * 核心:执行连接逻辑（可重复调用）
     */
    private void doConnect() {
        // 1. 保存旧的引用
        EventLoopGroup oldGroup = group;
        Channel oldChannel = channel;

        // 2. 初始化新的EventLoopGroup
        group = new NioEventLoopGroup();

        try {
            initFuture = new CompletableFuture<>();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true) // 禁用Nagle算法,减少延迟
                    .handler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                        @Override
                        protected void initChannel(io.netty.channel.socket.SocketChannel ch) {
                            ch.pipeline()
                                    // 修改心跳间隔为15秒（写空闲），与配置保持一致
                                    .addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS))
                                    .addLast(new MessageDecoder())
                                    .addLast(new MessageEncoder())
                                    .addLast(nettyClientHandler);
                        }
                    });

            // 3. 获取连接地址（集群场景从服务发现获取,单机用默认地址）
            String host = nettyClientProperties.getHost();
            int port = nettyClientProperties.getPort();

            // 4. 发起连接
            channel = bootstrap.connect(host, port).sync().channel();
            log.info("Netty客户端启动成功,连接到{}:{}", host, port);

            // 5. 监听通道关闭事件（通道关闭后触发重连）
            // 保存当前channel和group引用,避免关闭新连接的资源
            final Channel currentChannel = channel;
            final EventLoopGroup currentGroup = group;
            assert channel != null;
            channel.closeFuture().addListener(future -> {
                log.info("与配置服务的连接已关闭");
                // 只关闭当前连接对应的group,避免关闭新连接的group
                if (currentGroup != null && !currentGroup.isShutdown()) {
                    currentGroup.shutdownGracefully();
                }
                // 只有当关闭的channel是当前活跃的channel时,才触发重连
                if (currentChannel == channel) {
                    log.info("当前活跃连接断开,触发重连");
                    triggerReconnect();
                } else {
                    log.info("旧连接关闭,无需重连");
                }
            });

            // 6. 关闭旧连接和旧的EventLoopGroup
            if (oldChannel != null && oldChannel.isActive()) {
                oldChannel.close();
            }
            if (oldGroup != null && !oldGroup.isShutdown()) {
                oldGroup.shutdownGracefully();
            }

            // 7. 等待初始化完成
            initFuture.get(10, TimeUnit.SECONDS);
            if (isStartupPhase.get() && nettyClientProperties.isHasRoute()) {
                initRouteFuture.get(10, TimeUnit.SECONDS);
            }

            // 8. 连接成功:重置重连状态(必须在最后,确保整个流程完成)
            reconnectAttempts.set(0);
            isReconnecting.set(false);

        } catch (InterruptedException e) {
            log.error("Netty客户端连接被中断", e);
            Thread.currentThread().interrupt();
            // 启动阶段:抛出异常,阻止应用启动
            if (isStartupPhase.get()) {
                throw new RuntimeException("Netty客户端启动失败:连接被中断", e);
            }
            // 运行阶段:抛出异常,由reconnect()循环处理
            throw new RuntimeException("Netty客户端连接被中断", e);
        } catch (Exception e) {
            log.error("Netty客户端启动失败", e);
            // 清理资源
            if (group != null) {
                group.shutdownGracefully();
            }
            // 启动阶段:抛出异常,阻止应用启动
            if (isStartupPhase.get()) {
                throw new RuntimeException("Netty客户端启动失败:无法连接到配置服务 " +
                        nettyClientProperties.getHost() + ":" + nettyClientProperties.getPort(), e);
            }
            // 运行阶段:抛出异常,由reconnect()循环处理
            throw new RuntimeException("Netty客户端连接失败", e);
        }
    }

    /**
     * 标记启动阶段完成,进入运行阶段
     * 此后连接断开不会影响服务运行,只会后台重连
     */
    public void markStartupComplete() {
        isStartupPhase.set(false);
        log.info("Netty客户端启动阶段完成,进入运行阶段");

        // 启动轮询守护线程
        startPollThreads();
    }

    /**
     * 触发重连（核心入口）
     */
    public void triggerReconnect() {
        // 1. 检查是否正在重连
        if (isReconnecting.compareAndSet(false, true)) {
            new Thread(this::reconnect, "netty-client-reconnect-thread").start();
        }
    }

    /**
     * 实际重连逻辑（循环重连直到成功或达到最大次数）
     */
    private void reconnect() {
        while (true) {
            try {
                int currentAttempt = reconnectAttempts.incrementAndGet();
                int maxAttempts = nettyClientProperties.getMaxAttempts();

                // 1. 检查是否超过最大重连次数
                if (maxAttempts > 0 && currentAttempt > maxAttempts) {
                    log.error("重连次数已达上限（{}次）,停止重连", maxAttempts);
                    break;
                }

                // 2. 计算重连延迟（指数退避）
                long delay = nettyClientProperties.calculateDelay(currentAttempt);
                log.info("第{}次重连,延迟{}毫秒后尝试", currentAttempt, delay);
                TimeUnit.MILLISECONDS.sleep(delay);

                // 3. 执行连接
                doConnect();

                // 4. 连接成功,退出重连循环
                log.info("重连成功,退出重连循环");
                break;

            } catch (InterruptedException e) {
                log.error("重连线程被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 连接失败,记录错误,继续下一次重连（在while循环中）
                log.warn("第{}次重连失败,将继续尝试重连", reconnectAttempts.get());
            }
        }

        // 重连线程结束,重置标记
        isReconnecting.set(false);
        log.info("重连线程退出,isReconnecting已重置为false");
    }

    /**
     * 关闭客户端
     */
    public void stop() {
        log.info("开始关闭Netty客户端");
        // 停止重连
        isReconnecting.set(true);
        // 关闭通道
        if (channel != null) {
            channel.close();
        }
        // 关闭事件循环组
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("Netty客户端已关闭");
        stopPollThreads();
    }

    /**
     * 查询路由
     */
    public void queryRoutes() {
        if (channel != null && channel.isActive()) {
            Message queryMsg = new Message();
            queryMsg.setType(MessageType.QUERY_ROUTE.getCode());
            channel.writeAndFlush(queryMsg);
            log.info("发送路由查询请求");
        }
    }

    /**
     * 查询配置
     */
    public void queryConfig() {
        if (channel != null && channel.isActive()) {
            Message queryMsg = new Message();
            queryMsg.setType(MessageType.QUERY_CONFIG.getCode());
            queryMsg.setSubscribeNamespaces(nettyClientProperties.getSubscribeNamespaces());
            channel.writeAndFlush(queryMsg);
            if (log.isDebugEnabled()) {
                log.debug("发送配置查询请求");
            }
        } else {
            log.warn("无法查询配置,连接未建立");
        }
    }

    /**
     * 启动轮询守护线程
     */
    private void startPollThreads() {
        if (!isPolling.compareAndSet(false, true)) {
            log.warn("轮询线程已经启动，跳过重复启动");
            return;
        }

        // 1. 启动配置轮询线程
        configPollThread = new Thread(() -> {
            long sleepMills = nettyClientProperties.getConfigPollInterval() + random.nextInt(1, 10_000);
            log.info("配置轮询守护线程启动，间隔: {}ms", sleepMills);
            while (isPolling.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMills);
                    queryConfig();
                } catch (InterruptedException e) {
                    log.info("配置轮询线程被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("配置轮询异常", e);
                }
            }
            log.info("配置轮询守护线程退出");
        }, "config-poll-thread");
        configPollThread.setDaemon(true);
        configPollThread.start();

        // 2. 如果启用了路由功能，启动路由轮询线程
        if (nettyClientProperties.isHasRoute()) {
            routePollThread = new Thread(() -> {
                long sleepMills = nettyClientProperties.getRoutePollInterval() + random.nextInt(1, 10_000);
                log.info("路由轮询守护线程启动，间隔: {}ms", sleepMills);
                while (isPolling.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepMills);
                        queryRoutes();
                    } catch (InterruptedException e) {
                        log.info("路由轮询线程被中断");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("路由轮询异常", e);
                    }
                }
                log.info("路由轮询守护线程退出");
            }, "route-poll-thread");
            routePollThread.setDaemon(true);
            routePollThread.start();
        }
    }

    /**
     * 停止轮询线程
     */
    private void stopPollThreads() {
        isPolling.set(false);

        if (configPollThread != null && configPollThread.isAlive()) {
            configPollThread.interrupt();
            log.info("配置轮询线程已停止");
        }

        if (routePollThread != null && routePollThread.isAlive()) {
            routePollThread.interrupt();
            log.info("路由轮询线程已停止");
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    @ChannelHandler.Sharable
    public class NettyClientHandler extends SimpleChannelInboundHandler<Message> {
        /**
         * 连接建立后发送注册消息
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("与配置服务建立Netty连接,发送注册消息");
            Message registerMsg = new Message();
            registerMsg.setType(MessageType.INIT.getCode());
            registerMsg.setSubscribeNamespaces(nettyClientProperties.getSubscribeNamespaces());
            ctx.writeAndFlush(registerMsg);
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {

            log.debug("收到服务端消息, type: {}, message: {}", msg.getType(), msg);
            MessageType messageType = MessageType.fromCode(msg.getType());
            if (messageType == null) {
                log.error("未知消息类型:{}", msg.getType());
                return;
            }
            switch (messageType) {
                case CONFIG_UPDATE_NOTIFY -> {
                    handleConfig(msg);
                    Message message = new Message();
                    message.setSubscribeNamespaces(msg.getSubscribeNamespaces());
                    message.setType(MessageType.CONFIG_UPDATE_NOTIFY_RESPONSE.getCode());
                    message.setData("1");
                    ctx.writeAndFlush(message);
                    log.info("配置更新处理完成");
                }
                case ROUTE_UPDATE_NOTIFY -> {
                    // 检查路由 SHA1 是否变化
                    String newRouteSha1 = HashUtil.sha1(msg.getData());
                    if (newRouteSha1 != null && newRouteSha1.equals(routeSha1)) {
                        if (log.isDebugEnabled()){
                            log.debug("路由未变化，跳过更新. SHA1: {}", newRouteSha1);
                        }
                        return;
                    }

                    // 路由发生变化，更新并发布事件
                    routeSha1 = newRouteSha1;
                    log.info("收到路由更新通知, SHA1变化: {}", newRouteSha1);
                    routeLoadHandler.handleRouteResponse(msg, null);
                    applicationContext.publishEvent(new RouteRefreshEvent(this));
                }
                case HEARTBEAT_RESPONSE -> {
                    if (log.isDebugEnabled()) {
                        log.info("收到心跳响应");
                    }
                }
                case QUERY_CONFIG_RESPONSE -> {
                    if (log.isDebugEnabled()) {
                        log.info("收到配置查询响应");
                    }
                    handleConfig(msg);
                }
                case INIT_RESPONSE -> {
                    if (log.isDebugEnabled()) {
                        log.info("收到初始化响应");
                    }
                    handleConfig(msg);
                    initFuture.complete(null);

                    // 在初始化完成后，如果启用了路由功能，则立即查询路由
                    if (nettyClientProperties.isHasRoute()) {
                        queryRoutes();
                        log.info("初始化完成，已发送路由查询请求");
                    }
                }
                case QUERY_ROUTE_RESPONSE -> {
                    log.info("收到路由查询响应");
                    // 检查路由 SHA1 是否变化
                    String newRouteSha1 = HashUtil.sha1(msg.getData());
                    if (newRouteSha1 != null && newRouteSha1.equals(routeSha1)) {
                        if (log.isDebugEnabled()){
                            log.debug("路由未变化，跳过更新. SHA1: {}", newRouteSha1);
                        }
                        return;
                    }

                    // 路由发生变化，更新并发布事件
                    routeSha1 = newRouteSha1;
                    log.info("路由发生变化, SHA1: {}", newRouteSha1);
                    if (isStartupPhase.get()){
                        routeLoadHandler.handleRouteResponse(msg, initRouteFuture);
                    }
                }
                default -> log.warn("未知的消息类型, remoteAddress:{},message:{}", ctx.channel().remoteAddress(), msg);
            }
        }

        /**
         * 连接异常:触发重连或者中断启动
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("与配置服务连接异常", cause);
            ctx.close(); // 关闭通道,会触发channelInactive和closeFuture
        }

        /**
         * 空闲事件:发送心跳包
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                Message heartBeat = new Message();
                // 使用枚举常量替代硬编码,增强可维护性
                heartBeat.setType(MessageType.HEARTBEAT.getCode());
                ctx.writeAndFlush(heartBeat);
                log.info("发送心跳包到服务端");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        /**
         * 连接断开:触发重连（可选,因为closeFuture已监听）
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.warn("与配置服务的连接断开");
            super.channelInactive(ctx);
        }

        private void handleConfig(Message message) {
            // 1. 计算配置的 SHA1
            ConfigDTO configDTO = JsonUtil.fromJson(message.getData(), ConfigDTO.class);
            boolean flag = false;
            for (Map.Entry<String, TreeMap<String, String>> entry : configDTO.getConfigs().entrySet()) {
                TreeMap<String, String> map = entry.getValue();
                String configJson = JsonUtil.toJson(map);
                String newSha1 = HashUtil.sha1(configJson);
                String oldSha1 = namespaceConfigSha1.get(entry.getKey());

                // 2. 检查 SHA1 是否变化
                if (newSha1 != null && newSha1.equals(oldSha1)) {
                    log.debug("配置未变化，跳过更新. 命名空间: {}, SHA1: {}", entry.getKey(), newSha1);
                    continue;
                }

                if (!flag && nettyClientProperties.getRefreshNamespaces().contains(entry.getKey())) {
                    flag = true;
                }

                // 3. 配置发生变化，更新 SHA1 并应用配置
                namespaceConfigSha1.put(entry.getKey(), newSha1);
                log.info("配置发生变化, 命名空间: {}, SHA1: {}", entry.getKey(), newSha1);
                entry.getValue().forEach(dynamicPropertySource::setProperty);
            }

            // 4. 发布配置刷新事件
            if (applicationContext != null && flag) {
                applicationContext.publishEvent(new ConfigRefreshEvent(this));
                if (log.isDebugEnabled()) {
                    log.debug("发布配置刷新事件");
                }
            }
        }
    }
}